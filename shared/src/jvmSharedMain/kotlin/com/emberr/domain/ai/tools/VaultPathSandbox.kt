// Resolves a vault-relative path and rejects anything that would escape the vault folder.

package com.emberr.domain.ai.tools

import java.io.File

sealed class VaultPathResolution {
    data class Allowed(val file: File) : VaultPathResolution()
    data class Rejected(val reason: String) : VaultPathResolution()
}

class VaultPathSandbox(private val vaultRootDirectory: File) {

    fun resolve(relativePath: String): VaultPathResolution {
        if (relativePath.isBlank()) {
            return VaultPathResolution.Allowed(vaultRootDirectory.canonicalFile)
        }
        if (File(relativePath).isAbsolute) {
            return VaultPathResolution.Rejected("Path must be relative to the vault: $relativePath")
        }

        val canonicalRoot = vaultRootDirectory.canonicalFile
        val candidate = File(canonicalRoot, relativePath).canonicalFile
        val isInsideVault = candidate == canonicalRoot ||
            candidate.path.startsWith(canonicalRoot.path + File.separator)

        return if (isInsideVault) {
            VaultPathResolution.Allowed(candidate)
        } else {
            VaultPathResolution.Rejected("Path escapes the vault: $relativePath")
        }
    }
}
