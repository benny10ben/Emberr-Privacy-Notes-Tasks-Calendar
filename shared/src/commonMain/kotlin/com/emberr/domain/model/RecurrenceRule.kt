package com.emberr.domain.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.number
import kotlinx.serialization.Serializable

enum class RecurrenceFrequency { DAILY, WEEKLY, MONTHLY, YEARLY }

enum class RecurrenceEditScope { THIS_EVENT, ALL_FUTURE_EVENTS, ALL_PAST_EVENTS, ALL_EVENTS }

@Serializable
data class RecurrenceRule(
    val frequency: RecurrenceFrequency,
    val interval: Int = 1,
    val daysOfWeek: Set<DayOfWeek> = emptySet(),
    val untilDateString: String? = null
)

fun isoDayNumberToDayOfWeek(isoDayNumber: Int?): DayOfWeek? =
    DayOfWeek.entries.firstOrNull { it.isoDayNumber == isoDayNumber }

fun Set<DayOfWeek>.toIsoDayNumberCsv(): String =
    joinToString(",") { it.isoDayNumber.toString() }

object RecurrenceEngine {

    fun occursOn(rule: RecurrenceRule, anchor: LocalDate, candidate: LocalDate): Boolean {
        if (candidate < anchor) return false
        rule.untilDateString?.let { until -> if (candidate > LocalDate.parse(until)) return false }

        return when (rule.frequency) {
            RecurrenceFrequency.DAILY -> anchor.daysUntil(candidate) % rule.interval == 0
            RecurrenceFrequency.WEEKLY -> occursOnWeekly(rule, anchor, candidate)
            RecurrenceFrequency.MONTHLY -> occursOnMonthly(rule, anchor, candidate)
            RecurrenceFrequency.YEARLY -> occursOnYearly(rule, anchor, candidate)
        }
    }

    private fun weekStart(date: LocalDate): LocalDate =
        date.minus(date.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)

    private fun occursOnWeekly(rule: RecurrenceRule, anchor: LocalDate, candidate: LocalDate): Boolean {
        val targetDays = rule.daysOfWeek.ifEmpty { setOf(anchor.dayOfWeek) }
        if (candidate.dayOfWeek !in targetDays) return false
        val weeksBetween = weekStart(anchor).daysUntil(weekStart(candidate)) / 7
        return weeksBetween % rule.interval == 0
    }

    private fun daysInMonth(year: Int, month: Month): Int {
        val firstOfMonth = LocalDate(year, month, 1)
        return firstOfMonth.daysUntil(firstOfMonth.plus(1, DateTimeUnit.MONTH))
    }

    private fun occursOnMonthly(rule: RecurrenceRule, anchor: LocalDate, candidate: LocalDate): Boolean {
        val targetDay = minOf(anchor.day, daysInMonth(candidate.year, candidate.month))
        if (candidate.day != targetDay) return false
        val monthsBetween = (candidate.year - anchor.year) * 12 + (candidate.month.number - anchor.month.number)
        return monthsBetween % rule.interval == 0
    }

    private fun occursOnYearly(rule: RecurrenceRule, anchor: LocalDate, candidate: LocalDate): Boolean {
        if (candidate.month.number != anchor.month.number) return false
        val targetDay = minOf(anchor.day, daysInMonth(candidate.year, candidate.month))
        if (candidate.day != targetDay) return false
        val yearsBetween = candidate.year - anchor.year
        return yearsBetween % rule.interval == 0
    }

    fun occurrenceDatesInRange(rule: RecurrenceRule, anchor: LocalDate, rangeStart: LocalDate, rangeEnd: LocalDate): List<LocalDate> {
        var cursor = if (rangeStart < anchor) anchor else rangeStart
        if (cursor > rangeEnd) return emptyList()

        val result = mutableListOf<LocalDate>()
        while (cursor <= rangeEnd) {
            if (occursOn(rule, anchor, cursor)) result.add(cursor)
            cursor = cursor.plus(1, DateTimeUnit.DAY)
        }
        return result
    }

    fun retargetTimestampToDate(
        originalTimestamp: Long,
        newDate: LocalDate,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): Long {
        val originalDateTime = Instant.fromEpochMilliseconds(originalTimestamp).toLocalDateTime(timeZone)
        val retargeted = LocalDateTime(
            newDate.year, newDate.month.number, newDate.day,
            originalDateTime.hour, originalDateTime.minute, originalDateTime.second
        )
        return retargeted.toInstant(timeZone).toEpochMilliseconds()
    }

    fun previousOccurrence(rule: RecurrenceRule, anchor: LocalDate, before: LocalDate): LocalDate? {
        var cursor = before.minus(1, DateTimeUnit.DAY)
        while (cursor >= anchor) {
            if (occursOn(rule, anchor, cursor)) return cursor
            cursor = cursor.minus(1, DateTimeUnit.DAY)
        }
        return null
    }

    private const val MAX_FORWARD_SCAN_DAYS = 3660

    fun nextOccurrence(rule: RecurrenceRule, anchor: LocalDate, after: LocalDate): LocalDate? {
        var cursor = after.plus(1, DateTimeUnit.DAY)
        val until = rule.untilDateString?.let { LocalDate.parse(it) }
        val hardLimit = after.plus(MAX_FORWARD_SCAN_DAYS, DateTimeUnit.DAY)
        val scanLimit = if (until != null && until < hardLimit) until else hardLimit

        while (cursor <= scanLimit) {
            if (occursOn(rule, anchor, cursor)) return cursor
            cursor = cursor.plus(1, DateTimeUnit.DAY)
        }
        return null
    }
}
