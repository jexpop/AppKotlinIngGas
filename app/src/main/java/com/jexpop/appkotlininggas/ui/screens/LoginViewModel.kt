package com.jexpop.appkotlininggas.ui.screens

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jexpop.appkotlininggas.R
import com.jexpop.appkotlininggas.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel(
    application: Application,
    private val authRepository: AuthRepository = AuthRepository()
) : AndroidViewModel(application) {

    private val context = application.applicationContext

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
                    _state.value = LoginState.Error(
                        error.message ?: context.getString(R.string.error_unknown)
                    )
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
                    _state.value = LoginState.Error(
                        error.message ?: context.getString(R.string.error_unknown)
                    )
                }
        }
    }

    fun resetState() {
        _state.value = LoginState.Idle
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                return LoginViewModel(application) as T
            }
        }
    }

}