// Locks files and folders down so only the current user account can read them.
package com.emberr.core.security

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFileAttributeView
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions

object OwnerOnlyFilePermissions {

    private const val OWNER_ONLY_FILE = "rw-------"
    private const val OWNER_ONLY_DIRECTORY = "rwx------"

    fun isSupportedOn(path: Path): Boolean =
        Files.getFileAttributeView(path, PosixFileAttributeView::class.java) != null

    fun createFileReadableOnlyByOwner(path: Path) {
        if (isSupportedOn(path)) {
            Files.createFile(path, PosixFilePermissions.asFileAttribute(ownerOnlyFilePermissions()))
        } else {
            Files.createFile(path)
        }
    }

    fun restrictFileToOwner(path: Path): Boolean = applyPermissions(path, OWNER_ONLY_FILE)

    fun restrictDirectoryToOwner(path: Path): Boolean = applyPermissions(path, OWNER_ONLY_DIRECTORY)

    private fun applyPermissions(path: Path, permissions: String): Boolean {
        val posixView = Files.getFileAttributeView(path, PosixFileAttributeView::class.java) ?: return false
        return runCatching { posixView.setPermissions(PosixFilePermissions.fromString(permissions)) }.isSuccess
    }

    private fun ownerOnlyFilePermissions(): Set<PosixFilePermission> =
        PosixFilePermissions.fromString(OWNER_ONLY_FILE)
}
