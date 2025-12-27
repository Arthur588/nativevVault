package com.example.vault.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vault.data.repository.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Home screen.  Provides counts of total media files and
 * the number of available (not yet viewed) items for today.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: VaultRepository
) : ViewModel() {

    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount

    private val _remainingToday = MutableStateFlow(0)
    val remainingToday: StateFlow<Int> = _remainingToday

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                val (list, dailyIndex) = repository.getTodayMedia()
                _remainingToday.value = list.size - dailyIndex
                _totalCount.value = repository.getMediaCount()
            } catch (_: Exception) {
                _remainingToday.value = 0
                _totalCount.value = 0
            }
        }
    }
}