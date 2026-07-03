package com.jexpop.appkotlininggas.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object EncryptionManager {

    private const val PREFS_FILE = "ecogar_secure_prefs"
    private const val KEY_PASSWORD = "encryption_password"
    private const val PBKDF2_ITERATIONS = 100000
    private const val KEY_LENGTH = 256
    private const val SALT = "ecogar_salt_v1"

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
        getEncryptedPrefs(context).edit()
            .putString(KEY_PASSWORD, password)
            .apply()
    }

    fun getPassword(context: Context): String? {
        return getEncryptedPrefs(context).getString(KEY_PASSWORD, null)
    }

    fun hasPassword(context: Context): Boolean {
        return getPassword(context) != null
    }

    fun encrypt(data: ByteArray, password: String): ByteArray {
        val key = deriveKey(password)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data)
        // Prepend IV to encrypted data
        return iv + encrypted
    }

    fun decrypt(data: ByteArray, password: String): ByteArray {
        val key = deriveKey(password)
        val iv = data.copyOfRange(0, 12)
        val encrypted = data.copyOfRange(12, data.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted)
    }

    private fun deriveKey(password: String): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(
            password.toCharArray(),
            SALT.toByteArray(),
            PBKDF2_ITERATIONS,
            KEY_LENGTH
        )
        val secretKey = factory.generateSecret(spec)
        return SecretKeySpec(secretKey.encoded, "AES")
    }

    fun generateFileName(bankCode: String, paymentType: String, yearMonth: String): String {
        val code = bankCode.lowercase().replace("[^a-z0-9]".toRegex(), "")
        val type = if (paymentType == "C") "c" else "a"
        return "${code}_${type}_${yearMonth}.csv.enc"
    }
    
}