package com.jexpop.appkotlininggas

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jexpop.appkotlininggas.ui.theme.AppKotlinIngGasTheme
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Test de conexión con Supabase
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = supabase.from("payment_type").select()
                Log.d("SUPABASE", "Conexión OK: ${result.data}")
            } catch (e: Exception) {
                Log.e("SUPABASE", "Error de conexión: ${e.message}")
            }
        }

        setContent {
            AppKotlinIngGasTheme {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Probando conexión con Supabase...")
                }
            }
        }
    }
}