package com.deskora.setup.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.KSerializer

/**
 * Central JSON configuration and resilient decoding helpers.
 *
 * The configuration is lenient and ignores unknown keys so that older or
 * newer stored data never crashes deserialization. [decodeListSafely] recovers
 * as many valid elements as possible instead of discarding the whole list when
 * a single element is malformed.
 */
object AppJson {
    val instance: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        allowStructuredMapKeys = true
        coerceInputValues = true // fills invalid enum/null values with defaults
    }

    fun <T> encodeList(serializer: KSerializer<List<T>>, value: List<T>): String =
        try {
            instance.encodeToString(serializer, value)
        } catch (e: Exception) {
            "[]"
        }

    /**
     * Decodes a JSON array string into a list, recovering per-element where the
     * whole-list decode fails. Malformed elements are skipped, valid ones kept.
     */
    fun <T> decodeListSafely(
        json: String?,
        elementSerializer: KSerializer<T>
    ): List<T> {
        if (json.isNullOrBlank()) return emptyList()
        // Fast path: decode the whole list at once.
        try {
            val listSerializer = kotlinx.serialization.builtins.ListSerializer(elementSerializer)
            return instance.decodeFromString(listSerializer, json)
        } catch (_: Exception) {
            // Fall through to element-by-element recovery.
        }
        return try {
            val array = instance.parseToJsonElement(json) as? JsonArray ?: return emptyList()
            array.mapNotNull { element ->
                try {
                    instance.decodeFromJsonElement(elementSerializer, element)
                } catch (_: Exception) {
                    null
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun <T> decodeObjectSafely(
        json: String?,
        serializer: KSerializer<T>,
        default: T
    ): T {
        if (json.isNullOrBlank()) return default
        return try {
            instance.decodeFromString(serializer, json)
        } catch (_: Exception) {
            default
        }
    }
}
