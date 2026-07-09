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
import com.jexpop.appkotlininggas.data.DriveAuthManager
import com.jexpop.appkotlininggas.data.repository.AppParamRepository
import com.jexpop.appkotlininggas.R

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

    private val _isDriveConnected = MutableStateFlow(false)
    val isDriveConnected: StateFlow<Boolean> = _isDriveConnected


    private val _tokenExpiry = MutableStateFlow<String?>(null)
    val tokenExpiry: StateFlow<String?> = _tokenExpiry

    private val _tokenExpiryWarning = MutableStateFlow<TokenExpiryStatus>(TokenExpiryStatus.Ok)
    val tokenExpiryWarning: StateFlow<TokenExpiryStatus> = _tokenExpiryWarning

    private val appParamRepository = AppParamRepository()

    private val _githubToken = MutableStateFlow<String?>(null)
    val githubToken: StateFlow<String?> = _githubToken

    private val _githubRepoBackup = MutableStateFlow<String?>(null)
    val githubRepoBackup: StateFlow<String?> = _githubRepoBackup

    private val _githubRepoPublic = MutableStateFlow<String?>(null)
    val githubRepoPublic: StateFlow<String?> = _githubRepoPublic

    private val _githubUsername = MutableStateFlow<String?>(null)
    val githubUsername: StateFlow<String?> = _githubUsername

    init {
        _isEncryptionConfigured.value = EncryptionManager.hasPassword(context)
        _userEmail.value = authRepository.getCurrentUserEmail()
        loadTokenExpiry()
    }

    private fun loadTokenExpiry() {
        viewModelScope.launch {
            appParamRepository.getValue("GITHUB", "TOKEN_EXPIRY")
                .onSuccess { value ->
                    _tokenExpiry.value = value
                    _tokenExpiryWarning.value = calculateExpiryStatus(value)
                }
            appParamRepository.getValue("GITHUB", "TOKEN")
                .onSuccess { _githubToken.value = it }
            appParamRepository.getValue("GITHUB", "REPO_BACKUP")
                .onSuccess { _githubRepoBackup.value = it }
            appParamRepository.getValue("GITHUB", "REPO_PUBLIC")
                .onSuccess { _githubRepoPublic.value = it }
            appParamRepository.getValue("GITHUB", "USERNAME")
                .onSuccess { _githubUsername.value = it }
        }
    }

    fun updateTokenExpiry(date: String) {
        viewModelScope.launch {
            appParamRepository.setValue("GITHUB", "TOKEN_EXPIRY", date)
                .onSuccess {
                    _tokenExpiry.value = date
                    _tokenExpiryWarning.value = calculateExpiryStatus(date)
                    _state.value = SettingsState.Success
                }
                .onFailure {
                    _state.value = SettingsState.Error(it.message ?: context.getString(R.string.error_unknown))
                }
        }
    }

    private fun calculateExpiryStatus(date: String?): TokenExpiryStatus {
        if (date == null) return TokenExpiryStatus.Unknown
        return try {
            val parts = date.split("-")
            val expiry = java.util.Calendar.getInstance().apply {
                set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
            }
            val today = java.util.Calendar.getInstance()
            val daysLeft = ((expiry.timeInMillis - today.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
            when {
                daysLeft < 0 -> TokenExpiryStatus.Expired
                daysLeft < 30 -> TokenExpiryStatus.Warning(daysLeft)
                else -> TokenExpiryStatus.Ok
            }
        } catch (e: Exception) {
            TokenExpiryStatus.Unknown
        }
    }

    fun saveEncryptionPassword(password: String, confirmPassword: String): Boolean {
        if (password.isBlank()) {
            _state.value = SettingsState.Error(context.getString(R.string.settings_encryption_password_empty))
            return false
        }
        if (password != confirmPassword) {
            _state.value = SettingsState.Error(context.getString(R.string.settings_encryption_password_mismatch))
            return false
        }

        viewModelScope.launch {
            // 1. Intentar descargar salt de Supabase
            EncryptionManager.downloadSaltFromSupabase(context)

            // 2. Si no había salt, generar uno nuevo y subirlo
            if (EncryptionManager.getSaltBase64(context) == null) {
                EncryptionManager.initializeSaltIfNeeded(context)
                EncryptionManager.uploadSaltToSupabase(context)
                    .onSuccess { android.util.Log.d("SETTINGS", "Nuevo salt subido a Supabase") }
                    .onFailure { android.util.Log.e("SETTINGS", "Error subiendo salt: ${it.message}") }
            }

            // 3. Guardar contraseña
            EncryptionManager.savePassword(context, password)
            _isEncryptionConfigured.value = true
            _state.value = SettingsState.Success
        }

        return true
    }

    /** Obtiene el salt actual en Base64 (solo para admin) */
    fun getEncryptionSaltBase64(): String? {
        return EncryptionManager.getSaltBase64(context)
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


    fun checkDriveConnection(context: android.content.Context) {
        _isDriveConnected.value = DriveAuthManager.isSignedIn(context)
    }

    fun disconnectDrive(context: android.content.Context) {
        DriveAuthManager.signOut(context)
        _isDriveConnected.value = false
    }

    fun updateGithubToken(token: String) {
        viewModelScope.launch {
            appParamRepository.setValue("GITHUB", "TOKEN", token)
                .onSuccess {
                    _githubToken.value = token
                    _state.value = SettingsState.Success
                }
                .onFailure {
                    _state.value = SettingsState.Error(it.message ?: context.getString(R.string.error_unknown))
                }
        }
    }

    fun updateGithubRepoBackup(repo: String) {
        viewModelScope.launch {
            appParamRepository.setValue("GITHUB", "REPO_BACKUP", repo)
                .onSuccess {
                    _githubRepoBackup.value = repo
                    _state.value = SettingsState.Success
                }
                .onFailure {
                    _state.value = SettingsState.Error(it.message ?: context.getString(R.string.error_unknown))
                }
        }
    }

    fun updateGithubRepoPublic(repo: String) {
        viewModelScope.launch {
            appParamRepository.setValue("GITHUB", "REPO_PUBLIC", repo)
                .onSuccess {
                    _githubRepoPublic.value = repo
                    _state.value = SettingsState.Success
                }
                .onFailure {
                    _state.value = SettingsState.Error(it.message ?: context.getString(R.string.error_unknown))
                }
        }
    }

    fun updateGithubUsername(username: String) {
        viewModelScope.launch {
            appParamRepository.setValue("GITHUB", "USERNAME", username)
                .onSuccess {
                    _githubUsername.value = username
                    _state.value = SettingsState.Success
                }
                .onFailure {
                    _state.value = SettingsState.Error(it.message ?: context.getString(R.string.error_unknown))
                }
        }
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