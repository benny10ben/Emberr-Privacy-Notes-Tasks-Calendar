package com.ben.emberr.domain.ai.external

import com.github.javakeyring.Keyring
import com.github.javakeyring.PasswordAccessException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.util.prefs.Preferences

actual class SecureAiKeyStorage {

    private val json = Json { ignoreUnknownKeys = true }
    private val preferences = Preferences.userRoot().node(PREFERENCES_NODE)

    private val keyring by lazy {
        try {
            Keyring.create()
        } catch (cause: Exception) {
            null
        }
    }

    actual fun getConfig(provider: ExternalAiProvider): ExternalAiProviderConfig? {
        val raw = getSecureString(provider.name) ?: return null
        return try {
            json.decodeFromString(ExternalAiProviderConfig.serializer(), raw)
        } catch (cause: SerializationException) {
            null
        }
    }

    actual fun saveConfig(provider: ExternalAiProvider, config: ExternalAiProviderConfig) {
        saveSecureString(provider.name, json.encodeToString(ExternalAiProviderConfig.serializer(), config))
    }

    actual fun clearConfig(provider: ExternalAiProvider) {
        try {
            keyring?.deletePassword(SERVICE_NAME, provider.name)
        } catch (cause: Exception) {
        }
        preferences.remove(fallbackKey(provider.name))
    }

    actual fun clearAll() {
        ExternalAiProvider.entries.forEach { clearConfig(it) }
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
        const val SERVICE_NAME = "EmberrAiKeyVault"
        const val PREFERENCES_NODE = "com.ben.emberr.ai"
    }
}
