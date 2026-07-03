package com.jexpop.appkotlininggas.data.repository

import android.content.Context
import com.jexpop.appkotlininggas.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email

class AuthRepository {

    suspend fun signInWithEmail(email: String, password: String): Result<Unit> {
        return runCatching {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
        }
    }

    suspend fun signInWithGoogle(context: Context): Result<Unit> {
        return runCatching {
            supabase.auth.signInWith(Google) {
                scopes.add("email")
                scopes.add("profile")
            }
        }
    }

    fun isAuthenticated(): Boolean {
        return supabase.auth.currentSessionOrNull() != null
    }

    fun getCurrentUserEmail(): String? {
        return supabase.auth.currentSessionOrNull()?.user?.email
    }
}