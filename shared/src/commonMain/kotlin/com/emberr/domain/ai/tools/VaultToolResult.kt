// What a vault tool call returns, and how to turn that into text the model can read.

package com.emberr.domain.ai.tools

data class VaultNoteSummary(
    val relativePath: String,
    val title: String
)

sealed class VaultToolResult {
    data class Notes(val notes: List<VaultNoteSummary>, val truncated: Boolean = false) : VaultToolResult()
    data class NoteContent(val relativePath: String, val markdown: String) : VaultToolResult()
    data class Proposed(val pendingWrite: VaultPendingWrite) : VaultToolResult()
    data class Failure(val reason: String) : VaultToolResult()
}

fun VaultToolResult.renderForModel(): String = when (this) {
    is VaultToolResult.Notes -> {
        val body = if (notes.isEmpty()) "No notes found." else notes.joinToString("\n") { it.relativePath }
        if (truncated) "$body\n\n(showing the first ${notes.size} matches only, there are more)" else body
    }
    is VaultToolResult.NoteContent -> markdown
    is VaultToolResult.Proposed ->
        "Change proposed to the user for review at \"${pendingWrite.relativePath}\". " +
            "Not yet applied - it will only take effect if the user confirms it."
    is VaultToolResult.Failure -> "Error: $reason"
}
