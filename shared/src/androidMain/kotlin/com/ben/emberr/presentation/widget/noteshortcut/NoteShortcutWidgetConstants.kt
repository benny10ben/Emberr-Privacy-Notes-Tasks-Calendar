// The saved-state keys this widget uses to remember its note and the name it last drew.
package com.ben.emberr.presentation.widget.noteshortcut

import androidx.datastore.preferences.core.stringPreferencesKey

val shortcutNoteIdKey = stringPreferencesKey("shortcut_note_id")
val cachedShortcutKey = stringPreferencesKey("cached_shortcut")
