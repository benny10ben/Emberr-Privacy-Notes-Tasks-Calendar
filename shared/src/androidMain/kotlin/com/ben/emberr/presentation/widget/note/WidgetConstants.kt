// The saved-state keys and the intent extra name shared across this widget's files.
package com.ben.emberr.presentation.widget.note

import androidx.datastore.preferences.core.stringPreferencesKey

val selectedNoteIdKey = stringPreferencesKey("selected_note_id")
val cachedContentKey = stringPreferencesKey("cached_content")
