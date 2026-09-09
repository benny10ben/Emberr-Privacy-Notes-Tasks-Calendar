package com.emberr.domain.ai.external

import com.emberr.core.security.secrets.DesktopSecretStore
import com.emberr.core.security.secrets.SecretNamespace
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

actual class SecureAiKeyStorage(private val secretStore: DesktopSecretStore) {

    private val json = Json { ignoreUnknownKeys = true }

    actual fun getConfig(provider: ExternalAiProvider): ExternalAiProviderConfig? {
        val raw = secretStore.readSecret(SecretNamespace.AiProviders, provider.name) ?: return null
        return try {
            json.decodeFromString(ExternalAiProviderConfig.serializer(), raw)
        } catch (cause: SerializationException) {
            null
        }
    }

    actual fun saveConfig(provider: ExternalAiProvider, config: ExternalAiProviderConfig) {
        secretStore.writeSecret(
            SecretNamespace.AiProviders,
            provider.name,
            json.encodeToString(ExternalAiProviderConfig.serializer(), config)
        )
    }

    actual fun clearConfig(provider: ExternalAiProvider) {
        secretStore.removeSecret(SecretNamespace.AiProviders, provider.name)
    }

    actual fun clearAll() {
        ExternalAiProvider.entries.forEach { provider -> clearConfig(provider) }
    }
}
