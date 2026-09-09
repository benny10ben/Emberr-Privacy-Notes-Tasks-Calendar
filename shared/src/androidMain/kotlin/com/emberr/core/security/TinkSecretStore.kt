package com.emberr.core.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.core.content.edit

class TinkSecretStore(
    applicationContext: Context,
    private val storeFileName: String,
    private val secretCipher: AndroidSecretCipher
) {

    private val storedSecrets: SharedPreferences =
        applicationContext.getSharedPreferences(storeFileName, Context.MODE_PRIVATE)

    fun readSecret(secretName: String): String? {
        val storedValue = storedSecrets.getString(secretName, null) ?: return null
        return try {
            val encryptedBytes = Base64.decode(storedValue, Base64.NO_WRAP)
            secretCipher.decrypt(encryptedBytes, purposeLabelFor(secretName)).decodeToString()
        } catch (cause: Exception) {
            null
        }
    }

    fun writeSecret(secretName: String, secretValue: String) {
        val encryptedBytes = secretCipher.encrypt(
            secretValue.encodeToByteArray(),
            purposeLabelFor(secretName)
        )
        storedSecrets.edit(commit = true) {
            putString(secretName, Base64.encodeToString(encryptedBytes, Base64.NO_WRAP))
        }
    }

    fun removeSecret(secretName: String) {
        storedSecrets.edit(commit = true) { remove(secretName) }
    }

    fun clearAllSecrets() {
        storedSecrets.edit(commit = true) { clear() }
    }

    private fun purposeLabelFor(secretName: String) = "$storeFileName/$secretName"
}
