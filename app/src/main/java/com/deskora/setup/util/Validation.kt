package com.deskora.setup.util

/** Field validation and safe numeric parsing helpers. */
object Validation {

    const val SETUP_NOTE_MAX = 2000
    const val ITEM_NOTE_MAX = 1000
    const val CABLE_NOTE_MAX = 500
    const val CHECKLIST_NOTE_MAX = 500
    const val NEEDED_NOTE_MAX = 500
    const val NAME_MAX = 80

    fun isBlank(text: String?): Boolean = text == null || text.trim().isEmpty()

    fun trimTo(text: String, max: Int): String {
        val t = text
        return if (t.length <= max) t else t.substring(0, max)
    }

    /** Parses an optional positive decimal (centimeters). Returns null if empty/invalid. */
    fun parsePositiveDoubleOrNull(text: String): Double? {
        val cleaned = text.trim().replace(',', '.')
        if (cleaned.isEmpty()) return null
        val value = cleaned.toDoubleOrNull() ?: return null
        if (value.isNaN() || value.isInfinite() || value <= 0.0) return null
        return value.coerceIn(1.0, 10000.0)
    }

    /** Parses a positive integer interval, clamped to a sane maximum. */
    fun parsePositiveIntOrNull(text: String, max: Int = 3650): Int? {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return null
        val value = cleaned.toIntOrNull() ?: return null
        if (value <= 0) return null
        return value.coerceAtMost(max)
    }
}
