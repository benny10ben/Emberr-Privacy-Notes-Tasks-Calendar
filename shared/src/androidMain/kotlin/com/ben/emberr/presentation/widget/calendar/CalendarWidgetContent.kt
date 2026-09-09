// The month grid that the widget stores and draws, one row per week.
package com.ben.emberr.presentation.widget.calendar

import kotlinx.serialization.Serializable

@Serializable
data class CalendarWidgetContent(
    val shownMonth: String,
    val monthLabel: String,
    val weekdayLabels: List<String>,
    val weeks: List<List<CalendarDayCell>>
)

@Serializable
data class CalendarDayCell(
    val dateString: String?,
    val dayNumber: Int?,
    val isToday: Boolean,
    val dotColorHexes: List<String>
)
