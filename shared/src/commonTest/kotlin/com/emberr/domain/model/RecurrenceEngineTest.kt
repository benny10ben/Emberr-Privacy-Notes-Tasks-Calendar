package com.emberr.domain.model

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecurrenceEngineTest {

    private fun dailyRule(interval: Int = 1, untilDateString: String? = null) =
        RecurrenceRule(RecurrenceFrequency.DAILY, interval, emptySet(), untilDateString)

    private fun weeklyRule(interval: Int = 1, daysOfWeek: Set<DayOfWeek> = emptySet()) =
        RecurrenceRule(RecurrenceFrequency.WEEKLY, interval, daysOfWeek)

    private fun monthlyRule(interval: Int = 1) =
        RecurrenceRule(RecurrenceFrequency.MONTHLY, interval)

    private fun yearlyRule(interval: Int = 1) =
        RecurrenceRule(RecurrenceFrequency.YEARLY, interval)

    @Test
    fun aDateBeforeTheAnchorNeverCounts() {
        val anchor = LocalDate(2026, 1, 10)

        assertFalse(RecurrenceEngine.occursOn(dailyRule(), anchor, LocalDate(2026, 1, 9)))
    }

    @Test
    fun theAnchorItselfAlwaysCounts() {
        val anchor = LocalDate(2026, 1, 10)

        assertTrue(RecurrenceEngine.occursOn(dailyRule(interval = 7), anchor, anchor))
    }

    @Test
    fun dailyRuleWithIntervalOfThreeOnlyCountsEveryThirdDay() {
        val anchor = LocalDate(2026, 1, 1)
        val rule = dailyRule(interval = 3)

        assertTrue(RecurrenceEngine.occursOn(rule, anchor, LocalDate(2026, 1, 4)))
        assertFalse(RecurrenceEngine.occursOn(rule, anchor, LocalDate(2026, 1, 5)))
        assertFalse(RecurrenceEngine.occursOn(rule, anchor, LocalDate(2026, 1, 6)))
        assertTrue(RecurrenceEngine.occursOn(rule, anchor, LocalDate(2026, 1, 7)))
    }

    @Test
    fun theUntilDateIsInclusiveAndAnythingAfterItStops() {
        val anchor = LocalDate(2026, 1, 1)
        val rule = dailyRule(untilDateString = "2026-01-05")

        assertTrue(RecurrenceEngine.occursOn(rule, anchor, LocalDate(2026, 1, 5)))
        assertFalse(RecurrenceEngine.occursOn(rule, anchor, LocalDate(2026, 1, 6)))
    }

    @Test
    fun weeklyRuleRepeatsOnEveryChosenDayOfTheWeek() {
        val mondayAnchor = LocalDate(2026, 1, 5)
        val rule = weeklyRule(daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))

        assertTrue(RecurrenceEngine.occursOn(rule, mondayAnchor, LocalDate(2026, 1, 7)))
        assertTrue(RecurrenceEngine.occursOn(rule, mondayAnchor, LocalDate(2026, 1, 12)))
        assertFalse(RecurrenceEngine.occursOn(rule, mondayAnchor, LocalDate(2026, 1, 6)))
    }

    @Test
    fun weeklyRuleWithIntervalOfTwoSkipsTheWeekInBetween() {
        val mondayAnchor = LocalDate(2026, 1, 5)
        val rule = weeklyRule(interval = 2, daysOfWeek = setOf(DayOfWeek.MONDAY))

        assertTrue(RecurrenceEngine.occursOn(rule, mondayAnchor, LocalDate(2026, 1, 5)))
        assertFalse(RecurrenceEngine.occursOn(rule, mondayAnchor, LocalDate(2026, 1, 12)))
        assertTrue(RecurrenceEngine.occursOn(rule, mondayAnchor, LocalDate(2026, 1, 19)))
    }

    @Test
    fun weeklyRuleWithNoChosenDaysFallsBackToTheAnchorsOwnDay() {
        val mondayAnchor = LocalDate(2026, 1, 5)
        val rule = weeklyRule()

        assertTrue(RecurrenceEngine.occursOn(rule, mondayAnchor, LocalDate(2026, 1, 12)))
        assertFalse(RecurrenceEngine.occursOn(rule, mondayAnchor, LocalDate(2026, 1, 6)))
    }

    @Test
    fun weeklyRuleCountsWeeksFromMondaySoASundayAnchorStillRepeatsWeekly() {
        val sundayAnchor = LocalDate(2026, 1, 4)
        val rule = weeklyRule()

        assertTrue(RecurrenceEngine.occursOn(rule, sundayAnchor, LocalDate(2026, 1, 11)))
        assertTrue(RecurrenceEngine.occursOn(rule, sundayAnchor, LocalDate(2026, 1, 18)))
    }

    @Test
    fun monthlyRuleAnchoredOnTheThirtyFirstFallsBackToTheLastDayOfAShorterMonth() {
        val anchor = LocalDate(2026, 1, 31)
        val rule = monthlyRule()

        assertTrue(RecurrenceEngine.occursOn(rule, anchor, LocalDate(2026, 2, 28)))
        assertFalse(RecurrenceEngine.occursOn(rule, anchor, LocalDate(2026, 2, 27)))
        assertTrue(RecurrenceEngine.occursOn(rule, anchor, LocalDate(2026, 3, 31)))
        assertFalse(RecurrenceEngine.occursOn(rule, anchor, LocalDate(2026, 3, 28)))
    }

    @Test
    fun monthlyRuleAnchoredOnTheThirtyFirstLandsOnFebruaryTwentyNinthInALeapYear() {
        val anchor = LocalDate(2024, 1, 31)
        val rule = monthlyRule()

        assertTrue(RecurrenceEngine.occursOn(rule, anchor, LocalDate(2024, 2, 29)))
        assertFalse(RecurrenceEngine.occursOn(rule, anchor, LocalDate(2024, 2, 28)))
    }

    @Test
    fun monthlyRuleWithIntervalOfTwoSkipsTheMonthInBetween() {
        val anchor = LocalDate(2026, 1, 15)
        val rule = monthlyRule(interval = 2)

        assertFalse(RecurrenceEngine.occursOn(rule, anchor, LocalDate(2026, 2, 15)))
        assertTrue(RecurrenceEngine.occursOn(rule, anchor, LocalDate(2026, 3, 15)))
    }

    @Test
    fun monthlyRuleCountsMonthsAcrossAYearBoundary() {
        val anchor = LocalDate(2025, 11, 15)
        val rule = monthlyRule(interval = 3)

        assertTrue(RecurrenceEngine.occursOn(rule, anchor, LocalDate(2026, 2, 15)))
        assertFalse(RecurrenceEngine.occursOn(rule, anchor, LocalDate(2026, 1, 15)))
    }

    @Test
    fun yearlyRuleOnlyCountsTheSameMonthAndDay() {
        val anchor = LocalDate(2026, 3, 10)
        val rule = yearlyRule()

        assertTrue(RecurrenceEngine.occursOn(rule, anchor, LocalDate(2027, 3, 10)))
        assertFalse(RecurrenceEngine.occursOn(rule, anchor, LocalDate(2027, 4, 10)))
        assertFalse(RecurrenceEngine.occursOn(rule, anchor, LocalDate(2027, 3, 11)))
    }

    @Test
    fun yearlyRuleAnchoredOnALeapDayFallsBackToFebruaryTwentyEighth() {
        val anchor = LocalDate(2024, 2, 29)
        val rule = yearlyRule()

        assertTrue(RecurrenceEngine.occursOn(rule, anchor, LocalDate(2025, 2, 28)))
        assertTrue(RecurrenceEngine.occursOn(rule, anchor, LocalDate(2028, 2, 29)))
        assertFalse(RecurrenceEngine.occursOn(rule, anchor, LocalDate(2028, 2, 28)))
    }

    @Test
    fun occurrenceDatesInRangeListsEveryMatchingDayInOrder() {
        val anchor = LocalDate(2026, 1, 1)

        val dates = RecurrenceEngine.occurrenceDatesInRange(
            rule = dailyRule(interval = 3),
            anchor = anchor,
            rangeStart = LocalDate(2026, 1, 1),
            rangeEnd = LocalDate(2026, 1, 10)
        )

        assertEquals(
            listOf(
                LocalDate(2026, 1, 1),
                LocalDate(2026, 1, 4),
                LocalDate(2026, 1, 7),
                LocalDate(2026, 1, 10)
            ),
            dates
        )
    }

    @Test
    fun occurrenceDatesInRangeNeverReturnsDatesBeforeTheAnchor() {
        val anchor = LocalDate(2026, 1, 5)

        val dates = RecurrenceEngine.occurrenceDatesInRange(
            rule = dailyRule(),
            anchor = anchor,
            rangeStart = LocalDate(2026, 1, 1),
            rangeEnd = LocalDate(2026, 1, 6)
        )

        assertEquals(listOf(LocalDate(2026, 1, 5), LocalDate(2026, 1, 6)), dates)
    }

    @Test
    fun occurrenceDatesInRangeIsEmptyWhenTheWholeRangeEndsBeforeTheAnchor() {
        val dates = RecurrenceEngine.occurrenceDatesInRange(
            rule = dailyRule(),
            anchor = LocalDate(2026, 2, 1),
            rangeStart = LocalDate(2026, 1, 1),
            rangeEnd = LocalDate(2026, 1, 10)
        )

        assertTrue(dates.isEmpty())
    }

    @Test
    fun nextOccurrenceSkipsPastTheGivenDay() {
        val anchor = LocalDate(2026, 1, 1)

        assertEquals(
            LocalDate(2026, 1, 7),
            RecurrenceEngine.nextOccurrence(dailyRule(interval = 3), anchor, LocalDate(2026, 1, 4))
        )
    }

    @Test
    fun nextOccurrenceStopsAtTheUntilDate() {
        val anchor = LocalDate(2026, 1, 1)
        val rule = dailyRule(untilDateString = "2026-01-05")

        assertNull(RecurrenceEngine.nextOccurrence(rule, anchor, LocalDate(2026, 1, 5)))
    }

    @Test
    fun nextOccurrenceGivesUpOnceItHasScannedTenYearsAhead() {
        val anchor = LocalDate(2026, 1, 1)

        assertNull(RecurrenceEngine.nextOccurrence(yearlyRule(interval = 100), anchor, anchor))
    }

    @Test
    fun nextOccurrenceStillFindsAMatchJustInsideTheTenYearScan() {
        val anchor = LocalDate(2026, 1, 1)

        assertEquals(
            LocalDate(2031, 1, 1),
            RecurrenceEngine.nextOccurrence(yearlyRule(interval = 5), anchor, LocalDate(2026, 6, 1))
        )
    }

    @Test
    fun previousOccurrenceLooksBackwardsWithoutIncludingTheGivenDay() {
        val anchor = LocalDate(2026, 1, 1)

        assertEquals(
            LocalDate(2026, 1, 5),
            RecurrenceEngine.previousOccurrence(dailyRule(interval = 2), anchor, LocalDate(2026, 1, 6))
        )
    }

    @Test
    fun previousOccurrenceIsNullWhenNothingSitsBetweenTheAnchorAndTheGivenDay() {
        val anchor = LocalDate(2026, 1, 1)

        assertNull(RecurrenceEngine.previousOccurrence(dailyRule(), anchor, anchor))
    }

    @Test
    fun retargetingATimestampKeepsTheTimeOfDayAndReplacesTheDate() {
        val original = LocalDateTime(2026, 3, 10, 14, 30, 15)
            .toInstant(TimeZone.UTC)
            .toEpochMilliseconds()

        val retargeted = RecurrenceEngine.retargetTimestampToDate(original, LocalDate(2026, 7, 4))

        val retargetedDateTime = kotlin.time.Instant
            .fromEpochMilliseconds(retargeted)
            .toLocalDateTime(TimeZone.UTC)

        assertEquals(LocalDateTime(2026, 7, 4, 14, 30, 15), retargetedDateTime)
    }

    @Test
    fun retargetingATimestampDropsAnyMillisecondsFromTheOriginal() {
        val originalWithMilliseconds = LocalDateTime(2026, 3, 10, 14, 30, 15)
            .toInstant(TimeZone.UTC)
            .toEpochMilliseconds() + 123

        val retargeted = RecurrenceEngine.retargetTimestampToDate(
            originalWithMilliseconds,
            LocalDate(2026, 3, 10)
        )

        assertEquals(originalWithMilliseconds - 123, retargeted)
    }

    @Test
    fun retargetingKeepsTheWallClockTimeAcrossADaylightSavingChange() {
        val newYork = TimeZone.of("America/New_York")
        val winterTimestamp = LocalDateTime(2026, 1, 15, 14, 30, 0)
            .toInstant(newYork)
            .toEpochMilliseconds()

        val summerTimestamp = RecurrenceEngine.retargetTimestampToDate(
            originalTimestamp = winterTimestamp,
            newDate = LocalDate(2026, 7, 15),
            timeZone = newYork
        )

        val summerDateTime = kotlin.time.Instant
            .fromEpochMilliseconds(summerTimestamp)
            .toLocalDateTime(newYork)

        assertEquals(LocalDateTime(2026, 7, 15, 14, 30, 0), summerDateTime)
    }

    @Test
    fun crossingIntoDaylightSavingMovesTheRealMomentAnHourEarlier() {
        val newYork = TimeZone.of("America/New_York")
        val winterDate = LocalDate(2026, 1, 15)
        val summerDate = LocalDate(2026, 7, 15)
        val winterTimestamp = LocalDateTime(2026, 1, 15, 14, 30, 0)
            .toInstant(newYork)
            .toEpochMilliseconds()

        val summerTimestamp = RecurrenceEngine.retargetTimestampToDate(
            winterTimestamp,
            summerDate,
            newYork
        )

        val millisecondsPerDay = 24L * 60 * 60 * 1000
        val millisecondsPerHour = 60L * 60 * 1000
        val ignoringDaylightSaving = winterTimestamp + winterDate.daysUntil(summerDate) * millisecondsPerDay

        assertEquals(ignoringDaylightSaving - millisecondsPerHour, summerTimestamp)
    }

    @Test
    fun theCallerDecidesWhichTimeZoneTheReminderBelongsTo() {
        val kolkata = TimeZone.of("Asia/Kolkata")
        val lateEvening = LocalDateTime(2026, 3, 10, 23, 0, 0)
            .toInstant(TimeZone.UTC)
            .toEpochMilliseconds()
        val newDate = LocalDate(2026, 3, 15)

        val retargetedInUtc = RecurrenceEngine.retargetTimestampToDate(lateEvening, newDate, TimeZone.UTC)
        val retargetedInKolkata = RecurrenceEngine.retargetTimestampToDate(lateEvening, newDate, kolkata)

        assertNotEquals(retargetedInUtc, retargetedInKolkata)
        assertEquals(
            LocalDateTime(2026, 3, 15, 23, 0, 0),
            kotlin.time.Instant.fromEpochMilliseconds(retargetedInUtc).toLocalDateTime(TimeZone.UTC)
        )
        assertEquals(
            LocalDateTime(2026, 3, 15, 4, 30, 0),
            kotlin.time.Instant.fromEpochMilliseconds(retargetedInKolkata).toLocalDateTime(kolkata)
        )
    }

    @Test
    fun isoDayNumbersConvertToDaysOfWeekAndRejectAnythingOutOfRange() {
        assertEquals(DayOfWeek.MONDAY, isoDayNumberToDayOfWeek(1))
        assertEquals(DayOfWeek.SUNDAY, isoDayNumberToDayOfWeek(7))
        assertNull(isoDayNumberToDayOfWeek(0))
        assertNull(isoDayNumberToDayOfWeek(8))
        assertNull(isoDayNumberToDayOfWeek(null))
    }

    @Test
    fun daysOfWeekSurviveARoundTripThroughTheStoredCsvFormat() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SUNDAY)

        val csv = days.toIsoDayNumberCsv()
        val restored = csv.split(",").mapNotNull { isoDayNumberToDayOfWeek(it.toIntOrNull()) }.toSet()

        assertEquals("1,3,7", csv)
        assertEquals(days, restored)
    }
}
