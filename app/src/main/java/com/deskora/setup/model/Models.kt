package com.deskora.setup.model

import kotlinx.serialization.Serializable

/**
 * Data models for Deskora Setup. All models are @Serializable and use default
 * values so that stored JSON missing newly added fields deserializes safely.
 * Timestamps are ISO-8601 strings. Dates are YYYY-MM-DD strings.
 */

@Serializable
data class NormalizedPoint(
    val x: Float = 0f,
    val y: Float = 0f
)

@Serializable
data class DeskSetup(
    val id: String = "",
    val name: String = "",
    val setupType: SetupType = SetupType.Custom,
    val deskShape: DeskShape = DeskShape.Rectangle,
    val widthCm: Double? = null,
    val depthCm: Double? = null,
    val description: String = "",
    val templateSourceId: String? = null,
    val archived: Boolean = false,
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Serializable
data class DeskZone(
    val id: String = "",
    val setupId: String = "",
    val name: String = "",
    val zoneType: DeskZoneType = DeskZoneType.Custom,
    val normalizedX: Float = 0.1f,
    val normalizedY: Float = 0.1f,
    val normalizedWidth: Float = 0.3f,
    val normalizedHeight: Float = 0.3f,
    val sortOrder: Int = 0,
    val colorKey: String = "zone",
    val note: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Serializable
data class SetupItem(
    val id: String = "",
    val setupId: String = "",
    val zoneId: String? = null,
    val name: String = "",
    val itemType: SetupItemType = SetupItemType.Other,
    val customTypeName: String = "",
    val normalizedX: Float = 0.4f,
    val normalizedY: Float = 0.4f,
    val normalizedWidth: Float = 0.16f,
    val normalizedHeight: Float = 0.10f,
    val rotationDegrees: Int = 0,
    val colorKey: String = "monitor",
    val status: SetupItemStatus = SetupItemStatus.Unplaced,
    val note: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Serializable
data class CableRoute(
    val id: String = "",
    val setupId: String = "",
    val name: String = "",
    val cableType: CableType = CableType.Other,
    val startItemId: String? = null,
    val endItemId: String? = null,
    val intermediatePoints: List<NormalizedPoint> = emptyList(),
    val colorKey: String = "cable_other",
    val hidden: Boolean = false,
    val label: String = "",
    val note: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Serializable
data class CleaningTask(
    val id: String = "",
    val setupId: String = "",
    val title: String = "",
    val category: CleaningCategory = CleaningCategory.General,
    val frequencyType: CleaningFrequencyType = CleaningFrequencyType.Manual,
    val intervalValue: Int? = null,
    val selectedDays: List<WeekDay> = emptyList(),
    val nextDueDate: String = "",
    val lastCompletedDate: String = "",
    val enabled: Boolean = true,
    val note: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Serializable
data class NeededItem(
    val id: String = "",
    val setupId: String = "",
    val title: String = "",
    val category: NeededItemCategory = NeededItemCategory.Other,
    val quantityLabel: String = "",
    val priority: NeededItemPriority = NeededItemPriority.Normal,
    val acquired: Boolean = false,
    val plannedZoneId: String? = null,
    val note: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Serializable
data class TemplateZone(
    val id: String = "",
    val name: String = "",
    val zoneType: DeskZoneType = DeskZoneType.Custom,
    val normalizedX: Float = 0.1f,
    val normalizedY: Float = 0.1f,
    val normalizedWidth: Float = 0.3f,
    val normalizedHeight: Float = 0.3f,
    val sortOrder: Int = 0,
    val colorKey: String = "zone"
)

@Serializable
data class TemplateItem(
    val id: String = "",
    val name: String = "",
    val itemType: SetupItemType = SetupItemType.Other,
    val zoneTemplateId: String? = null,
    val normalizedX: Float = 0.4f,
    val normalizedY: Float = 0.4f,
    val normalizedWidth: Float = 0.16f,
    val normalizedHeight: Float = 0.10f,
    val rotationDegrees: Int = 0,
    val colorKey: String = "monitor"
)

@Serializable
data class SetupTemplate(
    val id: String = "",
    val name: String = "",
    val setupType: SetupType = SetupType.Custom,
    val deskShape: DeskShape = DeskShape.Rectangle,
    val defaultZones: List<TemplateZone> = emptyList(),
    val defaultItems: List<TemplateItem> = emptyList(),
    val description: String = "",
    val userCreated: Boolean = false,
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Serializable
data class AppSettings(
    val onboardingCompleted: Boolean = false,
    val activeSetupId: String? = null,
    val defaultTemplateId: String? = null,
    val showDeskGrid: Boolean = true,
    val showCableLabels: Boolean = true,
    val showZoneLabels: Boolean = true,
    val defaultItemSize: ItemSizePreset = ItemSizePreset.Medium,
    val mapControlStep: MapControlStep = MapControlStep.Medium,
    val firstDayOfWeek: WeekDay = WeekDay.Monday
)

/** Full application data snapshot exposed as a single immutable value. */
data class AppData(
    val setups: List<DeskSetup> = emptyList(),
    val zones: List<DeskZone> = emptyList(),
    val items: List<SetupItem> = emptyList(),
    val cables: List<CableRoute> = emptyList(),
    val cleaningTasks: List<CleaningTask> = emptyList(),
    val neededItems: List<NeededItem> = emptyList(),
    val templates: List<SetupTemplate> = emptyList(),
    val settings: AppSettings = AppSettings()
) {
    val activeSetup: DeskSetup?
        get() = setups.firstOrNull { it.id == settings.activeSetupId && !it.archived }
            ?: setups.firstOrNull { !it.archived }

    fun zonesFor(setupId: String?): List<DeskZone> =
        if (setupId == null) emptyList()
        else zones.filter { it.setupId == setupId }.sortedBy { it.sortOrder }

    fun itemsFor(setupId: String?): List<SetupItem> =
        if (setupId == null) emptyList() else items.filter { it.setupId == setupId }

    fun cablesFor(setupId: String?): List<CableRoute> =
        if (setupId == null) emptyList() else cables.filter { it.setupId == setupId }

    fun tasksFor(setupId: String?): List<CleaningTask> =
        if (setupId == null) emptyList() else cleaningTasks.filter { it.setupId == setupId }

    fun neededFor(setupId: String?): List<NeededItem> =
        if (setupId == null) emptyList() else neededItems.filter { it.setupId == setupId }
}
