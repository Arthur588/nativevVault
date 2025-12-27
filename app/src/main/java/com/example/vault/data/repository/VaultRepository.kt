package com.example.vault.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.annotation.WorkerThread
import com.example.vault.data.crypto.EncryptionManager
import com.example.vault.data.crypto.KeyDerivation
import com.example.vault.data.crypto.KeyManager
import com.example.vault.data.db.VaultDatabase
import com.example.vault.data.db.dao.CycleDao
import com.example.vault.data.db.dao.MediaDao
import com.example.vault.data.model.CycleState
import com.example.vault.data.model.MediaFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.SecureRandom
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * VaultRepository manages all data operations for the Vault.  It encapsulates
 * importing and encrypting files, retrieving daily lists with enforced limits,
 * marking items as viewed, deleting media, and handling the underlying
 * database.  The repository must be initialised with the user password via
 * [initialise] before any other methods can be invoked.  It retains the
 * derived encryption key in memory for the lifetime of the process.
 */
@Singleton
class VaultRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val keyManager: KeyManager
) {

    private var encryptionKey: ByteArray? = null
    private var db: VaultDatabase? = null

    /** Initialise the repository with a user supplied password.  This will
     * derive the encryption key using PBKDF2 and open the encrypted database.
     * It must be called before any data operations.  If the password is
     * incorrect an exception is thrown.
     */
    @Throws(IllegalStateException::class)
    fun initialise(password: String) {
        val key = keyManager.initialise(password)
        // Use the same 32 byte key for both file encryption and SQLCipher
        encryptionKey = key
        db = VaultDatabase.getInstance(context, key)
    }

    /** Shortcut to access the media DAO. */
    private fun mediaDao(): MediaDao {
        checkNotNull(db) { "Repository not initialised" }
        return db!!.mediaDao()
    }

    /** Shortcut to access the cycle DAO. */
    private fun cycleDao(): CycleDao {
        checkNotNull(db) { "Repository not initialised" }
        return db!!.cycleDao()
    }

    /**
     * Import a list of URIs into the vault.  Each file is copied into the
     * application internal storage and encrypted on the fly with the
     * encryption key.  Metadata is saved to the encrypted database.  A
     * thumbnail generation callback can be provided to perform expensive
     * thumbnail work off the main thread.  The number of successfully
     * imported items is returned.  New imports trigger regeneration of the
     * cycle order if necessary.
     */
    suspend fun importUris(
        uris: List<Uri>,
        generateThumbnail: suspend (MediaFile) -> Unit = {}
    ): Int = withContext(Dispatchers.IO) {
        val key = encryptionKey ?: throw IllegalStateException("Repository not initialised")
        val resolver: ContentResolver = context.contentResolver
        var count = 0
        for (uri in uris) {
            try {
                resolver.openInputStream(uri)?.use { inputStream ->
                    // Query metadata
                    val cursor = resolver.query(uri, null, null, null, null)
                    var name = "unknown"
                    var size: Long = 0
                    val mimeType: String = resolver.getType(uri) ?: "application/octet-stream"
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                            if (nameIndex != -1) name = it.getString(nameIndex)
                            if (sizeIndex != -1) size = it.getLong(sizeIndex)
                        }
                    }
                    // Generate unique ID
                    val id = UUID.randomUUID().toString()
                    // Create destination file
                    val vaultDir = File(context.filesDir, "vault")
                    if (!vaultDir.exists()) vaultDir.mkdirs()
                    val destFile = File(vaultDir, "$id.enc")
                    val fos = FileOutputStream(destFile)
                    // Encrypt stream using AES/GCM
                    val iv = EncryptionManager.encryptStream(inputStream, fos, key)
                    fos.close()
                    // Persist metadata
                    val mediaFile = MediaFile(
                        id = id,
                        originalName = name,
                        mimeType = mimeType,
                        size = size,
                        importTime = System.currentTimeMillis(),
                        encryptedPath = destFile.absolutePath,
                        iv = iv,
                        thumbPath = null,
                        viewedDate = null
                    )
                    mediaDao().insert(mediaFile)
                    // Generate thumbnail if needed
                    generateThumbnail(mediaFile)
                    count++
                }
            } catch (e: Exception) {
                // Ignore individual failures but log them
                Log.e("VaultRepository", "Failed to import URI $uri", e)
            }
        }
        if (count > 0) {
            regenerateOrderListIfNeeded()
        }
        return@withContext count
    }

    /**
     * Regenerate the random order list if it does not exist or if new items
     * were imported (i.e. items present in the database but not in the
     * existing orderList).  This method must be called from a background
     * thread.  It uses a deterministic shuffle (SecureRandom) for
     * unpredictability.
     */
    @WorkerThread
    private suspend fun regenerateOrderListIfNeeded() {
        val state = cycleDao().getState()
        val allMediaIds = mediaDao().getAllFlow().first().map { it.id }
        if (allMediaIds.isEmpty()) return
        if (state == null) {
            // Create a new random order for the first cycle
            val shuffled = allMediaIds.toMutableList()
            shuffleSecure(shuffled)
            val startOfToday = computeCurrentDayStart()
            val newState = CycleState(
                id = 0,
                orderList = shuffled.joinToString(","),
                pointer = 0,
                dailyIndex = 0,
                currentDayStart = startOfToday
            )
            cycleDao().insert(newState)
            return
        }
        // If there are IDs in DB not present in state.orderList, append them
        val existingIds = state.orderList.split(",").filter { it.isNotBlank() }
        val missing = allMediaIds.filter { !existingIds.contains(it) }
        if (missing.isNotEmpty()) {
            val newOrder = existingIds.toMutableList().apply { addAll(missing) }
            shuffleSecure(newOrder)
            val updated = state.copy(orderList = newOrder.joinToString(","))
            cycleDao().update(updated)
        }
    }

    /**
     * Retrieve the list of media files scheduled for today along with their
     * consumption status.  This method ensures day-boundary logic: if the
     * current time has passed the stored [CycleState.currentDayStart] plus
     * 24h, then the pointer is advanced by [dailyIndex] and [dailyIndex]
     * reset, unless no items were consumed.  It also handles cycle
     * completion by generating a new random order.  The returned list
     * always contains up to 7 items (or fewer if fewer remain) starting
     * from [pointer].  The first [dailyIndex] items in the returned list
     * have already been viewed today.
     */
    suspend fun getTodayMedia(): Pair<List<MediaFile>, Int> = withContext(Dispatchers.IO) {
        var state = cycleDao().getState()
        // If no state, create one
        if (state == null) {
            regenerateOrderListIfNeeded()
            state = cycleDao().getState()!!
        }
        // Evaluate day boundary
        val now = System.currentTimeMillis()
        val newDayStart = computeCurrentDayStart()
        var pointer = state.pointer
        var dailyIndex = state.dailyIndex
        var currentDayStart = state.currentDayStart
        var orderList = state.orderList.split(",").filter { it.isNotBlank() }
        if (currentDayStart < newDayStart) {
            // Day boundary crossed
            if (dailyIndex > 0) {
                pointer += dailyIndex
                dailyIndex = 0
            }
            currentDayStart = newDayStart
            // If pointer reached end, start a new cycle
            if (pointer >= orderList.size) {
                // generate new random order
                val allIds = mediaDao().getAllFlow().first().map { it.id }
                val shuffled = allIds.toMutableList()
                shuffleSecure(shuffled)
                orderList = shuffled
                pointer = 0
            }
            // persist state
            cycleDao().update(
                state.copy(
                    orderList = orderList.joinToString(","),
                    pointer = pointer,
                    dailyIndex = dailyIndex,
                    currentDayStart = currentDayStart
                )
            )
        }
        // Determine how many items to show today
        val remainingInCycle = orderList.size - pointer
        val count = if (remainingInCycle <= 0) {
            0
        } else {
            if (remainingInCycle < 7) remainingInCycle else 7
        }
        val end = pointer + count
        val idsForToday = if (pointer < orderList.size) orderList.subList(pointer, end) else emptyList()
        val files = if (idsForToday.isNotEmpty()) mediaDao().getByIds(idsForToday) else emptyList()
        // The first 'dailyIndex' items in files list have been viewed today
        return@withContext Pair(files, dailyIndex)
    }

    /**
     * Return the total number of media files currently stored in the vault.
     * If the repository is not initialised, returns 0.
     */
    suspend fun getMediaCount(): Int = withContext(Dispatchers.IO) {
        return@withContext try {
            mediaDao().getAllFlow().first().size
        } catch (_: Exception) {
            0
        }
    }

    /**
     * Mark a media item as viewed.  This increments the daily index and
     * updates the MediaFile viewedDate.  If the daily index reaches 7 or
     * there are no more items, the repository persists the new state.  This
     * method should be invoked when the user moves forward in the viewer.
     */
    suspend fun markViewed(id: String) = withContext(Dispatchers.IO) {
        val state = cycleDao().getState() ?: return@withContext
        val newDailyIndex = state.dailyIndex + 1
        // Update viewed date on media
        mediaDao().updateViewedDate(id, System.currentTimeMillis())
        // Persist cycle state with updated daily index
        cycleDao().update(state.copy(dailyIndex = newDailyIndex))
    }

    /**
     * Delete a media item from the vault.  The encrypted file and optional
     * thumbnail are removed from storage and the database entry deleted.
     */
    suspend fun deleteMedia(id: String) = withContext(Dispatchers.IO) {
        val file = mediaDao().getById(id) ?: return@withContext
        // Delete encrypted file
        try {
            File(file.encryptedPath).delete()
        } catch (_: Exception) {}
        // Delete thumbnail if exists
        file.thumbPath?.let { File(it).delete() }
        mediaDao().deleteById(id)
        // Remove from cycle order list if necessary
        val state = cycleDao().getState() ?: return@withContext
        val ids = state.orderList.split(",").filter { it.isNotBlank() }.toMutableList()
        val index = ids.indexOf(id)
        if (index != -1) {
            ids.removeAt(index)
            var pointer = state.pointer
            var dailyIndex = state.dailyIndex
            // adjust pointer and daily index if removal affects them
            if (index < state.pointer) {
                pointer = (state.pointer - 1).coerceAtLeast(0)
            }
            if (index < state.pointer + state.dailyIndex) {
                dailyIndex = (state.dailyIndex - 1).coerceAtLeast(0)
            }
            cycleDao().update(
                state.copy(
                    orderList = ids.joinToString(","),
                    pointer = pointer,
                    dailyIndex = dailyIndex
                )
            )
        }
    }

    /**
     * Decrypt a media file to a temporary file within cacheDir.  Returns the
     * File object of the decrypted copy.  The caller should manage
     * deletion of this temp file when done.  Note: this uses the in‑memory
     * encryption key and will throw if the repository has not been
     * initialised.  The decrypted file is named with the media id and
     * retains no file extension; it is safe to infer mime type from
     * MediaFile.mimeType.
     */
    suspend fun decryptToTempFile(media: MediaFile): File = withContext(Dispatchers.IO) {
        val key = encryptionKey ?: throw IllegalStateException("Repository not initialised")
        val decryptedDir = File(context.cacheDir, "decrypted")
        if (!decryptedDir.exists()) decryptedDir.mkdirs()
        val outFile = File(decryptedDir, media.id)
        File(media.encryptedPath).inputStream().use { input ->
            FileOutputStream(outFile).use { output ->
                EncryptionManager.decryptStream(input, output, key, media.iv)
            }
        }
        return@withContext outFile
    }

    /** Securely shuffle a list of strings using SecureRandom. */
    private fun shuffleSecure(list: MutableList<String>) {
        val random = SecureRandom()
        for (i in list.indices.reversed()) {
            val j = random.nextInt(i + 1)
            val tmp = list[i]
            list[i] = list[j]
            list[j] = tmp
        }
    }

    /** Compute the start of the current day at 07:00 local time. */
    private fun computeCurrentDayStart(): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = System.currentTimeMillis()
        // if current time is before 7:00, day start is 7:00 of previous day
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        if (hour < 7) {
            cal.add(Calendar.DAY_OF_MONTH, -1)
        }
        cal.set(Calendar.HOUR_OF_DAY, 7)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}