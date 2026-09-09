// The date heading and task rows that the widget stores and draws.
package com.ben.emberr.presentation.widget.todaytasks

import kotlinx.serialization.Serializable

@Serializable
data class TodayTasksWidgetContent(
    val dateLabel: String,
    val rows: List<TodayTaskRow>
)

@Serializable
data class TodayTaskRow(
    val blockId: String,
    val title: String,
    val isChecked: Boolean,
    val scheduleLabel: String?,
    val isOverdue: Boolean
)
