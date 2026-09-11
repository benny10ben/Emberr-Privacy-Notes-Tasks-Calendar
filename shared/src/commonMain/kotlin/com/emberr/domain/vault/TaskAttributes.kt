// The {due: ...; repeat: ...} group carrying a checkbox's due date, category, repeat rule, link and details.

package com.emberr.domain.vault

import com.emberr.domain.model.RecurrenceFrequency
import com.emberr.domain.model.RecurrenceRule
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

const val DEFAULT_TASK_DURATION_MINUTES = 30

private const val DUE_KEY = "due"
private const val DURATION_KEY = "for"
private const val CATEGORY_KEY = "category"
private const val REPEAT_KEY = "repeat"
private const val LINK_KEY = "link"
private const val DETAILS_KEY = "details"

private val knownKeys = setOf(DUE_KEY, DURATION_KEY, CATEGORY_KEY, REPEAT_KEY, LINK_KEY, DETAILS_KEY)
private val dayNameRegex = "(?:mon|tue|wed|thu|fri|sat|sun)"

// {due: 2026-09-12 14:00; for: 45m; category: Work; repeat: weekly on mon}
data class TaskAttributes(
    val dueText: String? = null,
    val durationMinutes: Int? = null,
    val categoryName: String? = null,
    val repeatText: String? = null,
    val link: String? = null,
    val details: String? = null
) {
    val isEmpty: Boolean
        get() = dueText == null && durationMinutes == null && categoryName == null &&
            repeatText == null && link == null && details == null
}

data class TaskTextAndAttributes(
    val text: String,
    val attributes: TaskAttributes
)

object TaskAttributesFormat {

    fun render(attributes: TaskAttributes): String? {
        if (attributes.isEmpty) return null

        val fields = mutableListOf<String>()
        attributes.dueText?.let { fields.add("$DUE_KEY: ${escapeValue(it)}") }
        attributes.durationMinutes?.let { fields.add("$DURATION_KEY: ${it}m") }
        attributes.categoryName?.let { fields.add("$CATEGORY_KEY: ${escapeValue(it)}") }
        attributes.repeatText?.let { fields.add("$REPEAT_KEY: ${escapeValue(it)}") }
        attributes.link?.let { fields.add("$LINK_KEY: ${escapeValue(it)}") }
        attributes.details?.let { fields.add("$DETAILS_KEY: ${escapeValue(it)}") }

        return "{" + fields.joinToString("; ") + "}"
    }

    // Pulls a trailing group off a line. An unknown key means the braces are ordinary text.
    fun extractFrom(text: String): TaskTextAndAttributes {
        val withoutTrailingSpace = text.trimEnd()
        if (!withoutTrailingSpace.endsWith("}")) return TaskTextAndAttributes(text, TaskAttributes())

        val openingIndex = lastUnescapedOpeningBrace(withoutTrailingSpace)
        if (openingIndex < 0) return TaskTextAndAttributes(text, TaskAttributes())

        val inside = withoutTrailingSpace.substring(openingIndex + 1, withoutTrailingSpace.length - 1)
        val fields = parseFields(inside) ?: return TaskTextAndAttributes(text, TaskAttributes())

        val attributes = TaskAttributes(
            dueText = fields[DUE_KEY]?.takeIf { it.isNotBlank() },
            durationMinutes = fields[DURATION_KEY]?.let { parseDurationMinutes(it) },
            categoryName = fields[CATEGORY_KEY]?.takeIf { it.isNotBlank() },
            repeatText = fields[REPEAT_KEY]?.takeIf { it.isNotBlank() },
            link = fields[LINK_KEY]?.takeIf { it.isNotBlank() },
            details = fields[DETAILS_KEY]?.takeIf { it.isNotBlank() }
        )
        if (attributes.isEmpty) return TaskTextAndAttributes(text, TaskAttributes())

        return TaskTextAndAttributes(
            text = withoutTrailingSpace.substring(0, openingIndex).trimEnd(),
            attributes = attributes
        )
    }

    // A value may hold escaped braces, so this skips those and takes the last unescaped one.
    private fun lastUnescapedOpeningBrace(text: String): Int {
        var lastIndex = -1
        var index = 0

        while (index < text.length) {
            val character = text[index]
            if (character == '\\') {
                index += 2
                continue
            }
            if (character == '{') lastIndex = index
            index++
        }
        return lastIndex
    }

    private fun parseFields(inside: String): Map<String, String>? {
        if (inside.isBlank()) return null

        val fields = LinkedHashMap<String, String>()
        for (part in splitOnUnescapedSemicolons(inside)) {
            val separatorIndex = part.indexOf(':')
            if (separatorIndex <= 0) return null

            val key = part.substring(0, separatorIndex).trim().lowercase()
            if (key !in knownKeys) return null
            fields[key] = unescapeValue(part.substring(separatorIndex + 1).trim())
        }
        return fields.ifEmpty { null }
    }

    private fun splitOnUnescapedSemicolons(text: String): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var index = 0

        while (index < text.length) {
            val character = text[index]
            when {
                character == '\\' && index + 1 < text.length -> {
                    current.append(character).append(text[index + 1])
                    index += 2
                }
                character == ';' -> {
                    parts.add(current.toString())
                    current.clear()
                    index++
                }
                else -> {
                    current.append(character)
                    index++
                }
            }
        }
        parts.add(current.toString())
        return parts.filter { it.isNotBlank() }
    }

    private fun escapeValue(value: String): String = buildString(value.length) {
        for (character in value) {
            when {
                character == '\n' -> append("\\n")
                character == '\r' -> Unit
                character == ';' || character == '{' || character == '}' || character == '\\' -> {
                    append('\\')
                    append(character)
                }
                else -> append(character)
            }
        }
    }

    private fun unescapeValue(value: String): String = buildString(value.length) {
        var index = 0
        while (index < value.length) {
            val character = value[index]
            if (character == '\\' && index + 1 < value.length) {
                val escaped = value[index + 1]
                append(if (escaped == 'n') '\n' else escaped)
                index += 2
            } else {
                append(character)
                index++
            }
        }
    }

    private fun parseDurationMinutes(value: String): Int? =
        value.trim().removeSuffix("m").trim().toIntOrNull()?.takeIf { it > 0 }
}

object TaskDueTimeFormat {

    fun render(epochMilliseconds: Long, timeZone: TimeZone): String {
        val local = Instant.fromEpochMilliseconds(epochMilliseconds).toLocalDateTime(timeZone)
        val date = "${pad(local.year, 4)}-${pad(local.month.number, 2)}-${pad(local.day, 2)}"
        val time = "${pad(local.hour, 2)}:${pad(local.minute, 2)}"

        return if (local.second == 0) "$date $time" else "$date $time:${pad(local.second, 2)}"
    }

    fun parse(value: String, timeZone: TimeZone): Long? {
        val parts = value.trim().split(' ').filter { it.isNotBlank() }
        if (parts.isEmpty()) return null

        return try {
            val date = LocalDate.parse(parts[0])
            val time = if (parts.size > 1) LocalTime.parse(padSecondsIfMissing(parts[1])) else LocalTime(0, 0)
            LocalDateTime(date, time).toInstant(timeZone).toEpochMilliseconds()
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun padSecondsIfMissing(time: String): String =
        if (time.count { it == ':' } == 1) "$time:00" else time

    private fun pad(value: Int, width: Int): String = value.toString().padStart(width, '0')
}

object TaskRepeatFormat {

    fun render(rule: RecurrenceRule): String {
        val head = if (rule.interval <= 1) {
            when (rule.frequency) {
                RecurrenceFrequency.DAILY -> "daily"
                RecurrenceFrequency.WEEKLY -> "weekly"
                RecurrenceFrequency.MONTHLY -> "monthly"
                RecurrenceFrequency.YEARLY -> "yearly"
            }
        } else {
            val unit = when (rule.frequency) {
                RecurrenceFrequency.DAILY -> "days"
                RecurrenceFrequency.WEEKLY -> "weeks"
                RecurrenceFrequency.MONTHLY -> "months"
                RecurrenceFrequency.YEARLY -> "years"
            }
            "every ${rule.interval} $unit"
        }

        val days = if (rule.daysOfWeek.isEmpty()) {
            ""
        } else {
            " on " + rule.daysOfWeek.sortedBy { it.isoDayNumber }.joinToString(",") { shortDayName(it) }
        }
        val until = rule.untilDateString?.let { " until $it" }.orEmpty()

        return head + days + until
    }

    fun parse(value: String): RecurrenceRule? {
        val lower = value.lowercase().trim()
        if (lower.isEmpty()) return null

        val frequency = when {
            lower.contains("daily") || Regex("""\bdays?\b""").containsMatchIn(lower) -> RecurrenceFrequency.DAILY
            lower.contains("weekly") || Regex("""\bweeks?\b""").containsMatchIn(lower) -> RecurrenceFrequency.WEEKLY
            lower.contains("monthly") || Regex("""\bmonths?\b""").containsMatchIn(lower) -> RecurrenceFrequency.MONTHLY
            lower.contains("yearly") || Regex("""\byears?\b""").containsMatchIn(lower) -> RecurrenceFrequency.YEARLY
            else -> return null
        }

        val interval = Regex("""every\s+(\d{1,3})""").find(lower)
            ?.groupValues?.get(1)?.toIntOrNull()?.coerceAtLeast(1) ?: 1

        val daysText = Regex("""on\s+($dayNameRegex(?:\s*,\s*$dayNameRegex)*)""").find(lower)?.groupValues?.get(1)
        val daysOfWeek = daysText
            ?.split(',')
            ?.mapNotNull { dayOfWeekNamed(it.trim()) }
            ?.toSet()
            .orEmpty()

        val untilDateString = Regex("""until\s+(\d{4}-\d{2}-\d{2})""").find(lower)?.groupValues?.get(1)

        return RecurrenceRule(
            frequency = frequency,
            interval = interval,
            daysOfWeek = daysOfWeek,
            untilDateString = untilDateString
        )
    }

    private fun shortDayName(day: DayOfWeek): String = day.name.take(3).lowercase()

    private fun dayOfWeekNamed(name: String): DayOfWeek? =
        DayOfWeek.entries.firstOrNull { it.name.take(3).equals(name, ignoreCase = true) }
}
