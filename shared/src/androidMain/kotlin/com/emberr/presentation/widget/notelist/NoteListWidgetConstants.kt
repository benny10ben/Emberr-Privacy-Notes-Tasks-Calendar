// The saved-state key this widget uses to remember the note list it last drew.
package com.emberr.presentation.widget.notelist

import androidx.datastore.preferences.core.stringPreferencesKey

val cachedNoteListKey = stringPreferencesKey("cached_note_list")
