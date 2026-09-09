package com.emberr.domain.selfhost.crypto

import com.emberr.core.security.secrets.DesktopSecretStore
import com.emberr.core.security.secrets.SecretNamespace
import com.emberr.domain.selfhost.webdav.SelfHostServerCredentials
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.util.Base64

actual class SecureSyncKeyStorage(private val secretStore: DesktopSecretStore) {

    private val json = Json { ignoreUnknownKeys = true }

    actual fun saveEncryptionKey(key: ByteArray) {
        secretStore.writeSecret(
            SecretNamespace.SelfHostSync,
            ACCOUNT_ENCRYPTION_KEY,
            Base64.getEncoder().encodeToString(key)
        )
    }

    actual fun getEncryptionKey(): ByteArray? {
        val encoded = secretStore.readSecret(SecretNamespace.SelfHostSync, ACCOUNT_ENCRYPTION_KEY) ?: return null
        return try {
            Base64.getDecoder().decode(encoded)
        } catch (cause: IllegalArgumentException) {
            null
        }
    }

    actual fun saveServerCredentials(credentials: SelfHostServerCredentials) {
        secretStore.writeSecret(
            SecretNamespace.SelfHostSync,
            ACCOUNT_SERVER_CREDENTIALS,
            json.encodeToString(SelfHostServerCredentials.serializer(), credentials)
        )
    }

    actual fun getServerCredentials(): SelfHostServerCredentials? {
        val raw = secretStore.readSecret(SecretNamespace.SelfHostSync, ACCOUNT_SERVER_CREDENTIALS) ?: return null
        return try {
            json.decodeFromString(SelfHostServerCredentials.serializer(), raw)
        } catch (cause: SerializationException) {
            null
        }
    }

    actual fun clearAll() {
        listOf(ACCOUNT_ENCRYPTION_KEY, ACCOUNT_SERVER_CREDENTIALS).forEach { account ->
            secretStore.removeSecret(SecretNamespace.SelfHostSync, account)
        }
    }

    private companion object {
        const val ACCOUNT_ENCRYPTION_KEY = "encryption_key"
        const val ACCOUNT_SERVER_CREDENTIALS = "server_credentials"
    }
}
