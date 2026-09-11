// On-disk record of which file and directory each note and folder owns, so removals survive a restart.

package com.emberr.domain.vault

import java.io.File
import java.io.IOException

private const val MEMORY_FILE_NAME = "vault-paths.txt"
private const val NOTE_KIND = "note"
private const val FOLDER_KIND = "folder"

data class VaultPathSnapshot(
    val noteFilesByNoteId: Map<String, String> = emptyMap(),
    val folderDirectoriesByFolderId: Map<String, String> = emptyMap()
) {
    val isEmpty: Boolean get() = noteFilesByNoteId.isEmpty() && folderDirectoriesByFolderId.isEmpty()
}

// A recorded path with nothing behind it means it was removed while the app was not watching.
// Lives beside the vault, so it is never mistaken for a note.
class VaultPathMemory(emberrDirectory: File) {

    private val memoryFile = File(emberrDirectory, MEMORY_FILE_NAME)

    fun load(): VaultPathSnapshot {
        if (!memoryFile.isFile) return VaultPathSnapshot()

        return try {
            val noteFiles = mutableMapOf<String, String>()
            val folderDirectories = mutableMapOf<String, String>()

            for (line in memoryFile.readLines()) {
                val parts = line.split('\t')
                if (parts.size != 3) continue
                val (kind, id, path) = parts
                if (id.isBlank() || path.isBlank()) continue

                when (kind) {
                    NOTE_KIND -> noteFiles[id] = path
                    FOLDER_KIND -> folderDirectories[id] = path
                }
            }
            VaultPathSnapshot(noteFiles, folderDirectories)
        } catch (cause: IOException) {
            VaultLog.e("Could not read $MEMORY_FILE_NAME: ${cause.message}")
            VaultPathSnapshot()
        }
    }

    fun save(snapshot: VaultPathSnapshot) {
        try {
            val lines = snapshot.noteFilesByNoteId.map { (id, path) -> "$NOTE_KIND\t$id\t$path" } +
                snapshot.folderDirectoriesByFolderId.map { (id, path) -> "$FOLDER_KIND\t$id\t$path" }
            memoryFile.writeText(if (lines.isEmpty()) "" else lines.joinToString("\n") + "\n")
        } catch (cause: IOException) {
            VaultLog.e("Could not write $MEMORY_FILE_NAME: ${cause.message}")
        }
    }
}
