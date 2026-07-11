package com.deskora.setup

import com.deskora.setup.model.CleaningFrequencyType
import com.deskora.setup.model.CleaningStatus
import com.deskora.setup.model.CleaningTask
import com.deskora.setup.model.WeekDay
import com.deskora.setup.util.ChecklistDates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ChecklistDatesTest {

    private val base = LocalDate.of(2026, 1, 15) // a Thursday

    private fun task(freq: CleaningFrequencyType, interval: Int? = null, days: List<WeekDay> = emptyList(), due: String = "", enabled: Boolean = true) =
        CleaningTask(id = "t", setupId = "s", title = "Clean", frequencyType = freq, intervalValue = interval, selectedDays = days, nextDueDate = due, enabled = enabled)

    @Test
    fun parseOrNull_returnsNullOnBadInput() {
        assertNull(ChecklistDates.parseOrNull(""))
        assertNull(ChecklistDates.parseOrNull("not-a-date"))
        assertNull(ChecklistDates.parseOrNull("2026-13-40"))
        assertTrue(ChecklistDates.parseOrNull("2026-01-15") != null)
    }

    @Test
    fun daily_nextDueIsPlusOneDay() {
        val next = ChecklistDates.computeNextDueDate(task(CleaningFrequencyType.Daily), base)
        assertEquals("2026-01-16", next)
    }

    @Test
    fun weekly_nextDueIsPlusOneWeek() {
        val next = ChecklistDates.computeNextDueDate(task(CleaningFrequencyType.Weekly), base)
        assertEquals("2026-01-22", next)
    }

    @Test
    fun monthly_usesCalendarMonth() {
        val next = ChecklistDates.computeNextDueDate(task(CleaningFrequencyType.Monthly), LocalDate.of(2026, 1, 31))
        // Jan 31 + 1 month -> Feb 28 (2026 is not a leap year)
        assertEquals("2026-02-28", next)
    }

    @Test
    fun everyNumberOfDays_addsInterval() {
        val next = ChecklistDates.computeNextDueDate(task(CleaningFrequencyType.EveryNumberOfDays, interval = 10), base)
        assertEquals("2026-01-25", next)
    }

    @Test
    fun selectedDays_findsNextMatchingWeekday() {
        // From Thu Jan 15, next Monday is Jan 19.
        val next = ChecklistDates.computeNextDueDate(
            task(CleaningFrequencyType.SelectedDays, days = listOf(WeekDay.Monday)), base
        )
        assertEquals("2026-01-19", next)
    }

    @Test
    fun manual_hasNoAutoDate() {
        assertEquals("", ChecklistDates.computeNextDueDate(task(CleaningFrequencyType.Manual), base))
    }

    @Test
    fun status_dueWhenDatePassed() {
        val t = task(CleaningFrequencyType.Daily, due = "2026-01-10")
        assertEquals(CleaningStatus.Due, ChecklistDates.statusOf(t, base))
        assertTrue(ChecklistDates.isDue(t, base))
    }

    @Test
    fun status_disabledOverridesEverything() {
        val t = task(CleaningFrequencyType.Daily, due = "2026-01-10", enabled = false)
        assertEquals(CleaningStatus.Disabled, ChecklistDates.statusOf(t, base))
    }

    @Test
    fun status_manualWhenNeverCompleted() {
        val t = task(CleaningFrequencyType.Manual)
        assertEquals(CleaningStatus.Manual, ChecklistDates.statusOf(t, base))
    }

    @Test
    fun status_invalidDateFallsBackGracefully() {
        val t = task(CleaningFrequencyType.Daily, due = "garbage")
        // Should not throw; returns a non-crashing status.
        val s = ChecklistDates.statusOf(t, base)
        assertTrue(s == CleaningStatus.Upcoming || s == CleaningStatus.Due)
    }
}
