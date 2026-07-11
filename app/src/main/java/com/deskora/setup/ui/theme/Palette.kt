package com.deskora.setup.ui.theme

import androidx.compose.ui.graphics.Color
import com.deskora.setup.model.CableType
import com.deskora.setup.model.SetupItemStatus
import com.deskora.setup.model.SetupItemType

/** Maps model color keys and types to concrete Colors. */
object Palette {

    /** Selectable color keys shown in the item/zone editors. */
    val itemColorKeys = listOf(
        "monitor" to DeskoraColors.MonitorSlate,
        "laptop" to DeskoraColors.LaptopBlueGray,
        "accessory" to DeskoraColors.AccessoryClay,
        "storage" to DeskoraColors.StorageSage,
        "lighting" to DeskoraColors.LightingAmber,
        "planned" to DeskoraColors.PlannedViolet,
        "unplaced" to DeskoraColors.UnplacedGray
    )

    val zoneColorKeys = listOf(
        "zone" to DeskoraColors.ZoneFill,
        "cable_area" to DeskoraColors.CableArea,
        "storage" to Color(0xFFDCE6DC),
        "writing" to Color(0xFFEDE7DA)
    )

    fun itemColor(colorKey: String, status: SetupItemStatus): Color {
        if (status == SetupItemStatus.Unplaced) return DeskoraColors.UnplacedGray
        if (status == SetupItemStatus.Planned) return DeskoraColors.PlannedViolet
        if (status == SetupItemStatus.Archived) return DeskoraColors.Archived
        return itemColorKeys.firstOrNull { it.first == colorKey }?.second ?: DeskoraColors.MonitorSlate
    }

    fun zoneColor(colorKey: String): Color =
        zoneColorKeys.firstOrNull { it.first == colorKey }?.second ?: DeskoraColors.ZoneFill

    fun cableColor(type: CableType): Color = when (type) {
        CableType.Power -> DeskoraColors.PowerCable
        CableType.Display -> DeskoraColors.DisplayCable
        CableType.Data -> DeskoraColors.DataCable
        CableType.Audio -> DeskoraColors.AudioCable
        CableType.Charging -> DeskoraColors.ChargingCable
        CableType.Network -> DeskoraColors.NetworkCable
        CableType.Other -> DeskoraColors.OtherCable
    }

    /** Suggested default color key for a new item of a given type. */
    fun defaultColorKey(type: SetupItemType): String = when (type) {
        SetupItemType.Monitor, SetupItemType.DesktopComputer -> "monitor"
        SetupItemType.Laptop -> "laptop"
        SetupItemType.Lamp -> "lighting"
        SetupItemType.StorageTray, SetupItemType.Plant, SetupItemType.Book -> "storage"
        else -> "accessory"
    }
}
