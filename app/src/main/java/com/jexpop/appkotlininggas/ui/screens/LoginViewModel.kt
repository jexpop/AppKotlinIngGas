package com.jexpop.appkotlininggas.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jexpop.appkotlininggas.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state: StateFlow<LoginState> = _state

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _state.value = LoginState.Loading
            authRepository.signInWithEmail(email, password)
                .onSuccess {
                    _state.value = LoginState.Success
                }
                .onFailure { error ->
                    _state.value = LoginState.Error(error.message ?: "Error desconocido")
                }
        }
    }

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _state.value = LoginState.Loading
            authRepository.signInWithGoogle(context)
                .onSuccess {
                    _state.value = LoginState.Success
                }
                .onFailure { error ->
                    _state.value = LoginState.Error(error.message ?: "Error desconocido")
                }
        }
    }

    fun resetState() {
        _state.value = LoginState.Idle
    }
}