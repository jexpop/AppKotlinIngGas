package com.jexpop.appkotlininggas.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.jexpop.appkotlininggas.data.EncryptionManager
import com.jexpop.appkotlininggas.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.jexpop.appkotlininggas.BuildConfig
import com.jexpop.appkotlininggas.supabase
import io.github.jan.supabase.auth.auth
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

sealed class SettingsState {
    object Idle : SettingsState()
    object Success : SettingsState()
    data class Error(val message: String) : SettingsState()
}

class SettingsViewModel(
    application: Application,
    private val authRepository: AuthRepository = AuthRepository()
) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val _state = MutableStateFlow<SettingsState>(SettingsState.Idle)
    val state: StateFlow<SettingsState> = _state

    private val _isEncryptionConfigured = MutableStateFlow(false)
    val isEncryptionConfigured: StateFlow<Boolean> = _isEncryptionConfigured

    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail

    init {
        _isEncryptionConfigured.value = EncryptionManager.hasPassword(context)
        _userEmail.value = authRepository.getCurrentUserEmail()
    }

    fun saveEncryptionPassword(password: String, confirmPassword: String): Boolean {
        if (password.isBlank()) {
            _state.value = SettingsState.Error("La contraseña no puede estar vacía")
            return false
        }
        if (password != confirmPassword) {
            _state.value = SettingsState.Error("Las contraseñas no coinciden")
            return false
        }
        EncryptionManager.savePassword(context, password)
        _isEncryptionConfigured.value = true
        _state.value = SettingsState.Success
        return true
    }

    fun signOut(context: android.content.Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                supabase.auth.signOut()
            }
            val intent = context.packageManager
                .getLaunchIntentForPackage(context.packageName)!!
                .apply {
                    addFlags(
                        android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                }
            context.startActivity(intent)
            onSuccess()
        }
    }

    fun resetState() {
        _state.value = SettingsState.Idle
    }

    fun isAdmin(): Boolean {
        return _userEmail.value == BuildConfig.ADMIN_EMAIL
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                return SettingsViewModel(application) as T
            }
        }
    }
}