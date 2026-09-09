// Describes where secrets are currently kept, and names the three groups of secrets.
package com.emberr.core.security.secrets

sealed interface SecretStorageState {

    data object Starting : SecretStorageState

    data class ProtectedByCredentialManager(val backendDisplayName: String) : SecretStorageState

    data class StoredInPlainText(val reason: String, val remedy: String) : SecretStorageState
}

enum class SecretNamespace(val keyringServiceName: String) {
    AiProviders("EmberrAiKeyVault"),
    SelfHostSync("EmberrSelfHostSyncVault"),
    AppSettings("EmberrAppVault")
}
