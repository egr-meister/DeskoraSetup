package com.deskora.setup.util

import com.deskora.setup.model.DeskShape
import com.deskora.setup.model.DeskZoneType
import com.deskora.setup.model.SetupTemplate
import com.deskora.setup.model.SetupItemType
import com.deskora.setup.model.SetupType
import com.deskora.setup.model.TemplateItem
import com.deskora.setup.model.TemplateZone

/**
 * Built-in, read-only templates. Their IDs are fixed constants so they are
 * detected and never duplicated across relaunches. All labels are generic;
 * no brand names, prices, or product recommendations are included.
 */
object BuiltInTemplates {

    const val WORK_ID = "builtin_work_desk"
    const val STUDY_ID = "builtin_study_desk"
    const val GAMING_ID = "builtin_gaming_desk"
    const val BLANK_ID = "builtin_blank_desk"

    val builtInIds = setOf(WORK_ID, STUDY_ID, GAMING_ID, BLANK_ID)

    private const val TS = "2024-01-01T00:00:00Z"

    private fun zone(
        id: String, name: String, type: DeskZoneType,
        x: Float, y: Float, w: Float, h: Float, order: Int, color: String
    ) = TemplateZone(id, name, type, x, y, w, h, order, color)

    private fun item(
        id: String, name: String, type: SetupItemType, zoneId: String?,
        x: Float, y: Float, w: Float, h: Float, color: String, rotation: Int = 0
    ) = TemplateItem(id, name, type, zoneId, x, y, w, h, rotation, color)

    fun all(): List<SetupTemplate> = listOf(workDesk(), studyDesk(), gamingDesk(), blankDesk())

    fun workDesk(): SetupTemplate {
        val zMain = "wz_main"
        val zLeft = "wz_left"
        val zCable = "wz_cable"
        return SetupTemplate(
            id = WORK_ID,
            name = "Work Desk",
            setupType = SetupType.WorkDesk,
            deskShape = DeskShape.Rectangle,
            description = "A balanced layout for focused work: a central monitor and " +
                "keyboard, a laptop and notebook to one side, and a cable zone at the rear. " +
                "Every item is a generic placeholder you can rename or remove.",
            userCreated = false,
            createdAt = TS,
            updatedAt = TS,
            defaultZones = listOf(
                zone(zMain, "Center", DeskZoneType.Center, 0.30f, 0.20f, 0.42f, 0.55f, 0, "zone"),
                zone(zLeft, "Left", DeskZoneType.Left, 0.04f, 0.20f, 0.24f, 0.55f, 1, "zone"),
                zone(zCable, "Cable", DeskZoneType.Cable, 0.04f, 0.02f, 0.90f, 0.14f, 2, "cable_area")
            ),
            defaultItems = listOf(
                item("wi_monitor", "Monitor", SetupItemType.Monitor, zMain, 0.36f, 0.22f, 0.30f, 0.12f, "monitor"),
                item("wi_keyboard", "Keyboard", SetupItemType.Keyboard, zMain, 0.34f, 0.55f, 0.34f, 0.10f, "accessory"),
                item("wi_mouse", "Mouse", SetupItemType.Mouse, zMain, 0.70f, 0.56f, 0.07f, 0.09f, "accessory"),
                item("wi_laptop", "Laptop", SetupItemType.Laptop, zLeft, 0.06f, 0.40f, 0.20f, 0.14f, "laptop"),
                item("wi_lamp", "Desk Lamp", SetupItemType.Lamp, zLeft, 0.08f, 0.22f, 0.10f, 0.10f, "lighting"),
                item("wi_notebook", "Notebook Area", SetupItemType.Notebook, zLeft, 0.06f, 0.58f, 0.16f, 0.12f, "accessory")
            )
        )
    }

    fun studyDesk(): SetupTemplate {
        val zWrite = "sz_write"
        val zStore = "sz_store"
        return SetupTemplate(
            id = STUDY_ID,
            name = "Study Desk",
            setupType = SetupType.StudyDesk,
            deskShape = DeskShape.Compact,
            description = "A compact study layout: a laptop and notebook in the writing " +
                "area, a reading lamp, and a small storage tray. Generic placeholders only.",
            userCreated = false,
            createdAt = TS,
            updatedAt = TS,
            defaultZones = listOf(
                zone(zWrite, "Writing", DeskZoneType.Writing, 0.10f, 0.28f, 0.55f, 0.55f, 0, "zone"),
                zone(zStore, "Storage", DeskZoneType.Storage, 0.70f, 0.20f, 0.24f, 0.60f, 1, "zone")
            ),
            defaultItems = listOf(
                item("si_laptop", "Laptop", SetupItemType.Laptop, zWrite, 0.20f, 0.40f, 0.26f, 0.18f, "laptop"),
                item("si_notebook", "Notebook", SetupItemType.Notebook, zWrite, 0.20f, 0.62f, 0.20f, 0.14f, "accessory"),
                item("si_pen", "Pen Holder", SetupItemType.PenHolder, zWrite, 0.50f, 0.40f, 0.08f, 0.10f, "accessory"),
                item("si_lamp", "Reading Lamp", SetupItemType.Lamp, zStore, 0.72f, 0.24f, 0.12f, 0.12f, "lighting"),
                item("si_book", "Book Area", SetupItemType.Book, zStore, 0.72f, 0.44f, 0.18f, 0.14f, "accessory"),
                item("si_tray", "Storage Tray", SetupItemType.StorageTray, zStore, 0.72f, 0.62f, 0.18f, 0.14f, "storage")
            )
        )
    }

    fun gamingDesk(): SetupTemplate {
        val zMain = "gz_main"
        val zAccess = "gz_access"
        val zCable = "gz_cable"
        return SetupTemplate(
            id = GAMING_ID,
            name = "Gaming Desk",
            setupType = SetupType.GamingDesk,
            deskShape = DeskShape.Rectangle,
            description = "A workspace-style gaming layout: a main monitor with a secondary " +
                "monitor placeholder, keyboard and mouse, a headset stand, speaker placeholders, " +
                "a controller area, and a cable zone. Generic placeholders only.",
            userCreated = false,
            createdAt = TS,
            updatedAt = TS,
            defaultZones = listOf(
                zone(zMain, "Main", DeskZoneType.Main, 0.22f, 0.20f, 0.56f, 0.50f, 0, "zone"),
                zone(zAccess, "Right", DeskZoneType.Right, 0.80f, 0.20f, 0.16f, 0.60f, 1, "zone"),
                zone(zCable, "Cable", DeskZoneType.Cable, 0.04f, 0.02f, 0.90f, 0.14f, 2, "cable_area")
            ),
            defaultItems = listOf(
                item("gi_mon1", "Main Monitor", SetupItemType.Monitor, zMain, 0.36f, 0.20f, 0.28f, 0.12f, "monitor"),
                item("gi_mon2", "Secondary Monitor", SetupItemType.Monitor, zMain, 0.62f, 0.22f, 0.18f, 0.10f, "monitor", rotation = 90),
                item("gi_kb", "Keyboard", SetupItemType.Keyboard, zMain, 0.30f, 0.52f, 0.34f, 0.10f, "accessory"),
                item("gi_mouse", "Mouse", SetupItemType.Mouse, zMain, 0.66f, 0.53f, 0.07f, 0.09f, "accessory"),
                item("gi_headset", "Headset Stand", SetupItemType.HeadsetStand, zAccess, 0.81f, 0.24f, 0.12f, 0.12f, "accessory"),
                item("gi_spk1", "Speaker", SetupItemType.Speaker, zMain, 0.24f, 0.22f, 0.08f, 0.12f, "accessory"),
                item("gi_spk2", "Speaker", SetupItemType.Speaker, zMain, 0.68f, 0.22f, 0.08f, 0.12f, "accessory"),
                item("gi_ctrl", "Controller Area", SetupItemType.Controller, zAccess, 0.81f, 0.55f, 0.12f, 0.12f, "accessory")
            )
        )
    }

    fun blankDesk(): SetupTemplate = SetupTemplate(
        id = BLANK_ID,
        name = "Blank Desk",
        setupType = SetupType.Custom,
        deskShape = DeskShape.Rectangle,
        description = "An empty desk surface with no zones or items. Start from scratch.",
        userCreated = false,
        createdAt = TS,
        updatedAt = TS,
        defaultZones = emptyList(),
        defaultItems = emptyList()
    )
}
