package com.example.vault.ui.today

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
 * ViewModel for the Today screen.  Provides the list of media items
 * scheduled for today along with the index of already viewed items.  Also
 * allows marking items as viewed.
 */
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val repository: VaultRepository
) : ViewModel() {
    private val _media = MutableStateFlow<List<MediaFile>>(emptyList())
    val media: StateFlow<List<MediaFile>> = _media
    private val _viewedCount = MutableStateFlow(0)
    val viewedCount: StateFlow<Int> = _viewedCount

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                val (list, dailyIndex) = repository.getTodayMedia()
                _media.value = list
                _viewedCount.value = dailyIndex
            } catch (_: Exception) {
                _media.value = emptyList()
                _viewedCount.value = 0
            }
        }
    }

    fun markViewed(id: String) {
        viewModelScope.launch {
            repository.markViewed(id)
            refresh()
        }
    }
}