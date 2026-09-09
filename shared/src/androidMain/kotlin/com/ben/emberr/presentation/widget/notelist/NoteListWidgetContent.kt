// The drawable rows of the note list that the widget stores and draws.
package com.ben.emberr.presentation.widget.notelist

import kotlinx.serialization.Serializable

@Serializable
data class NoteListWidgetContent(
    val notes: List<NoteListWidgetRow>
)

@Serializable
data class NoteListWidgetRow(
    val noteId: String,
    val title: String,
    val snippet: String
)
