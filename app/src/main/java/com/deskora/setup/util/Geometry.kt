package com.deskora.setup.util

import com.deskora.setup.model.NormalizedPoint
import com.deskora.setup.model.SetupItem

/**
 * Pure geometry helpers operating on normalized coordinates (0.0..1.0).
 *
 * All functions are total: they never throw, always clamp, and never produce
 * NaN or Infinity. They are safe to call directly from Compose drawing code.
 */
object Geometry {

    const val MIN_SIZE = 0.02f
    const val MAX_SIZE = 1.0f

    /** Replaces NaN / Infinity with a fallback and returns a finite float. */
    fun finite(value: Float, fallback: Float = 0f): Float =
        if (value.isNaN() || value.isInfinite()) fallback else value

    /** Clamps a value into [min, max], guarding against NaN/Infinity. */
    fun clamp(value: Float, min: Float, max: Float): Float {
        val v = finite(value, min)
        val lo = minOf(min, max)
        val hi = maxOf(min, max)
        return when {
            v < lo -> lo
            v > hi -> hi
            else -> v
        }
    }

    /** Clamps a normalized position coordinate to [0, 1]. */
    fun clampCoord(value: Float): Float = clamp(value, 0f, 1f)

    /** Clamps a normalized size to a sane visible range. */
    fun clampSize(value: Float): Float = clamp(value, MIN_SIZE, MAX_SIZE)

    /**
     * Clamps an item's rectangle so that it stays fully inside the desk bounds
     * while remaining at least MIN_SIZE in each dimension and always visible.
     */
    fun clampItemBounds(
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ): FloatArray {
        val w = clampSize(width).coerceAtMost(1f)
        val h = clampSize(height).coerceAtMost(1f)
        val cx = clamp(x, 0f, 1f - w)
        val cy = clamp(y, 0f, 1f - h)
        return floatArrayOf(cx, cy, w, h)
    }

    fun moveItem(item: SetupItem, dx: Float, dy: Float): SetupItem {
        val b = clampItemBounds(item.normalizedX + dx, item.normalizedY + dy, item.normalizedWidth, item.normalizedHeight)
        return item.copy(normalizedX = b[0], normalizedY = b[1])
    }

    fun resizeItem(item: SetupItem, dWidth: Float, dHeight: Float): SetupItem {
        val b = clampItemBounds(
            item.normalizedX,
            item.normalizedY,
            item.normalizedWidth + dWidth,
            item.normalizedHeight + dHeight
        )
        return item.copy(
            normalizedX = b[0],
            normalizedY = b[1],
            normalizedWidth = b[2],
            normalizedHeight = b[3]
        )
    }

    /** Rotates in 90-degree increments, normalized to {0, 90, 180, 270}. */
    fun rotateItem(item: SetupItem, deltaDegrees: Int): SetupItem =
        item.copy(rotationDegrees = normalizeRotation(item.rotationDegrees + deltaDegrees))

    fun normalizeRotation(degrees: Int): Int {
        var d = degrees % 360
        if (d < 0) d += 360
        return when {
            d < 45 -> 0
            d < 135 -> 90
            d < 225 -> 180
            d < 315 -> 270
            else -> 0
        }
    }

    /** Center point of an item's rectangle in normalized coordinates. */
    fun itemCenter(item: SetupItem): NormalizedPoint {
        val cx = clampCoord(item.normalizedX + item.normalizedWidth / 2f)
        val cy = clampCoord(item.normalizedY + item.normalizedHeight / 2f)
        return NormalizedPoint(cx, cy)
    }

    /** Anchor for a cable end at the given item's center. */
    fun cableAnchor(item: SetupItem?): NormalizedPoint? =
        item?.let { itemCenter(it) }

    /** True when the item's rectangle lies fully within the desk. */
    fun isWithinDesk(x: Float, y: Float, width: Float, height: Float): Boolean {
        if (listOf(x, y, width, height).any { it.isNaN() || it.isInfinite() }) return false
        return x >= 0f && y >= 0f && width > 0f && height > 0f && x + width <= 1.0001f && y + height <= 1.0001f
    }

    /** True when the child rectangle lies (mostly) within the parent zone. */
    fun isWithinZone(
        itemX: Float, itemY: Float, itemW: Float, itemH: Float,
        zoneX: Float, zoneY: Float, zoneW: Float, zoneH: Float
    ): Boolean {
        val icx = itemX + itemW / 2f
        val icy = itemY + itemH / 2f
        return icx in zoneX..(zoneX + zoneW) && icy in zoneY..(zoneY + zoneH)
    }

    fun clampZoneBounds(x: Float, y: Float, width: Float, height: Float): FloatArray {
        val w = clamp(width, MIN_SIZE, 1f)
        val h = clamp(height, MIN_SIZE, 1f)
        val cx = clamp(x, 0f, 1f - w)
        val cy = clamp(y, 0f, 1f - h)
        return floatArrayOf(cx, cy, w, h)
    }

    /** Normalized (0..1) value -> pixel, guarding against a zero-sized canvas. */
    fun normToPx(value: Float, sizePx: Float): Float {
        if (sizePx <= 0f || sizePx.isNaN() || sizePx.isInfinite()) return 0f
        return clampCoord(value) * sizePx
    }

    /** Pixel -> normalized (0..1) value, guarding against a zero-sized canvas. */
    fun pxToNorm(px: Float, sizePx: Float): Float {
        if (sizePx <= 0f || sizePx.isNaN() || sizePx.isInfinite()) return 0f
        return clampCoord(px / sizePx)
    }

    /**
     * Safe desk aspect ratio (width / depth). Falls back to a default when
     * dimensions are missing, zero, or invalid.
     */
    fun deskAspectRatio(widthCm: Double?, depthCm: Double?, default: Float = 1.6f): Float {
        val w = widthCm ?: return default
        val d = depthCm ?: return default
        if (w <= 0.0 || d <= 0.0 || w.isNaN() || d.isNaN()) return default
        val ratio = (w / d).toFloat()
        return clamp(ratio, 0.75f, 3.0f)
    }

    /** Deterministic render order for items: archived first (behind), then by id. */
    fun renderOrder(items: List<SetupItem>): List<SetupItem> =
        items.sortedWith(compareBy({ it.status.name }, { it.id }))
}
