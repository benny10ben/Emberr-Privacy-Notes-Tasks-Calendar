// The date-grouped upcoming events that the widget stores and draws.
package com.ben.emberr.presentation.widget.upcomingevents

import kotlinx.serialization.Serializable

@Serializable
data class UpcomingEventsWidgetContent(
    val events: List<UpcomingEvent>
)

@Serializable
data class UpcomingEvent(
    val blockId: String,
    val title: String,
    val dateGroupKey: String,
    val dateLabel: String,
    val timeLabel: String?,
    val isOverdue: Boolean,
    val accentColorHex: String?
)
