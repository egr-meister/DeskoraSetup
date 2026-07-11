package com.deskora.setup.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import com.deskora.setup.model.CableRoute
import com.deskora.setup.model.DeskSetup
import com.deskora.setup.model.DeskZone
import com.deskora.setup.model.SetupItem
import com.deskora.setup.model.SetupItemStatus
import com.deskora.setup.ui.theme.DeskoraColors
import com.deskora.setup.ui.theme.Palette
import com.deskora.setup.util.Geometry
import kotlin.math.max
import kotlin.math.min

/**
 * Top-down desk map. Draws, in order: desk surface, grid, zones, cables, items,
 * selection outlines, labels. Uses normalized coordinates and clips to bounds.
 * Never throws on a zero-sized canvas. Tapping reports the tapped item id.
 */
@Composable
fun DeskMap(
    setup: DeskSetup,
    zones: List<DeskZone>,
    items: List<SetupItem>,
    cables: List<CableRoute>,
    showGrid: Boolean,
    showZoneLabels: Boolean,
    showCableLabels: Boolean,
    modifier: Modifier = Modifier,
    selectedItemId: String? = null,
    selectedZoneId: String? = null,
    onItemTap: (String) -> Unit = {},
    onBackgroundTap: () -> Unit = {}
) {
    val aspect = Geometry.deskAspectRatio(setup.widthCm, setup.depthCm)
    val textMeasurer = rememberTextMeasurer()

    val visibleItems = Geometry.renderOrder(items.filter { it.status != SetupItemStatus.Archived })

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspect, matchHeightConstraintsFirst = false)
            .semantics {
                contentDescription = deskMapDescription(setup, zones, visibleItems, cables)
            }
            .pointerInput(visibleItems, setup.id) {
                detectTapGestures { offset ->
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    if (w <= 0f || h <= 0f) return@detectTapGestures
                    val nx = Geometry.pxToNorm(offset.x, w)
                    val ny = Geometry.pxToNorm(offset.y, h)
                    // Topmost item first (reverse render order).
                    val tapped = visibleItems.lastOrNull { item ->
                        nx in item.normalizedX..(item.normalizedX + item.normalizedWidth) &&
                            ny in item.normalizedY..(item.normalizedY + item.normalizedHeight)
                    }
                    if (tapped != null) onItemTap(tapped.id) else onBackgroundTap()
                }
            }
    ) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        drawDeskSurface(w, h)
        if (showGrid) drawGrid(w, h)
        zones.forEach { zone -> drawZone(zone, w, h, textMeasurer, showZoneLabels, zone.id == selectedZoneId) }
        cables.filter { !it.hidden }.forEach { cable ->
            drawCable(cable, items, w, h, textMeasurer, showCableLabels)
        }
        visibleItems.forEach { item ->
            drawItem(item, w, h, textMeasurer, item.id == selectedItemId)
        }
    }
}

private fun DrawScope.drawDeskSurface(w: Float, h: Float) {
    drawRect(color = DeskoraColors.DeskSurface, size = Size(w, h))
    drawRect(color = DeskoraColors.DeskEdge, size = Size(w, h), style = Stroke(width = with(this) { 3f }))
}

private fun DrawScope.drawGrid(w: Float, h: Float) {
    val divisions = 10
    val stroke = 1f
    for (i in 1 until divisions) {
        val x = w * i / divisions
        drawLine(DeskoraColors.GridLine, Offset(x, 0f), Offset(x, h), strokeWidth = stroke)
        val y = h * i / divisions
        drawLine(DeskoraColors.GridLine, Offset(0f, y), Offset(w, y), strokeWidth = stroke)
    }
}

private fun DrawScope.drawZone(
    zone: DeskZone,
    w: Float, h: Float,
    textMeasurer: TextMeasurer,
    showLabel: Boolean,
    selected: Boolean
) {
    val b = Geometry.clampZoneBounds(zone.normalizedX, zone.normalizedY, zone.normalizedWidth, zone.normalizedHeight)
    val left = b[0] * w
    val top = b[1] * h
    val zw = b[2] * w
    val zh = b[3] * h
    val fill = Palette.zoneColor(zone.colorKey).copy(alpha = 0.35f)
    drawRect(color = fill, topLeft = Offset(left, top), size = Size(zw, zh))
    drawRect(
        color = if (selected) DeskoraColors.BlueprintNavy else DeskoraColors.ZoneBorder,
        topLeft = Offset(left, top),
        size = Size(zw, zh),
        style = Stroke(width = if (selected) 3f else 1.5f)
    )
    if (showLabel && zw > 40f) {
        drawLabel(textMeasurer, zone.name, topLeft = Offset(left + 6f, top + 4f))
    }
}

private fun DrawScope.drawCable(
    cable: CableRoute,
    items: List<SetupItem>,
    w: Float, h: Float,
    textMeasurer: TextMeasurer,
    showLabel: Boolean
) {
    val start = items.firstOrNull { it.id == cable.startItemId }
    val end = items.firstOrNull { it.id == cable.endItemId }
    if (start == null || end == null) return // missing endpoint: nothing to draw on the map
    val a = Geometry.itemCenter(start)
    val z = Geometry.itemCenter(end)
    val color = Palette.cableColor(cable.cableType)

    val points = mutableListOf(Offset(a.x * w, a.y * h))
    cable.intermediatePoints.forEach { p ->
        points.add(Offset(Geometry.clampCoord(p.x) * w, Geometry.clampCoord(p.y) * h))
    }
    points.add(Offset(z.x * w, z.y * h))

    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
    }
    drawPath(path, color = color, style = Stroke(width = 3f))
    // Anchors
    drawCircle(color, radius = 5f, center = points.first())
    drawCircle(color, radius = 5f, center = points.last())

    if (showLabel && cable.label.isNotBlank()) {
        val mid = points[points.size / 2]
        drawLabel(textMeasurer, cable.label, topLeft = Offset(mid.x + 4f, mid.y - 18f))
    }
}

private fun DrawScope.drawItem(
    item: SetupItem,
    w: Float, h: Float,
    textMeasurer: TextMeasurer,
    selected: Boolean
) {
    val b = Geometry.clampItemBounds(item.normalizedX, item.normalizedY, item.normalizedWidth, item.normalizedHeight)
    val left = b[0] * w
    val top = b[1] * h
    val iw = max(6f, b[2] * w)
    val ih = max(6f, b[3] * h)
    val color = Palette.itemColor(item.colorKey, item.status)
    val rect = Rect(left, top, left + iw, top + ih)

    // Rotate the shape drawing around the item's center for 90-degree increments.
    val cx = rect.center.x
    val cy = rect.center.y
    rotateRad(item.rotationDegrees.toFloat(), cx, cy) {
        drawItemShape(item, rect, color, textMeasurer)
    }

    if (selected) {
        drawRect(
            color = DeskoraColors.BlueprintNavy,
            topLeft = Offset(left - 3f, top - 3f),
            size = Size(iw + 6f, ih + 6f),
            style = Stroke(width = 3f)
        )
    }
}

/** Draws an abstract, non-branded representation of the item by type. */
private fun DrawScope.drawItemShape(
    item: SetupItem,
    rect: Rect,
    color: Color,
    textMeasurer: TextMeasurer
) {
    val stroke = Stroke(width = 2f)
    val fill = color.copy(alpha = 0.85f)
    when (item.itemType) {
        com.deskora.setup.model.SetupItemType.Monitor -> {
            drawRect(fill, topLeft = rect.topLeft, size = rect.size)
            drawRect(DeskoraColors.DeepText.copy(alpha = 0.4f), topLeft = rect.topLeft, size = rect.size, style = stroke)
            // stand line
            drawLine(color, Offset(rect.center.x, rect.bottom), Offset(rect.center.x, rect.bottom + rect.height * 0.15f), strokeWidth = 3f)
        }
        com.deskora.setup.model.SetupItemType.Laptop -> {
            drawRect(fill, topLeft = rect.topLeft, size = rect.size)
            drawLine(DeskoraColors.DeepText.copy(alpha = 0.5f), Offset(rect.left, rect.center.y), Offset(rect.right, rect.center.y), strokeWidth = 2f)
        }
        com.deskora.setup.model.SetupItemType.Keyboard -> {
            drawRect(fill, topLeft = rect.topLeft, size = rect.size)
            val rows = 3
            for (r in 1 until rows) {
                val y = rect.top + rect.height * r / rows
                drawLine(DeskoraColors.Surface.copy(alpha = 0.6f), Offset(rect.left + 3f, y), Offset(rect.right - 3f, y), strokeWidth = 1.5f)
            }
        }
        com.deskora.setup.model.SetupItemType.Mouse, com.deskora.setup.model.SetupItemType.Controller -> {
            drawOval(fill, topLeft = rect.topLeft, size = rect.size)
        }
        com.deskora.setup.model.SetupItemType.Lamp -> {
            drawCircle(fill, radius = min(rect.width, rect.height) / 2f, center = rect.center)
            drawLine(color, rect.center, Offset(rect.center.x, rect.top), strokeWidth = 3f)
        }
        com.deskora.setup.model.SetupItemType.Headphones, com.deskora.setup.model.SetupItemType.HeadsetStand -> {
            drawArc(fill, startAngle = 180f, sweepAngle = 180f, useCenter = false, topLeft = rect.topLeft, size = rect.size, style = Stroke(width = 4f))
        }
        com.deskora.setup.model.SetupItemType.Plant -> {
            drawCircle(DeskoraColors.StorageSage, radius = min(rect.width, rect.height) / 3f, center = rect.center)
            drawCircle(fill, radius = min(rect.width, rect.height) / 5f, center = Offset(rect.center.x, rect.bottom - rect.height * 0.2f))
        }
        com.deskora.setup.model.SetupItemType.Notebook, com.deskora.setup.model.SetupItemType.Book -> {
            drawRect(DeskoraColors.Surface, topLeft = rect.topLeft, size = rect.size)
            drawRect(color, topLeft = rect.topLeft, size = rect.size, style = stroke)
            val rows = 3
            for (r in 1..rows) {
                val y = rect.top + rect.height * r / (rows + 1)
                drawLine(color.copy(alpha = 0.5f), Offset(rect.left + 4f, y), Offset(rect.right - 4f, y), strokeWidth = 1f)
            }
        }
        com.deskora.setup.model.SetupItemType.Speaker -> {
            drawRect(fill, topLeft = rect.topLeft, size = rect.size)
            drawCircle(DeskoraColors.Surface.copy(alpha = 0.7f), radius = min(rect.width, rect.height) / 4f, center = rect.center)
        }
        else -> {
            // Generic rounded plate for other types.
            drawRect(fill, topLeft = rect.topLeft, size = rect.size)
            drawRect(DeskoraColors.DeepText.copy(alpha = 0.3f), topLeft = rect.topLeft, size = rect.size, style = stroke)
        }
    }

    // Item label under the shape when there is room.
    if (rect.width > 44f) {
        drawLabel(textMeasurer, item.name, topLeft = Offset(rect.left, rect.bottom + 2f))
    }
}

/** Helper to draw text with a measurer at a top-left offset. */
private fun DrawScope.drawLabel(measurer: TextMeasurer, text: String, topLeft: Offset) {
    if (text.isBlank()) return
    drawText(measurer, text, topLeft = topLeft)
}

/** Rotates the drawing scope by whole degrees around a pivot, then restores. */
private inline fun DrawScope.rotateRad(degrees: Float, pivotX: Float, pivotY: Float, block: DrawScope.() -> Unit) {
    if (degrees == 0f) {
        block()
        return
    }
    drawContext.canvas.save()
    drawContext.canvas.nativeCanvas.rotate(degrees, pivotX, pivotY)
    block()
    drawContext.canvas.restore()
}

/** Accessible text description of the desk map. */
private fun deskMapDescription(
    setup: DeskSetup,
    zones: List<DeskZone>,
    items: List<SetupItem>,
    cables: List<CableRoute>
): String {
    val placed = items.count { it.status == SetupItemStatus.Placed }
    val visibleCables = cables.count { !it.hidden }
    return buildString {
        append("Top-down desk map for ${setup.name}. ")
        append("${zones.size} zones, ${items.size} items on the desk, ")
        append("$placed placed, $visibleCables visible cable routes.")
    }
}
