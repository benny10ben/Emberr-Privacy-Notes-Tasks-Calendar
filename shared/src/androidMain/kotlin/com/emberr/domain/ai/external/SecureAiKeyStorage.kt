package com.emberr.domain.ai.external

import android.content.Context
import com.emberr.core.security.AndroidSecretCipher
import com.emberr.core.security.TinkSecretStore
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

actual class SecureAiKeyStorage(
    context: Context,
    secretCipher: AndroidSecretCipher
) {

    private val json = Json { ignoreUnknownKeys = true }

    private val secretStore = TinkSecretStore(
        applicationContext = context.applicationContext,
        storeFileName = SECRET_STORE_FILE_NAME,
        secretCipher = secretCipher
    )

    actual fun getConfig(provider: ExternalAiProvider): ExternalAiProviderConfig? {
        val raw = secretStore.readSecret(configKey(provider)) ?: return null
        return try {
            json.decodeFromString(ExternalAiProviderConfig.serializer(), raw)
        } catch (cause: SerializationException) {
            null
        }
    }

    actual fun saveConfig(provider: ExternalAiProvider, config: ExternalAiProviderConfig) {
        secretStore.writeSecret(
            configKey(provider),
            json.encodeToString(ExternalAiProviderConfig.serializer(), config)
        )
    }

    actual fun clearConfig(provider: ExternalAiProvider) {
        secretStore.removeSecret(configKey(provider))
    }

    actual fun clearAll() {
        secretStore.clearAllSecrets()
    }

    private fun configKey(provider: ExternalAiProvider) = "ai_provider_config_${provider.name}"

    private companion object {
        const val SECRET_STORE_FILE_NAME = "emberr_ai_key_vault"
    }
}
