// The saved-state key this widget uses to remember the day it last drew.
package com.emberr.presentation.widget.todaytasks

import androidx.datastore.preferences.core.stringPreferencesKey

val cachedTodayTasksKey = stringPreferencesKey("cached_today_tasks")

const val toggleTodayTaskAction = "com.emberr.widget.todaytasks.TOGGLE_TASK"
