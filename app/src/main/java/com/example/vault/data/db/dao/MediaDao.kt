package com.example.vault.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.vault.data.model.MediaFile
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mediaFile: MediaFile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(files: List<MediaFile>)

    @Query("SELECT * FROM media_files WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MediaFile?

    @Query("SELECT * FROM media_files WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<MediaFile>

    @Query("UPDATE media_files SET viewedDate = :timestamp WHERE id = :id")
    suspend fun updateViewedDate(id: String, timestamp: Long)

    @Query("DELETE FROM media_files WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM media_files")
    fun getAllFlow(): Flow<List<MediaFile>>
}