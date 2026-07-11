package com.deskora.setup.util

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

/** Local ID and timestamp helpers. No network time is used. */
object Ids {
    fun newId(prefix: String = ""): String {
        val raw = UUID.randomUUID().toString()
        return if (prefix.isBlank()) raw else "${prefix}_$raw"
    }

    fun nowIso(): String = try {
        DateTimeFormatter.ISO_INSTANT.format(Instant.now())
    } catch (e: Exception) {
        "1970-01-01T00:00:00Z"
    }
}
