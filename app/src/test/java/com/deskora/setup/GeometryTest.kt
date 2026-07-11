package com.deskora.setup

import com.deskora.setup.model.SetupItem
import com.deskora.setup.util.Geometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeometryTest {

    private fun item(x: Float, y: Float, w: Float, h: Float) =
        SetupItem(id = "i", setupId = "s", normalizedX = x, normalizedY = y, normalizedWidth = w, normalizedHeight = h)

    @Test
    fun clampCoord_keepsWithinUnitRange() {
        assertEquals(0f, Geometry.clampCoord(-0.5f))
        assertEquals(1f, Geometry.clampCoord(2f))
        assertEquals(0.4f, Geometry.clampCoord(0.4f))
    }

    @Test
    fun clamp_handlesNaNAndInfinity() {
        assertEquals(0f, Geometry.clampCoord(Float.NaN))
        // Non-finite falls back to the lower bound, guaranteeing a finite result.
        assertEquals(0f, Geometry.clampCoord(Float.POSITIVE_INFINITY))
        assertTrue(Geometry.finite(Float.NaN, 3f) == 3f)
        assertTrue(Geometry.finite(Float.NEGATIVE_INFINITY, 5f) == 5f)
    }

    @Test
    fun clampItemBounds_keepsItemInsideDesk() {
        val b = Geometry.clampItemBounds(0.95f, 0.95f, 0.5f, 0.5f)
        assertTrue(b[0] + b[2] <= 1.0001f)
        assertTrue(b[1] + b[3] <= 1.0001f)
    }

    @Test
    fun clampSize_neverZeroOrNegative() {
        val b = Geometry.clampItemBounds(0.1f, 0.1f, -0.5f, 0f)
        assertTrue(b[2] >= Geometry.MIN_SIZE)
        assertTrue(b[3] >= Geometry.MIN_SIZE)
    }

    @Test
    fun moveItem_clampsToBounds() {
        val moved = Geometry.moveItem(item(0.9f, 0.9f, 0.2f, 0.2f), 0.5f, 0.5f)
        assertTrue(moved.normalizedX + moved.normalizedWidth <= 1.0001f)
        assertTrue(moved.normalizedY + moved.normalizedHeight <= 1.0001f)
    }

    @Test
    fun resizeItem_growsAndClamps() {
        val resized = Geometry.resizeItem(item(0.1f, 0.1f, 0.2f, 0.2f), 0.1f, 0.1f)
        assertEquals(0.3f, resized.normalizedWidth, 0.0001f)
    }

    @Test
    fun rotateItem_snapsTo90() {
        assertEquals(90, Geometry.rotateItem(item(0.1f, 0.1f, 0.2f, 0.2f), 90).rotationDegrees)
        assertEquals(0, Geometry.rotateItem(item(0.1f, 0.1f, 0.2f, 0.2f), 360).rotationDegrees)
        assertEquals(270, Geometry.rotateItem(item(0.1f, 0.1f, 0.2f, 0.2f), -90).rotationDegrees)
    }

    @Test
    fun normalizeRotation_bucketsCorrectly() {
        assertEquals(0, Geometry.normalizeRotation(10))
        assertEquals(90, Geometry.normalizeRotation(100))
        assertEquals(180, Geometry.normalizeRotation(190))
        assertEquals(270, Geometry.normalizeRotation(300))
    }

    @Test
    fun itemCenter_isMidpoint() {
        val c = Geometry.itemCenter(item(0.2f, 0.2f, 0.4f, 0.2f))
        assertEquals(0.4f, c.x, 0.0001f)
        assertEquals(0.3f, c.y, 0.0001f)
    }

    @Test
    fun cableAnchor_nullForMissingItem() {
        assertEquals(null, Geometry.cableAnchor(null))
        assertTrue(Geometry.cableAnchor(item(0.2f, 0.2f, 0.4f, 0.2f)) != null)
    }

    @Test
    fun isWithinDesk_rejectsInvalid() {
        assertFalse(Geometry.isWithinDesk(Float.NaN, 0f, 0.2f, 0.2f))
        assertFalse(Geometry.isWithinDesk(0.9f, 0.9f, 0.5f, 0.5f))
        assertTrue(Geometry.isWithinDesk(0.1f, 0.1f, 0.2f, 0.2f))
    }

    @Test
    fun normToPx_handlesZeroCanvas() {
        assertEquals(0f, Geometry.normToPx(0.5f, 0f))
        assertEquals(50f, Geometry.normToPx(0.5f, 100f), 0.001f)
    }

    @Test
    fun pxToNorm_handlesZeroCanvas() {
        assertEquals(0f, Geometry.pxToNorm(50f, 0f))
        assertEquals(0.5f, Geometry.pxToNorm(50f, 100f), 0.001f)
    }

    @Test
    fun deskAspectRatio_defaultsWhenMissing() {
        assertEquals(1.6f, Geometry.deskAspectRatio(null, null), 0.001f)
        assertEquals(1.6f, Geometry.deskAspectRatio(0.0, 10.0), 0.001f)
        assertEquals(2.0f, Geometry.deskAspectRatio(200.0, 100.0), 0.001f)
    }

    @Test
    fun renderOrder_isDeterministic() {
        val a = item(0f, 0f, 0.1f, 0.1f).copy(id = "b")
        val b = item(0f, 0f, 0.1f, 0.1f).copy(id = "a")
        val ordered = Geometry.renderOrder(listOf(a, b))
        assertEquals("a", ordered.first().id)
    }
}
