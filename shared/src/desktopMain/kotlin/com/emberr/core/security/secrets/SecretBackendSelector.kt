// Tries every credential manager this operating system offers and picks the first that passes the probe.
package com.emberr.core.security.secrets

import com.github.javakeyring.KeyringStorageType

class SecretBackendSelector(
    private val probe: SecretBackendProbe,
    private val plaintextBackend: PlaintextSecretBackend
) {

    fun selectUsableBackend(): SecretBackendSelection {
        var atLeastOneManagerAnswered = false

        candidatesForCurrentOperatingSystem().forEach { candidate ->
            val backend = probe.withTimeout(null) { KeyringSecretBackend.openOrNull(candidate.storageType) }
            if (backend != null) {
                atLeastOneManagerAnswered = true
                if (probe.isBackendUsable(backend)) {
                    return SecretBackendSelection(
                        backend = backend,
                        displayName = resolveDisplayNameFor(candidate),
                        unavailableReason = null
                    )
                }
            }
        }

        return SecretBackendSelection(
            backend = plaintextBackend,
            displayName = PLAIN_TEXT_DISPLAY_NAME,
            unavailableReason = if (atLeastOneManagerAnswered) {
                "A credential manager answered but rejected a test write."
            } else {
                "No credential manager is running on this system."
            }
        )
    }

    private fun resolveDisplayNameFor(candidate: KeyringCandidate): String =
        probe.withTimeout(null) { candidate.detectDisplayName() } ?: candidate.fallbackDisplayName

    private fun candidatesForCurrentOperatingSystem(): List<KeyringCandidate> {
        val operatingSystemName = System.getProperty("os.name").orEmpty().lowercase()
        return when {
            operatingSystemName.contains("mac") || operatingSystemName.contains("darwin") -> listOf(
                KeyringCandidate(KeyringStorageType.OSX_KEYCHAIN, "macOS Keychain"),
                KeyringCandidate(KeyringStorageType.LEGACY_OSX_KEYCHAIN, "macOS Keychain")
            )

            operatingSystemName.contains("windows") -> listOf(
                KeyringCandidate(KeyringStorageType.WINDOWS_CREDENTIAL_STORE, "Windows Credential Manager")
            )

            else -> listOf(
                KeyringCandidate(
                    storageType = KeyringStorageType.GNOME_KEYRING,
                    fallbackDisplayName = SecretServiceProviderName.UNRECOGNISED_PROVIDER_NAME,
                    detectDisplayName = { SecretServiceProviderName.detectOrNull() }
                ),
                KeyringCandidate(KeyringStorageType.KWALLET, "KWallet")
            )
        }
    }

    private class KeyringCandidate(
        val storageType: KeyringStorageType,
        val fallbackDisplayName: String,
        val detectDisplayName: () -> String? = { null }
    )

    private companion object {
        const val PLAIN_TEXT_DISPLAY_NAME = "Plain text file"
    }
}

class SecretBackendSelection(
    val backend: SecretBackend,
    val displayName: String,
    val unavailableReason: String?
)
