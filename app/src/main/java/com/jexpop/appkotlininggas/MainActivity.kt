package com.jexpop.appkotlininggas

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
import com.jexpop.appkotlininggas.ui.screens.LoginScreen
import com.jexpop.appkotlininggas.ui.screens.importcsv.ImportScreen
import com.jexpop.appkotlininggas.ui.theme.AppKotlinIngGasTheme

class MainActivity : ComponentActivity() {

    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppKotlinIngGasTheme {
                var isAuthenticated by remember {
                    mutableStateOf(authRepository.isAuthenticated())
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (isAuthenticated) {
                        ImportScreen(
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        LoginScreen(
                            modifier = Modifier.padding(innerPadding),
                            onLoginSuccess = { isAuthenticated = true }
                        )
                    }
                }
            }
        }
    }
}