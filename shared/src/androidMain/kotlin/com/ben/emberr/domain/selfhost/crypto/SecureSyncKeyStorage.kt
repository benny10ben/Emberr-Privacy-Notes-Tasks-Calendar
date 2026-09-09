package com.ben.emberr.domain.selfhost.crypto

import android.content.Context
import android.util.Base64
import com.ben.emberr.core.security.AndroidSecretCipher
import com.ben.emberr.core.security.TinkSecretStore
import com.ben.emberr.domain.selfhost.webdav.SelfHostServerCredentials
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

actual class SecureSyncKeyStorage(
    context: Context,
    secretCipher: AndroidSecretCipher
) {

    private val json = Json { ignoreUnknownKeys = true }

    private val secretStore = TinkSecretStore(
        applicationContext = context.applicationContext,
        storeFileName = SECRET_STORE_FILE_NAME,
        secretCipher = secretCipher
    )

    actual fun saveEncryptionKey(key: ByteArray) {
        secretStore.writeSecret(KEY_ENCRYPTION_KEY, Base64.encodeToString(key, Base64.NO_WRAP))
    }

    actual fun getEncryptionKey(): ByteArray? {
        val encoded = secretStore.readSecret(KEY_ENCRYPTION_KEY) ?: return null
        return try {
            Base64.decode(encoded, Base64.NO_WRAP)
        } catch (cause: IllegalArgumentException) {
            null
        }
    }

    actual fun saveServerCredentials(credentials: SelfHostServerCredentials) {
        secretStore.writeSecret(
            KEY_SERVER_CREDENTIALS,
            json.encodeToString(SelfHostServerCredentials.serializer(), credentials)
        )
    }

    actual fun getServerCredentials(): SelfHostServerCredentials? {
        val raw = secretStore.readSecret(KEY_SERVER_CREDENTIALS) ?: return null
        return try {
            json.decodeFromString(SelfHostServerCredentials.serializer(), raw)
        } catch (cause: SerializationException) {
            null
        }
    }

    actual fun clearAll() {
        secretStore.clearAllSecrets()
    }

    private companion object {
        const val SECRET_STORE_FILE_NAME = "emberr_selfhost_sync_vault"
        const val KEY_ENCRYPTION_KEY = "selfhost_encryption_key"
        const val KEY_SERVER_CREDENTIALS = "selfhost_server_credentials"
    }
}
