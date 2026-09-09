package com.ben.emberr.data.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ben.emberr.domain.model.RecurrenceFrequency
import com.ben.emberr.domain.model.RecurrenceRule
import com.ben.emberr.domain.model.isoDayNumberToDayOfWeek
import com.ben.emberr.domain.model.toIsoDayNumberCsv
import kotlinx.serialization.Serializable

enum class TaskSource {
    DAILY,
    NOTE
}

@Serializable
@Entity(
    tableName = "calendar_tasks",
    indices = [
        Index("targetDate"),
        Index("noteId")
    ]
)
data class CalendarTaskEntity(
    @PrimaryKey val blockId: String,
    val noteId: String,
    val text: String,
    val isChecked: Boolean,
    val targetDate: String?,
    val reminderTimestamp: Long?,
    val sourceType: TaskSource,
    val categoryId: String? = null,
    @ColumnInfo(defaultValue = "30") val durationMinutes: Int = 30,
    val url: String? = null,
    val description: String? = null,
    val recurrenceFrequency: RecurrenceFrequency? = null,
    @ColumnInfo(defaultValue = "1") val recurrenceInterval: Int = 1,
    val recurrenceDaysOfWeek: String? = null,
    val recurrenceUntil: String? = null
)

fun CalendarTaskEntity.toRecurrenceRule(): RecurrenceRule? {
    val frequency = recurrenceFrequency ?: return null
    return RecurrenceRule(
        frequency = frequency,
        interval = recurrenceInterval,
        daysOfWeek = recurrenceDaysOfWeek
            ?.split(",")
            ?.mapNotNull { isoDayNumberToDayOfWeek(it.trim().toIntOrNull()) }
            ?.toSet()
            ?: emptySet(),
        untilDateString = recurrenceUntil
    )
}

fun RecurrenceRule.toEntityColumns(): Triple<RecurrenceFrequency, Int, String?> =
    Triple(frequency, interval, daysOfWeek.takeIf { it.isNotEmpty() }?.toIsoDayNumberCsv())