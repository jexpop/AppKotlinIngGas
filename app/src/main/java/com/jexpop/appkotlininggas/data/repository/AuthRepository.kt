package com.jexpop.appkotlininggas.data.repository

import com.jexpop.appkotlininggas.supabase
import io.github.jan.supabase.auth.auth
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

    suspend fun signOut(): Result<Unit> {
        return runCatching {
            supabase.auth.signOut()
        }
    }

    fun isAuthenticated(): Boolean {
        return supabase.auth.currentSessionOrNull() != null
    }
}