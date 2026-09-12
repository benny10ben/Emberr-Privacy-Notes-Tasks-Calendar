package com.emberr.domain.util.task

import com.emberr.domain.model.ParsedTask
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class HeuristicTaskExtractorTest {

    private class FixedClock(private val fixedInstant: Instant) : Clock {
        override fun now(): Instant = fixedInstant
    }

    private val timeZone = TimeZone.UTC
    private val mondayMorning = LocalDateTime(2026, 1, 5, 8, 0, 0).toInstant(timeZone)

    private val extractor = HeuristicTaskExtractor(
        clock = FixedClock(mondayMorning),
        currentTimeZone = { timeZone }
    )

    private fun singleTaskFor(transcript: String): ParsedTask =
        extractor.extractTasks(transcript).single()

    private fun textFor(transcript: String): String = singleTaskFor(transcript).taskText

    private fun reminderFor(transcript: String): LocalDateTime =
        Instant.fromEpochMilliseconds(singleTaskFor(transcript).timestamp!!).toLocalDateTime(timeZone)

    @Test
    fun nothingIsExtractedFromAnEmptyTranscript() {
        assertTrue(extractor.extractTasks("").isEmpty())
        assertTrue(extractor.extractTasks("   ").isEmpty())
        assertTrue(extractor.extractTasks("\n\t").isEmpty())
    }

    @Test
    fun theCommonWaysOfAskingForAReminderAreAllStrippedAway() {
        assertEquals("Buy milk", textFor("remind me to buy milk"))
        assertEquals("Buy milk", textFor("please remind me to buy milk"))
        assertEquals("Buy milk", textFor("could you please remind me to buy milk"))
        assertEquals("Buy milk", textFor("set a reminder to buy milk"))
        assertEquals("Buy milk", textFor("add a reminder to buy milk"))
        assertEquals("Call mum", textFor("don't forget to call mum"))
        assertEquals("Submit the form", textFor("i need to submit the form"))
        assertEquals("Water the plants", textFor("note to self: water the plants"))
        assertEquals("Pay rent", textFor("todo: pay rent"))
    }

    @Test
    fun aWakeWordGreetingIsStrippedAway() {
        assertEquals("Buy milk", textFor("hey Emberr, remind me to buy milk"))
        assertEquals("Buy milk", textFor("ok Emberr remind me to buy milk"))
    }

    @Test
    fun politeEndingsAreStrippedAway() {
        assertEquals("Buy milk", textFor("remind me to buy milk please"))
        assertEquals("Buy milk", textFor("remind me to buy milk thanks a lot"))
        assertEquals("Buy milk", textFor("remind me to buy milk thank you"))
    }

    @Test
    fun theTaskAlwaysStartsWithACapitalLetter() {
        assertEquals("Buy milk", textFor("buy milk"))
        assertEquals("Buy milk", textFor("  buy milk  "))
    }

    @Test
    fun aTranscriptWithNoTimeInItHasNoReminderSet() {
        assertNull(singleTaskFor("buy milk").timestamp)
    }

    @Test
    fun oneSentenceCanCarryTwoSeparateReminders() {
        val tasks = extractor.extractTasks("remind me to buy milk and also remind me to call mum")

        assertEquals(listOf("Buy milk", "Call mum"), tasks.map { it.taskText })
    }

    @Test
    fun aSemicolonAlsoSeparatesTwoReminders() {
        val tasks = extractor.extractTasks("buy milk; call mum")

        assertEquals(listOf("Buy milk", "Call mum"), tasks.map { it.taskText })
    }

    @Test
    fun aBareWordLikeAndNeverSplitsATaskInHalf() {
        assertEquals("Buy bread and butter", textFor("remind me to buy bread and butter"))
    }

    @Test
    fun aTranscriptThatIsNothingButATimeStillBecomesAUsableReminder() {
        val task = singleTaskFor("in 20 minutes")

        assertEquals("Voice reminder", task.taskText)
        assertEquals(LocalDateTime(2026, 1, 5, 8, 20, 0), reminderFor("in 20 minutes"))
    }

    @Test
    fun aReminderInSoManyMinutesIsSetThatFarAhead() {
        val task = singleTaskFor("remind me to take the pills in 20 minutes")

        assertEquals("Take the pills", task.taskText)
        assertEquals(LocalDateTime(2026, 1, 5, 8, 20, 0), reminderFor("take the pills in 20 minutes"))
    }

    @Test
    fun spokenNumbersAndVagueAmountsAreBothUnderstood() {
        assertEquals(LocalDateTime(2026, 1, 5, 8, 30, 0), reminderFor("in half an hour"))
        assertEquals(LocalDateTime(2026, 1, 5, 9, 0, 0), reminderFor("in an hour"))
        assertEquals(LocalDateTime(2026, 1, 5, 10, 0, 0), reminderFor("in two hours"))
        assertEquals(LocalDateTime(2026, 1, 5, 10, 0, 0), reminderFor("in a couple of hours"))
        assertEquals(LocalDateTime(2026, 1, 5, 8, 45, 0), reminderFor("in 45 minutes"))
        assertEquals(LocalDateTime(2026, 1, 8, 8, 0, 0), reminderFor("in a few days"))
        assertEquals(LocalDateTime(2026, 1, 19, 8, 0, 0), reminderFor("in two weeks"))
    }

    @Test
    fun tomorrowMeansTomorrowAtTheDefaultTimeOfNineInTheMorning() {
        assertEquals(LocalDateTime(2026, 1, 6, 9, 0, 0), reminderFor("call the plumber tomorrow"))
        assertEquals("Call the plumber", textFor("remind me to call the plumber tomorrow"))
    }

    @Test
    fun theDayAfterTomorrowMeansTwoDaysAhead() {
        assertEquals(
            LocalDateTime(2026, 1, 7, 9, 0, 0),
            reminderFor("call the plumber the day after tomorrow")
        )
    }

    @Test
    fun nextWeekMeansSevenDaysAhead() {
        assertEquals(LocalDateTime(2026, 1, 12, 9, 0, 0), reminderFor("call the plumber next week"))
    }

    @Test
    fun partsOfTheDayMapToSensibleClockTimes() {
        assertEquals(LocalDateTime(2026, 1, 6, 9, 0, 0), reminderFor("buy milk tomorrow morning"))
        assertEquals(LocalDateTime(2026, 1, 6, 14, 0, 0), reminderFor("buy milk tomorrow afternoon"))
        assertEquals(LocalDateTime(2026, 1, 6, 18, 0, 0), reminderFor("buy milk tomorrow evening"))
        assertEquals(LocalDateTime(2026, 1, 6, 20, 0, 0), reminderFor("buy milk tomorrow night"))
    }

    @Test
    fun partsOfTodayStayOnToday() {
        assertEquals(LocalDateTime(2026, 1, 5, 14, 0, 0), reminderFor("buy milk this afternoon"))
        assertEquals(LocalDateTime(2026, 1, 5, 18, 0, 0), reminderFor("buy milk this evening"))
        assertEquals(LocalDateTime(2026, 1, 5, 20, 0, 0), reminderFor("buy milk tonight"))
    }

    @Test
    fun aTwelveHourClockTimeIsUnderstood() {
        assertEquals(LocalDateTime(2026, 1, 5, 15, 0, 0), reminderFor("buy milk at 3pm"))
        assertEquals(LocalDateTime(2026, 1, 5, 15, 30, 0), reminderFor("buy milk at 3:30pm"))
        assertEquals(LocalDateTime(2026, 1, 5, 12, 0, 0), reminderFor("buy milk at 12pm"))
    }

    @Test
    fun aTwentyFourHourClockTimeIsUnderstood() {
        assertEquals(LocalDateTime(2026, 1, 5, 14, 30, 0), reminderFor("buy milk at 14:30"))
        assertEquals(LocalDateTime(2026, 1, 5, 23, 15, 0), reminderFor("buy milk at 23:15"))
    }

    @Test
    fun noonIsUnderstood() {
        assertEquals(LocalDateTime(2026, 1, 5, 12, 0, 0), reminderFor("buy milk at noon"))
        assertEquals(LocalDateTime(2026, 1, 5, 12, 0, 0), reminderFor("buy milk at midday"))
    }

    @Test
    fun aTimeThatHasAlreadyPassedTodayRollsOverToTomorrow() {
        assertEquals(LocalDateTime(2026, 1, 6, 7, 0, 0), reminderFor("buy milk at 7am"))
        assertEquals(LocalDateTime(2026, 1, 6, 7, 5, 0), reminderFor("buy milk at 07:05"))
        assertEquals(LocalDateTime(2026, 1, 6, 0, 0, 0), reminderFor("buy milk at midnight"))
        assertEquals(LocalDateTime(2026, 1, 6, 0, 0, 0), reminderFor("buy milk at 12am"))
    }

    @Test
    fun spokenFractionsOfAnHourAreUnderstood() {
        assertEquals(LocalDateTime(2026, 1, 6, 3, 15, 0), reminderFor("buy milk at quarter past 3"))
        assertEquals(LocalDateTime(2026, 1, 6, 6, 30, 0), reminderFor("buy milk at half past 6"))
        assertEquals(LocalDateTime(2026, 1, 6, 4, 45, 0), reminderFor("buy milk at quarter to 5"))
    }

    @Test
    fun anExplicitClockTimeBeatsAVagueTimeOfDay() {
        assertEquals(LocalDateTime(2026, 1, 6, 15, 0, 0), reminderFor("buy milk tomorrow morning at 3pm"))
    }

    @Test
    fun aNamedWeekdayLandsOnThatDayInTheComingWeek() {
        assertEquals(LocalDateTime(2026, 1, 9, 9, 0, 0), reminderFor("buy milk on friday"))
        assertEquals(LocalDateTime(2026, 1, 6, 9, 0, 0), reminderFor("buy milk on tuesday"))
    }

    @Test
    fun aNamedWeekdayThatIsTodayMeansAWeekFromNow() {
        assertEquals(LocalDateTime(2026, 1, 12, 9, 0, 0), reminderFor("buy milk on monday"))
    }

    @Test
    fun nextWeekdaySkipsPastTheComingOne() {
        assertEquals(LocalDateTime(2026, 1, 16, 9, 0, 0), reminderFor("buy milk next friday"))
    }

    @Test
    fun theDefaultExtractorReadsTheRealClockAndTimeZone() {
        val defaultExtractor = HeuristicTaskExtractor()
        val before = Clock.System.now().toEpochMilliseconds()

        val task = defaultExtractor.extractTasks("remind me to take the pills in 20 minutes").single()

        val twentyMinutesInMillis = 20L * 60 * 1000
        assertEquals("Take the pills", task.taskText)
        assertTrue(
            task.timestamp!! in (before + twentyMinutesInMillis)..(before + twentyMinutesInMillis + 30_000L),
            "the default extractor did not use the current time"
        )
    }
}
