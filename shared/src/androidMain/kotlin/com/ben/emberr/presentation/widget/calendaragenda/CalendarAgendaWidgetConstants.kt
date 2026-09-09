// The saved-state keys and intent actions this widget uses to remember and change the shown month.
package com.ben.emberr.presentation.widget.calendaragenda

import androidx.datastore.preferences.core.stringPreferencesKey

val agendaShownMonthKey = stringPreferencesKey("agenda_shown_month")
val cachedAgendaKey = stringPreferencesKey("cached_agenda")

const val showPreviousAgendaMonthAction = "com.ben.emberr.widget.calendaragenda.SHOW_PREVIOUS_MONTH"
const val showNextAgendaMonthAction = "com.ben.emberr.widget.calendaragenda.SHOW_NEXT_MONTH"
