// The drawable pieces of the task list - section headings and task rows - that the widget stores and draws.
package com.ben.emberr.presentation.widget.tasks

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TasksWidgetContent(
    val isShowingCompleted: Boolean,
    val rows: List<TasksWidgetRow>
)

@Serializable
sealed interface TasksWidgetRow {
    val key: String

    @Serializable
    @SerialName("section")
    data class SectionHeading(
        override val key: String,
        val label: String
    ) : TasksWidgetRow

    @Serializable
    @SerialName("task")
    data class Task(
        override val key: String,
        val blockId: String,
        val text: String,
        val isChecked: Boolean,
        val timeLabel: String?
    ) : TasksWidgetRow
}
