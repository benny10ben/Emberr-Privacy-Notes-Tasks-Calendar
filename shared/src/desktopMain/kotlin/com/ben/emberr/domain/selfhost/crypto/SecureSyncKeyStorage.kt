package com.ben.emberr.domain.selfhost.crypto

import com.ben.emberr.domain.selfhost.webdav.SelfHostServerCredentials
import com.github.javakeyring.Keyring
import com.github.javakeyring.PasswordAccessException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.util.Base64
import java.util.prefs.Preferences

actual class SecureSyncKeyStorage {

    private val json = Json { ignoreUnknownKeys = true }
    private val preferences = Preferences.userRoot().node(PREFERENCES_NODE)

    private val keyring by lazy {
        try {
            Keyring.create()
        } catch (cause: Exception) {
            null
        }
    }

    actual fun saveEncryptionKey(key: ByteArray) {
        saveSecureString(ACCOUNT_ENCRYPTION_KEY, Base64.getEncoder().encodeToString(key))
    }

    actual fun getEncryptionKey(): ByteArray? {
        val encoded = getSecureString(ACCOUNT_ENCRYPTION_KEY) ?: return null
        return Base64.getDecoder().decode(encoded)
    }

    actual fun saveServerCredentials(credentials: SelfHostServerCredentials) {
        saveSecureString(
            ACCOUNT_SERVER_CREDENTIALS,
            json.encodeToString(SelfHostServerCredentials.serializer(), credentials)
        )
    }

    actual fun getServerCredentials(): SelfHostServerCredentials? {
        val raw = getSecureString(ACCOUNT_SERVER_CREDENTIALS) ?: return null
        return try {
            json.decodeFromString(SelfHostServerCredentials.serializer(), raw)
        } catch (cause: SerializationException) {
            null
        }
    }

    actual fun clearAll() {
        listOf(ACCOUNT_ENCRYPTION_KEY, ACCOUNT_SERVER_CREDENTIALS).forEach { account ->
            try {
                keyring?.deletePassword(SERVICE_NAME, account)
            } catch (cause: Exception) {
            }
            preferences.remove(fallbackKey(account))
        }
    }

    private fun saveSecureString(account: String, secret: String) {
        try {
            keyring?.setPassword(SERVICE_NAME, account, secret) ?: preferences.put(fallbackKey(account), secret)
        } catch (cause: Exception) {
            preferences.put(fallbackKey(account), secret)
        }
    }

    private fun getSecureString(account: String): String? {
        return try {
            keyring?.getPassword(SERVICE_NAME, account) ?: preferences.get(fallbackKey(account), null)
        } catch (cause: PasswordAccessException) {
            preferences.get(fallbackKey(account), null)
        } catch (cause: Exception) {
            preferences.get(fallbackKey(account), null)
        }
    }

    private fun fallbackKey(account: String) = "SECURE_$account"

    private companion object {
        const val SERVICE_NAME = "EmberrSelfHostSyncVault"
        const val PREFERENCES_NODE = "com.ben.emberr.selfhost"
        const val ACCOUNT_ENCRYPTION_KEY = "encryption_key"
        const val ACCOUNT_SERVER_CREDENTIALS = "server_credentials"
    }
}