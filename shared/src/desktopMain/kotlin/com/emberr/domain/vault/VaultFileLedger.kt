// In-memory record of what the app last wrote to each file, so its own writes are not mistaken for edits.

package com.emberr.domain.vault

private class LedgerEntry(
    val noteId: String,
    val markdown: String,
    val noteUpdatedAt: Long
)

// Also holds how old the note was at the time, which gives the merge its base.
class VaultFileLedger {

    private val entriesByPath = mutableMapOf<String, LedgerEntry>()
    private val pathsByNoteId = mutableMapOf<String, String>()

    val isEmpty: Boolean get() = entriesByPath.isEmpty()

    fun recordWrite(noteId: String, path: String, markdown: String, noteUpdatedAt: Long) {
        pathsByNoteId[noteId]?.let { previousPath ->
            if (previousPath != path) entriesByPath.remove(previousPath)
        }
        entriesByPath[path] = LedgerEntry(noteId, markdown, noteUpdatedAt)
        pathsByNoteId[noteId] = path
    }

    fun forgetNote(noteId: String): String? {
        val path = pathsByNoteId.remove(noteId) ?: return null
        entriesByPath.remove(path)
        return path
    }

    fun replaceEverything(writes: List<VaultLedgerWrite>) {
        entriesByPath.clear()
        pathsByNoteId.clear()
        writes.forEach { recordWrite(it.noteId, it.path, it.markdown, it.noteUpdatedAt) }
    }

    fun pathForNote(noteId: String): String? = pathsByNoteId[noteId]

    fun pathsByNote(): Map<String, String> = pathsByNoteId.toMap()

    fun noteIdForPath(path: String): String? = entriesByPath[path]?.noteId

    fun baseMarkdownForPath(path: String): String? = entriesByPath[path]?.markdown

    fun noteUpdatedAtForPath(path: String): Long? = entriesByPath[path]?.noteUpdatedAt

    fun wasWrittenByUs(path: String, markdownOnDisk: String): Boolean =
        entriesByPath[path]?.markdown == markdownOnDisk
}

data class VaultLedgerWrite(
    val noteId: String,
    val path: String,
    val markdown: String,
    val noteUpdatedAt: Long
)
