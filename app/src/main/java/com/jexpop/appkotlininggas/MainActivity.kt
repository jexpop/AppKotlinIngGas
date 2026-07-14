package com.jexpop.appkotlininggas

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import android.widget.Toast
import com.jexpop.appkotlininggas.data.repository.AuthRepository
import com.jexpop.appkotlininggas.data.DriveManager
import com.jexpop.appkotlininggas.data.DriveAuthManager
import com.jexpop.appkotlininggas.data.EncryptionManager
import com.jexpop.appkotlininggas.ui.AppNavigation
import com.jexpop.appkotlininggas.ui.screens.LoginScreen
import com.jexpop.appkotlininggas.ui.theme.AppKotlinIngGasTheme
import io.github.jan.supabase.auth.handleDeeplinks
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supabase.handleDeeplinks(intent)
        enableEdgeToEdge()

        setContent {
            AppKotlinIngGasTheme {
                var isAuthenticated by remember {
                    mutableStateOf(authRepository.isAuthenticated())
                }

                LaunchedEffect(isAuthenticated) {
                    if (isAuthenticated) {
                        synchronizeStartupBackups()
                    }
                }

                if (isAuthenticated) {
                    AppNavigation(onLogout = { isAuthenticated = false })
                } else {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        LoginScreen(
                            modifier = Modifier.padding(innerPadding),
                            onLoginSuccess = { isAuthenticated = true }
                        )
                    }
                }
            }
        }
    }

    private suspend fun synchronizeStartupBackups() {
        withContext(Dispatchers.IO) {
            if (EncryptionManager.getSaltBase64(this@MainActivity) == null) {
                EncryptionManager.downloadSaltFromSupabase(this@MainActivity)
                    .onSuccess { android.util.Log.d("MAIN", "Salt sincronizado") }
                    .onFailure { android.util.Log.e("MAIN", "Error sincronizando salt: ${it.message}") }
            }

            val allowedEmail = com.jexpop.appkotlininggas.BuildConfig.DRIVE_ALLOWED_EMAIL
            if (allowedEmail.isBlank()) {
                return@withContext
            }

            if (!DriveAuthManager.isSignedIn(this@MainActivity)) {
                return@withContext
            }

            if (!DriveAuthManager.isAuthorizedAccount(this@MainActivity, allowedEmail)) {
                android.util.Log.w("MAIN", "Cuenta de Google no autorizada para Drive")
                return@withContext
            }

            val syncResult = DriveManager.syncSqlBackupsFromSupabase(this@MainActivity)
            withContext(Dispatchers.Main) {
                syncResult
                    .onSuccess { uploadedCount ->
                        android.util.Log.d("MAIN", "Backups SQL sincronizados con Drive: $uploadedCount subidos")
                        Toast.makeText(
                            this@MainActivity,
                            if (uploadedCount > 0) {
                                "Drive sincronizado: $uploadedCount nuevos"
                            } else {
                                "Drive ya estaba al día"
                            },
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .onFailure {
                        android.util.Log.e("MAIN", "Error sincronizando backups SQL: ${it.message}")
                        Toast.makeText(
                            this@MainActivity,
                            "No se pudo sincronizar Drive",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        supabase.handleDeeplinks(intent)
    }
}
