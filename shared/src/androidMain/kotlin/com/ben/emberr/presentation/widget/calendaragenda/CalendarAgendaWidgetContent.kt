// The compact month grid and the month's event list that the widget stores and draws.
package com.ben.emberr.presentation.widget.calendaragenda

import kotlinx.serialization.Serializable

@Serializable
data class CalendarAgendaWidgetContent(
    val shownMonth: String,
    val monthLabel: String,
    val weekdayLabels: List<String>,
    val weeks: List<List<AgendaDayCell>>,
    val events: List<AgendaEvent>
)

@Serializable
data class AgendaDayCell(
    val dayNumber: Int?,
    val isToday: Boolean,
    val hasEvents: Boolean
)

@Serializable
data class AgendaEvent(
    val blockId: String,
    val title: String,
    val whenLabel: String,
    val isDone: Boolean,
    val accentColorHex: String?
)
