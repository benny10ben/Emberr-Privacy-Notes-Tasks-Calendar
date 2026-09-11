// Checks the due date, repeat rule and value escaping in a checkbox's attribute group.

package com.emberr.domain.vault

import com.emberr.domain.model.RecurrenceFrequency
import com.emberr.domain.model.RecurrenceRule
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TaskAttributesTest {

    private val utc = TimeZone.UTC

    @Test
    fun anEmptyGroupIsNotWrittenAtAll() {
        assertNull(TaskAttributesFormat.render(TaskAttributes()))
    }

    @Test
    fun everyFieldIsWrittenInAFixedOrder() {
        val rendered = TaskAttributesFormat.render(
            TaskAttributes(
                dueText = "2026-09-12 14:00",
                durationMinutes = 45,
                categoryName = "Work",
                repeatText = "weekly on mon"
            )
        )

        assertEquals(
            "{due: 2026-09-12 14:00; for: 45m; category: Work; repeat: weekly on mon}",
            rendered
        )
    }

    @Test
    fun aGroupIsPulledOffTheEndOfTheLine() {
        val split = TaskAttributesFormat.extractFrom("Email finance team {due: 2026-09-12 14:00}")

        assertEquals("Email finance team", split.text)
        assertEquals("2026-09-12 14:00", split.attributes.dueText)
    }

    @Test
    fun bracesWithUnknownKeysAreLeftAsPlainText() {
        val split = TaskAttributesFormat.extractFrom("check the config {debug: true}")

        assertEquals("check the config {debug: true}", split.text)
        assertTrue(split.attributes.isEmpty)
    }

    @Test
    fun ordinaryBracesAreLeftAlone() {
        val split = TaskAttributesFormat.extractFrom("rename {old} to {new}")

        assertEquals("rename {old} to {new}", split.text)
        assertTrue(split.attributes.isEmpty)
    }

    @Test
    fun aValueHoldingASemicolonSurvivesTheRoundTrip() {
        val attributes = TaskAttributes(categoryName = "Work; Urgent")
        val rendered = TaskAttributesFormat.render(attributes)!!

        val split = TaskAttributesFormat.extractFrom("a task $rendered")

        assertEquals("a task", split.text)
        assertEquals("Work; Urgent", split.attributes.categoryName)
    }

    @Test
    fun aLinkAndDescriptionAreWrittenLast() {
        val rendered = TaskAttributesFormat.render(
            TaskAttributes(
                dueText = "2026-09-12 14:00",
                link = "https://example.com/q3",
                details = "Bring the Q3 numbers"
            )
        )

        assertEquals(
            "{due: 2026-09-12 14:00; link: https://example.com/q3; details: Bring the Q3 numbers}",
            rendered
        )
    }

    @Test
    fun aDescriptionHoldingBracesSurvivesTheRoundTrip() {
        val rendered = TaskAttributesFormat.render(
            TaskAttributes(details = "rename {old} to {new}")
        )!!

        val split = TaskAttributesFormat.extractFrom("a task $rendered")

        assertEquals("a task", split.text)
        assertEquals("rename {old} to {new}", split.attributes.details)
    }

    @Test
    fun aDescriptionHoldingALineBreakSurvivesTheRoundTrip() {
        val rendered = TaskAttributesFormat.render(
            TaskAttributes(details = "first line\nsecond line")
        )!!

        assertTrue(!rendered.contains('\n'), "the group has to stay on one line: $rendered")

        val split = TaskAttributesFormat.extractFrom("a task $rendered")

        assertEquals("first line\nsecond line", split.attributes.details)
    }

    @Test
    fun aDescriptionHoldingABackslashNSurvivesAsLiteralText() {
        val rendered = TaskAttributesFormat.render(
            TaskAttributes(details = "path is C:\\notes")
        )!!

        val split = TaskAttributesFormat.extractFrom("a task $rendered")

        assertEquals("path is C:\\notes", split.attributes.details)
    }

    @Test
    fun everyKeyTogetherSurvivesTheRoundTrip() {
        val attributes = TaskAttributes(
            dueText = "2026-09-12 14:00",
            durationMinutes = 90,
            categoryName = "Work; Home",
            repeatText = "every 2 weeks on mon,thu until 2027-01-31",
            link = "https://example.com/a(b)",
            details = "see {this}; and that\nplus a second line"
        )

        val rendered = TaskAttributesFormat.render(attributes)!!
        val split = TaskAttributesFormat.extractFrom("a task $rendered")

        assertEquals("a task", split.text)
        assertEquals(attributes, split.attributes)
    }

    @Test
    fun aDueTimeKeepsItsMinuteWhenWrittenAndReadBack() {
        val original = TaskDueTimeFormat.parse("2026-09-12 14:35", utc)!!

        assertEquals("2026-09-12 14:35", TaskDueTimeFormat.render(original, utc))
        assertEquals(original, TaskDueTimeFormat.parse(TaskDueTimeFormat.render(original, utc), utc))
    }

    @Test
    fun secondsAreOnlyWrittenWhenTheyMatter() {
        val onTheMinute = TaskDueTimeFormat.parse("2026-09-12 14:00", utc)!!
        assertEquals("2026-09-12 14:00", TaskDueTimeFormat.render(onTheMinute, utc))
        assertEquals("2026-09-12 14:00:30", TaskDueTimeFormat.render(onTheMinute + 30_000L, utc))
    }

    @Test
    fun aDateWithNoTimeIsReadAsMidnight() {
        assertEquals(
            TaskDueTimeFormat.parse("2026-09-12 00:00", utc),
            TaskDueTimeFormat.parse("2026-09-12", utc)
        )
    }

    @Test
    fun nonsenseDueTextIsRejectedRatherThanGuessed() {
        assertNull(TaskDueTimeFormat.parse("next tuesday", utc))
        assertNull(TaskDueTimeFormat.parse("", utc))
    }

    @Test
    fun everyRepeatShapeSurvivesTheRoundTrip() {
        val rules = listOf(
            RecurrenceRule(RecurrenceFrequency.DAILY),
            RecurrenceRule(RecurrenceFrequency.DAILY, interval = 3),
            RecurrenceRule(RecurrenceFrequency.WEEKLY),
            RecurrenceRule(
                RecurrenceFrequency.WEEKLY,
                interval = 2,
                daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)
            ),
            RecurrenceRule(RecurrenceFrequency.MONTHLY, interval = 6),
            RecurrenceRule(RecurrenceFrequency.YEARLY, untilDateString = "2030-01-01"),
            RecurrenceRule(
                RecurrenceFrequency.WEEKLY,
                interval = 2,
                daysOfWeek = setOf(DayOfWeek.FRIDAY),
                untilDateString = "2027-06-30"
            )
        )

        for (rule in rules) {
            val text = TaskRepeatFormat.render(rule)
            assertEquals(rule, TaskRepeatFormat.parse(text), "drifted for: $text")
        }
    }

    @Test
    fun repeatTextReadsTheWayAPersonWouldWriteIt() {
        assertEquals("daily", TaskRepeatFormat.render(RecurrenceRule(RecurrenceFrequency.DAILY)))
        assertEquals(
            "every 3 days",
            TaskRepeatFormat.render(RecurrenceRule(RecurrenceFrequency.DAILY, interval = 3))
        )
        assertEquals(
            "weekly on mon,fri",
            TaskRepeatFormat.render(
                RecurrenceRule(
                    RecurrenceFrequency.WEEKLY,
                    daysOfWeek = setOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY)
                )
            )
        )
    }

    @Test
    fun unparseableRepeatTextIsRejected() {
        assertNull(TaskRepeatFormat.parse("whenever I feel like it"))
        assertNull(TaskRepeatFormat.parse(""))
    }
}
