package com.ben.emberr.presentation.reminders

import com.ben.emberr.data.local.room.CalendarEventExceptionDao
import com.ben.emberr.data.local.room.CalendarEventExceptionEntity
import com.ben.emberr.data.local.room.CalendarTaskDao
import com.ben.emberr.data.local.room.CalendarTaskEntity
import com.ben.emberr.data.local.room.NoteDao
import com.ben.emberr.data.local.room.NoteMetadataEntity
import com.ben.emberr.data.local.room.toRecurrenceRule
import com.ben.emberr.domain.model.RecurrenceEngine
import kotlinx.coroutines.flow.first
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

private const val RESCHEDULE_HORIZON_DAYS = 60

private data class PendingReminder(
    val blockId: String,
    val noteId: String,
    val text: String,
    val timestamp: Long
)

class ReminderRescheduler(
    private val calendarTaskDao: CalendarTaskDao,
    private val calendarEventExceptionDao: CalendarEventExceptionDao,
    private val noteDao: NoteDao,
    private val reminderScheduler: ReminderScheduler
) {

    suspend fun rescheduleUpcomingReminders() {
        try {
            val nowMillis = Clock.System.now().toEpochMilliseconds()
            val tasks = calendarTaskDao.getAllTasks()
            if (tasks.isEmpty()) return

            val exceptions = calendarEventExceptionDao.getAllExceptionsFlow().first()
            val exceptionsByOccurrence = exceptions.associateBy { it.blockId to it.occurrenceDate }

            val pendingReminders = tasks.mapNotNull { task ->
                findNextReminder(task, exceptionsByOccurrence, nowMillis)
            }
            if (pendingReminders.isEmpty()) return

            val notesById = noteDao
                .getNotesByIds(pendingReminders.map { it.noteId }.distinct())
                .associateBy { it.noteId }

            pendingReminders.forEach { reminder ->
                val note = notesById[reminder.noteId]
                if (note != null && note.trashedAt == null) {
                    reminderScheduler.schedule(
                        blockId = reminder.blockId,
                        noteTitle = buildNotificationTitle(note),
                        text = reminder.text.ifBlank { "Unfinished task" },
                        timestamp = reminder.timestamp
                    )
                }
            }
        } catch (error: Exception) {
            error.printStackTrace()
        }
    }

    private fun findNextReminder(
        task: CalendarTaskEntity,
        exceptionsByOccurrence: Map<Pair<String, String>, CalendarEventExceptionEntity>,
        nowMillis: Long
    ): PendingReminder? {
        val recurrenceRule = task.toRecurrenceRule()

        if (recurrenceRule == null) {
            if (task.isChecked) return null
            val timestamp = task.reminderTimestamp ?: return null
            if (timestamp <= nowMillis) return null
            return PendingReminder(task.blockId, task.noteId, task.text, timestamp)
        }

        val baseTimestamp = task.reminderTimestamp ?: return null
        val anchor = task.targetDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null

        val timeZone = TimeZone.currentSystemDefault()
        val today = Clock.System.now().toLocalDateTime(timeZone).date
        val horizonEnd = today.plus(RESCHEDULE_HORIZON_DAYS, DateTimeUnit.DAY)

        val occurrenceDates = RecurrenceEngine.occurrenceDatesInRange(
            rule = recurrenceRule,
            anchor = anchor,
            rangeStart = today,
            rangeEnd = horizonEnd
        )

        for (occurrenceDate in occurrenceDates) {
            val exception = exceptionsByOccurrence[task.blockId to occurrenceDate.toString()]
            if (exception?.isCancelled == true) continue
            if (exception?.isChecked == true) continue

            val occurrenceTimestamp = exception?.overrideTimestamp
                ?: RecurrenceEngine.retargetTimestampToDate(baseTimestamp, occurrenceDate)
            if (occurrenceTimestamp <= nowMillis) continue

            return PendingReminder(
                blockId = task.blockId,
                noteId = task.noteId,
                text = exception?.overrideText ?: task.text,
                timestamp = occurrenceTimestamp
            )
        }
        return null
    }

    private fun buildNotificationTitle(note: NoteMetadataEntity): String = when {
        note.isDaily -> "Daily: ${note.dateString.orEmpty()}"
        note.title.isNotBlank() -> note.title
        else -> "Task Reminder"
    }
}
