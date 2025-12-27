package com.example.vault.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vault.data.repository.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the login screen.  Manages the user's password input and
 * initialises the repository when the user submits their password.  Exposes
 * states for loading, success and error.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: VaultRepository
) : ViewModel() {

    sealed class LoginState {
        object Idle : LoginState()
        object Loading : LoginState()
        object Success : LoginState()
        data class Error(val message: String) : LoginState()
    }

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    fun login(password: String) {
        if (password.isBlank()) {
            _loginState.value = LoginState.Error("Password cannot be empty")
            return
        }
        _loginState.value = LoginState.Loading
        viewModelScope.launch {
            try {
                repository.initialise(password)
                _loginState.value = LoginState.Success
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Unknown error")
            }
        }
    }
}