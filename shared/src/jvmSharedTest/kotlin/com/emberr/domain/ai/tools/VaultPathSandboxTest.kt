// Tests that VaultPathSandbox blocks every way a path could escape the vault folder.

package com.emberr.domain.ai.tools

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class VaultPathSandboxTest {

    private lateinit var vaultRootDirectory: File
    private lateinit var sandbox: VaultPathSandbox

    @BeforeTest
    fun setUp() {
        vaultRootDirectory = Files.createTempDirectory("vault-sandbox-test").toFile()
        sandbox = VaultPathSandbox(vaultRootDirectory)
    }

    @AfterTest
    fun tearDown() {
        vaultRootDirectory.deleteRecursively()
    }

    @Test
    fun blankPathResolvesToTheVaultRoot() {
        val resolution = assertIs<VaultPathResolution.Allowed>(sandbox.resolve(""))
        assertEquals(vaultRootDirectory.canonicalFile, resolution.file)
    }

    @Test
    fun aPlainNestedPathIsAllowed() {
        val resolution = assertIs<VaultPathResolution.Allowed>(sandbox.resolve("Daily/2026-09-12.md"))
        assertEquals(
            File(vaultRootDirectory, "Daily/2026-09-12.md").canonicalFile,
            resolution.file
        )
    }

    @Test
    fun anAbsolutePathIsRejected() {
        val outsideAbsolutePath = File(vaultRootDirectory.parentFile, "outside.md").absolutePath
        assertIs<VaultPathResolution.Rejected>(sandbox.resolve(outsideAbsolutePath))
    }

    @Test
    fun aParentDirectoryEscapeIsRejected() {
        assertIs<VaultPathResolution.Rejected>(sandbox.resolve("../outside.md"))
    }

    @Test
    fun aParentDirectoryEscapeBuriedInsideANestedPathIsRejected() {
        assertIs<VaultPathResolution.Rejected>(sandbox.resolve("Daily/../../outside.md"))
    }

    @Test
    fun aSymlinkPointingOutsideTheVaultIsRejected() {
        val outsideTarget = Files.createTempDirectory("vault-sandbox-outside-test").toFile()
        try {
            val symlink = File(vaultRootDirectory, "escape-link")
            Files.createSymbolicLink(symlink.toPath(), outsideTarget.toPath())

            assertIs<VaultPathResolution.Rejected>(sandbox.resolve("escape-link/note.md"))
        } finally {
            outsideTarget.deleteRecursively()
        }
    }
}
