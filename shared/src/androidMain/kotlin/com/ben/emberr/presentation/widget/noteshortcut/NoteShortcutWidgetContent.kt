// The note name and icon that the widget stores and draws.
package com.ben.emberr.presentation.widget.noteshortcut

import kotlinx.serialization.Serializable

@Serializable
data class NoteShortcutWidgetContent(
    val noteId: String,
    val title: String,
    val icon: String?
)
