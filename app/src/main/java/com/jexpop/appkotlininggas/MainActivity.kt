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
import com.jexpop.appkotlininggas.data.repository.AuthRepository
import com.jexpop.appkotlininggas.ui.AppNavigation
import com.jexpop.appkotlininggas.ui.screens.LoginScreen
import com.jexpop.appkotlininggas.ui.theme.AppKotlinIngGasTheme
import io.github.jan.supabase.auth.handleDeeplinks

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

                if (isAuthenticated) {
                    AppNavigation()
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        supabase.handleDeeplinks(intent)
    }
}