// The saved-state keys, intent actions and extra names shared across this widget's files.
package com.ben.emberr.presentation.widget.tasks

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

val showingCompletedKey = booleanPreferencesKey("showing_completed")
val cachedTasksKey = stringPreferencesKey("cached_tasks")


const val toggleCompletedViewAction = "com.ben.emberr.widget.tasks.TOGGLE_COMPLETED_VIEW"
const val toggleTaskAction = "com.ben.emberr.widget.tasks.TOGGLE_TASK"
