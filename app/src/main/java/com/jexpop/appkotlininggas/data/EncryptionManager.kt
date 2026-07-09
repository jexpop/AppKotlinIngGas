package com.jexpop.appkotlininggas.data

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.jexpop.appkotlininggas.supabase
import io.github.jan.supabase.postgrest.from
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

object EncryptionManager {

    private const val PREFS_FILE = "ecogar_secure_prefs"
    private const val KEY_PASSWORD = "encryption_password"
    private const val KEY_SALT = "encryption_salt"
    private const val PBKDF2_ITERATIONS = 100000
    private const val KEY_LENGTH = 256
    private const val TAG = "EncryptionManager"

    private fun getEncryptedPrefs(context: Context) =
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    fun savePassword(context: Context, password: String) {
        val prefs = getEncryptedPrefs(context)
        val existingSalt = prefs.getString(KEY_SALT, null)

        // Solo guarda la contraseña, NO genera nuevo salt
        // El salt ya debe existir (descargado de Supabase o generado previamente)
        prefs.edit()
            .putString(KEY_PASSWORD, password)
            .apply()

        Log.d(TAG, "Password guardada, salt: ${if (existingSalt != null) "existente" else "pendiente de sincronizar"}")
    }

    fun getPassword(context: Context): String? {
        return getEncryptedPrefs(context).getString(KEY_PASSWORD, null)
    }

    fun hasPassword(context: Context): Boolean {
        return getPassword(context) != null
    }

    fun getSaltBase64(context: Context): String? {
        return getEncryptedPrefs(context).getString(KEY_SALT, null)
    }

    suspend fun uploadSaltToSupabase(context: Context): Result<Unit> {
        return runCatching {
            val saltB64 = getSaltBase64(context) ?: return Result.failure(Exception("No hay salt"))
            supabase.from("app_param").update({
                set("value", saltB64)
            }) {
                filter {
                    eq("cond1", "ENCRYPTION")
                    eq("cond2", "SALT")
                }
            }
            Log.d(TAG, "Salt subido a Supabase")
        }
    }

    suspend fun downloadSaltFromSupabase(context: Context): Result<Unit> {
        return runCatching {
            val result = supabase.from("app_param")
                .select {
                    filter {
                        eq("cond1", "ENCRYPTION")
                        eq("cond2", "SALT")
                    }
                }
                .decodeList<com.jexpop.appkotlininggas.data.repository.AppParam>()

            val saltB64 = result.firstOrNull()?.value
            if (!saltB64.isNullOrBlank()) {
                getEncryptedPrefs(context).edit()
                    .putString(KEY_SALT, saltB64)
                    .apply()
                Log.d(TAG, "Salt descargado de Supabase")
            } else {
                Log.w(TAG, "No hay salt en Supabase")
            }
        }
    }

    fun encrypt(data: ByteArray, password: String, context: Context): ByteArray {
        val key = deriveKey(password, context)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data)
        return iv + encrypted
    }

    fun decrypt(data: ByteArray, password: String, context: Context): ByteArray {
        val key = deriveKey(password, context)
        val iv = data.copyOfRange(0, 12)
        val encrypted = data.copyOfRange(12, data.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted)
    }

    private fun deriveKey(password: String, context: Context): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val salt = getOrCreateSalt(context)
        val spec = PBEKeySpec(
            password.toCharArray(),
            salt,
            PBKDF2_ITERATIONS,
            KEY_LENGTH
        )
        val secretKey = factory.generateSecret(spec)
        return SecretKeySpec(secretKey.encoded, "AES")
    }

    private fun getOrCreateSalt(context: Context): ByteArray {
        val prefs = getEncryptedPrefs(context)
        val saltB64 = prefs.getString(KEY_SALT, null)
        return if (saltB64 != null) {
            Base64.decode(saltB64, Base64.NO_WRAP)
        } else {
            Log.w(TAG, "Salt no encontrado, usando legacy fallback")
            "ecogar_salt_v1".toByteArray()
        }
    }

    fun generateFileName(bankCode: String, paymentType: String, yearMonth: String): String {
        val code = bankCode.lowercase().replace("[^a-z0-9]".toRegex(), "")
        val type = if (paymentType == "C") "c" else "a"
        return "${code}_${type}_${yearMonth}.csv.enc"
    }

    fun initializeSaltIfNeeded(context: Context) {
        val prefs = getEncryptedPrefs(context)
        if (prefs.getString(KEY_SALT, null) == null) {
            val salt = ByteArray(16)
            SecureRandom().nextBytes(salt)
            val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
            prefs.edit().putString(KEY_SALT, saltB64).apply()
            Log.d(TAG, "Nuevo salt inicializado")
        }
    }

}