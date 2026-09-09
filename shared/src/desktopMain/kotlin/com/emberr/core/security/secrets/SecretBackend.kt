// The common shape of any place Emberr can store a secret.
package com.emberr.core.security.secrets

interface SecretBackend {

    fun readSecret(service: String, account: String): String?

    fun writeSecret(service: String, account: String, secret: String)

    fun removeSecret(service: String, account: String)
}
