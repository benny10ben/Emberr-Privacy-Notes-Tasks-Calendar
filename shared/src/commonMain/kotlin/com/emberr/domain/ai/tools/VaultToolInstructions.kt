// System-prompt instructions telling the model the vault markdown format before it writes anything.

package com.emberr.domain.ai.tools

import com.emberr.domain.vault.VaultMarkdownFormatGuide

object VaultToolInstructions {

    val FOR_WRITE_ACCESS: String = """
        You can read and write the user's notes through the tools above. Every note is stored
        as markdown in this exact format. Writes that do not follow it can lose data, so follow
        it exactly when you call create_note, update_note, or append_to_note.

        ${VaultMarkdownFormatGuide.TEXT}
    """.trimIndent()
}
