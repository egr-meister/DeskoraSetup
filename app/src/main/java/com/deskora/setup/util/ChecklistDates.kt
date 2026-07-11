package com.deskora.setup.util

import com.deskora.setup.model.CleaningFrequencyType
import com.deskora.setup.model.CleaningStatus
import com.deskora.setup.model.CleaningTask
import com.deskora.setup.model.WeekDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Calendar-aware checklist recurrence utilities.
 *
 * Uses java.time (LocalDate) rather than millisecond arithmetic so that day and
 * month boundaries, and daylight changes, are handled correctly. No background
 * scheduling is used anywhere; due state is computed on demand.
 */
object ChecklistDates {

    private val FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun today(): LocalDate = LocalDate.now()

    fun format(date: LocalDate): String = date.format(FORMAT)

    /** Parses a YYYY-MM-DD string, returning null on any failure. */
    fun parseOrNull(text: String?): LocalDate? {
        if (text.isNullOrBlank()) return null
        return try {
            LocalDate.parse(text, FORMAT)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Computes the next due date after completing a task on [completedOn].
     * Returns an ISO date string. For Manual frequency, returns empty (no auto date).
     */
    fun computeNextDueDate(task: CleaningTask, completedOn: LocalDate): String {
        return when (task.frequencyType) {
            CleaningFrequencyType.Manual -> ""
            CleaningFrequencyType.Daily -> format(completedOn.plusDays(1))
            CleaningFrequencyType.Weekly -> format(completedOn.plusWeeks(1))
            CleaningFrequencyType.Monthly -> format(completedOn.plusMonths(1))
            CleaningFrequencyType.EveryNumberOfDays -> {
                val n = (task.intervalValue ?: 1).coerceIn(1, 3650)
                format(completedOn.plusDays(n.toLong()))
            }
            CleaningFrequencyType.SelectedDays -> {
                val next = nextSelectedDay(completedOn.plusDays(1), task.selectedDays)
                if (next != null) format(next) else format(completedOn.plusDays(1))
            }
        }
    }

    /**
     * Computes the initial due date for a newly created/edited task, based on
     * [from] (usually today).
     */
    fun computeInitialDueDate(
        frequencyType: CleaningFrequencyType,
        intervalValue: Int?,
        selectedDays: List<WeekDay>,
        from: LocalDate
    ): String {
        return when (frequencyType) {
            CleaningFrequencyType.Manual -> ""
            CleaningFrequencyType.Daily -> format(from)
            CleaningFrequencyType.Weekly -> format(from)
            CleaningFrequencyType.Monthly -> format(from)
            CleaningFrequencyType.EveryNumberOfDays -> {
                val n = (intervalValue ?: 1).coerceIn(1, 3650)
                format(from.plusDays(n.toLong()))
            }
            CleaningFrequencyType.SelectedDays -> {
                val next = nextSelectedDay(from, selectedDays)
                if (next != null) format(next) else format(from)
            }
        }
    }

    /** Finds the first date on/after [start] whose weekday is in [days]. */
    private fun nextSelectedDay(start: LocalDate, days: List<WeekDay>): LocalDate? {
        if (days.isEmpty()) return null
        val wanted = days.map { it.isoValue }.toSet()
        var cursor = start
        var guard = 0
        while (guard < 14) {
            if (cursor.dayOfWeek.toIso() in wanted) return cursor
            cursor = cursor.plusDays(1)
            guard++
        }
        return null
    }

    /**
     * Computes the display status of a task relative to [reference] (today).
     * Never returns alarming wording.
     */
    fun statusOf(task: CleaningTask, reference: LocalDate): CleaningStatus {
        if (!task.enabled) return CleaningStatus.Disabled
        if (task.frequencyType == CleaningFrequencyType.Manual) {
            return if (task.lastCompletedDate.isNotBlank()) CleaningStatus.Complete else CleaningStatus.Manual
        }
        val due = parseOrNull(task.nextDueDate) ?: return CleaningStatus.Upcoming
        return when {
            !due.isAfter(reference) -> CleaningStatus.Due
            due.isEqual(reference.plusDays(1)) || due.isBefore(reference.plusDays(3)) -> CleaningStatus.Upcoming
            else -> CleaningStatus.Upcoming
        }
    }

    fun isDue(task: CleaningTask, reference: LocalDate): Boolean =
        statusOf(task, reference) == CleaningStatus.Due

    private fun DayOfWeek.toIso(): Int = this.value // Monday=1..Sunday=7
}
