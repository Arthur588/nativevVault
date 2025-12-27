package com.example.vault.ui.importing

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vault.data.repository.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the import screen.  Handles importing a list of URIs
 * asynchronously and exposes state about the import process.
 */
@HiltViewModel
class ImportViewModel @Inject constructor(
    private val repository: VaultRepository
) : ViewModel() {

    sealed class ImportState {
        object Idle : ImportState()
        object Importing : ImportState()
        data class Success(val count: Int) : ImportState()
        data class Error(val message: String) : ImportState()
    }

    private val _state = MutableStateFlow<ImportState>(ImportState.Idle)
    val state: StateFlow<ImportState> = _state

    fun importUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        _state.value = ImportState.Importing
        viewModelScope.launch {
            try {
                val count = repository.importUris(uris)
                _state.value = ImportState.Success(count)
            } catch (e: Exception) {
                _state.value = ImportState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun reset() {
        _state.value = ImportState.Idle
    }
}