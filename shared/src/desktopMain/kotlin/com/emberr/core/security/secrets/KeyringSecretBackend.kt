// Stores secrets in the operating system's own credential manager.
package com.emberr.core.security.secrets

import com.github.javakeyring.Keyring
import com.github.javakeyring.KeyringStorageType

class KeyringSecretBackend private constructor(private val keyring: Keyring) : SecretBackend {

    override fun readSecret(service: String, account: String): String? =
        try {
            keyring.getPassword(service, account)
        } catch (cause: Exception) {
            null
        }

    override fun writeSecret(service: String, account: String, secret: String) {
        keyring.setPassword(service, account, secret)
    }

    override fun removeSecret(service: String, account: String) {
        try {
            keyring.deletePassword(service, account)
        } catch (cause: Exception) {
        }
    }

    companion object {
        fun openOrNull(storageType: KeyringStorageType): KeyringSecretBackend? =
            try {
                KeyringSecretBackend(Keyring.create(storageType))
            } catch (cause: Exception) {
                null
            }
    }
}
