package com.example.vault.ui.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vault.data.model.MediaFile
import com.example.vault.data.repository.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the media viewer screen.  Loads the list for today and
 * exposes the current media item and its index.  Handles moving to the
 * next item and decrypting files on demand.
 */
@HiltViewModel
class ViewerViewModel @Inject constructor(
    private val repository: VaultRepository
) : ViewModel() {

    private var todayList: List<MediaFile> = emptyList()
    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex
    private val _currentMedia = MutableStateFlow<MediaFile?>(null)
    val currentMedia: StateFlow<MediaFile?> = _currentMedia

    private val _viewedCount = MutableStateFlow(0)
    val viewedCount: StateFlow<Int> = _viewedCount

    /** Load the today list and position the view at the item with the given ID.
     * Should be called before showing the viewer.  If the ID is not found,
     * the first item is selected.
     */
    fun load(mediaId: String?) {
        viewModelScope.launch {
            val (list, dailyIndex) = repository.getTodayMedia()
            todayList = list
            _viewedCount.value = dailyIndex
            val index = mediaId?.let { id -> list.indexOfFirst { it.id == id } } ?: 0
            val idx = if (index >= 0) index else 0
            _currentIndex.value = idx
            _currentMedia.value = list.getOrNull(idx)
        }
    }

    /** Move to the next item.  Marks the current item as viewed and updates
     * the pointer to the next item.  If there is no next item, the index
     * remains at the end and currentMedia becomes null.
     */
    fun next() {
        viewModelScope.launch {
            val current = _currentMedia.value ?: return@launch
            repository.markViewed(current.id)
            // update local state
            val newIndex = _currentIndex.value + 1
            _currentIndex.value = newIndex
            _currentMedia.value = todayList.getOrNull(newIndex)
            // update viewed count
            _viewedCount.value = _viewedCount.value + 1
        }
    }

    /** Decrypt the currently selected media to a temporary file.  Returns the
     * file path once complete.  Should be called from a coroutine or
     * suspend function.
     */
    suspend fun decryptCurrent(): java.io.File? {
        val media = _currentMedia.value ?: return null
        return repository.decryptToTempFile(media)
    }
}