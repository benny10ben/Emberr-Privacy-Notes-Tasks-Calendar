// The vault tools an AI can call, with the description each provider adapter turns into its own schema.

package com.emberr.domain.ai.tools

enum class VaultToolAccessLevel { READ, WRITE }

data class VaultToolParameter(
    val name: String,
    val description: String,
    val isRequired: Boolean = true
)

data class VaultToolDefinition(
    val name: String,
    val description: String,
    val accessLevel: VaultToolAccessLevel,
    val parameters: List<VaultToolParameter>
)

object VaultTools {

    val listNotes = VaultToolDefinition(
        name = "list_notes",
        description = "List the notes inside a folder of the vault. Use an empty folder_path for the vault root.",
        accessLevel = VaultToolAccessLevel.READ,
        parameters = listOf(
            VaultToolParameter(
                name = "folder_path",
                description = "Folder path relative to the vault root, e.g. \"Daily\" or \"Work/Projects\". Empty for the vault root.",
                isRequired = false
            )
        )
    )

    val readNote = VaultToolDefinition(
        name = "read_note",
        description = "Read the full markdown content of one note.",
        accessLevel = VaultToolAccessLevel.READ,
        parameters = listOf(
            VaultToolParameter(
                name = "path",
                description = "Note path relative to the vault root, e.g. \"Daily/2026-09-12.md\"."
            )
        )
    )

    val searchNotes = VaultToolDefinition(
        name = "search_notes",
        description = "Search every note in the vault for a case-insensitive text match and return the matching note paths.",
        accessLevel = VaultToolAccessLevel.READ,
        parameters = listOf(
            VaultToolParameter(
                name = "query",
                description = "Text to search for."
            )
        )
    )

    val createNote = VaultToolDefinition(
        name = "create_note",
        description = "Create a new note at the given path with the given markdown content. Fails if a note " +
            "already exists there. Content does not need `^em-xxxx` tags - they are added automatically.",
        accessLevel = VaultToolAccessLevel.WRITE,
        parameters = listOf(
            VaultToolParameter(name = "path", description = "Path for the new note, relative to the vault root."),
            VaultToolParameter(name = "content", description = "Full markdown content for the new note.")
        )
    )

    val updateNote = VaultToolDefinition(
        name = "update_note",
        description = "Replace the full markdown content of an existing note. Preserve every existing " +
            "`^em-xxxx` block tag exactly as it appeared when you read the note - do not remove, change, " +
            "or invent one. Only content that is genuinely new can be left untagged.",
        accessLevel = VaultToolAccessLevel.WRITE,
        parameters = listOf(
            VaultToolParameter(name = "path", description = "Path of the note to replace, relative to the vault root."),
            VaultToolParameter(name = "content", description = "New full markdown content for the note.")
        )
    )

    val appendToNote = VaultToolDefinition(
        name = "append_to_note",
        description = "Append markdown content to the end of an existing note. The appended content does " +
            "not need a `^em-xxxx` tag - one is added automatically.",
        accessLevel = VaultToolAccessLevel.WRITE,
        parameters = listOf(
            VaultToolParameter(name = "path", description = "Path of the note to append to, relative to the vault root."),
            VaultToolParameter(name = "content", description = "Markdown content to append.")
        )
    )

    val deleteNote = VaultToolDefinition(
        name = "delete_note",
        description = "Delete an existing note.",
        accessLevel = VaultToolAccessLevel.WRITE,
        parameters = listOf(
            VaultToolParameter(name = "path", description = "Path of the note to delete, relative to the vault root.")
        )
    )

    val all: List<VaultToolDefinition> = listOf(
        listNotes, readNote, searchNotes, createNote, updateNote, appendToNote, deleteNote
    )

    val readOnly: List<VaultToolDefinition> = all.filter { it.accessLevel == VaultToolAccessLevel.READ }
}
