package com.deskora.setup.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * All enumerations used by Deskora Setup.
 *
 * Every enum has an "Unknown"/safe default handling done at the repository level:
 * kotlinx.serialization is configured with a UNKNOWN fallback where useful so that
 * older or corrupted stored values never throw during deserialization.
 */

@Serializable
enum class SetupType(val label: String) {
    @SerialName("WorkDesk") WorkDesk("Work Desk"),
    @SerialName("StudyDesk") StudyDesk("Study Desk"),
    @SerialName("GamingDesk") GamingDesk("Gaming Desk"),
    @SerialName("HybridDesk") HybridDesk("Hybrid Desk"),
    @SerialName("Custom") Custom("Custom");

    companion object {
        fun fromNameOrDefault(name: String?): SetupType =
            entries.firstOrNull { it.name == name } ?: Custom
    }
}

@Serializable
enum class DeskShape(val label: String) {
    @SerialName("Rectangle") Rectangle("Rectangle"),
    @SerialName("LShape") LShape("L-Shape"),
    @SerialName("Corner") Corner("Corner"),
    @SerialName("Compact") Compact("Compact"),
    @SerialName("Custom") Custom("Custom");

    companion object {
        fun fromNameOrDefault(name: String?): DeskShape =
            entries.firstOrNull { it.name == name } ?: Rectangle
    }
}

@Serializable
enum class DeskZoneType(val label: String) {
    @SerialName("Main") Main("Main"),
    @SerialName("Left") Left("Left"),
    @SerialName("Center") Center("Center"),
    @SerialName("Right") Right("Right"),
    @SerialName("Rear") Rear("Rear"),
    @SerialName("Front") Front("Front"),
    @SerialName("Cable") Cable("Cable"),
    @SerialName("Storage") Storage("Storage"),
    @SerialName("Writing") Writing("Writing"),
    @SerialName("Custom") Custom("Custom");

    companion object {
        fun fromNameOrDefault(name: String?): DeskZoneType =
            entries.firstOrNull { it.name == name } ?: Custom
    }
}

@Serializable
enum class SetupItemType(val label: String) {
    @SerialName("Monitor") Monitor("Monitor"),
    @SerialName("Laptop") Laptop("Laptop"),
    @SerialName("DesktopComputer") DesktopComputer("Desktop Computer"),
    @SerialName("Keyboard") Keyboard("Keyboard"),
    @SerialName("Mouse") Mouse("Mouse"),
    @SerialName("Speaker") Speaker("Speaker"),
    @SerialName("Headphones") Headphones("Headphones"),
    @SerialName("HeadsetStand") HeadsetStand("Headset Stand"),
    @SerialName("Lamp") Lamp("Lamp"),
    @SerialName("Dock") Dock("Dock"),
    @SerialName("Charger") Charger("Charger"),
    @SerialName("Controller") Controller("Controller"),
    @SerialName("MicrophoneStand") MicrophoneStand("Microphone Stand"),
    @SerialName("Notebook") Notebook("Notebook"),
    @SerialName("Book") Book("Book"),
    @SerialName("PenHolder") PenHolder("Pen Holder"),
    @SerialName("DeskMat") DeskMat("Desk Mat"),
    @SerialName("StorageTray") StorageTray("Storage Tray"),
    @SerialName("Plant") Plant("Plant"),
    @SerialName("CableHub") CableHub("Cable Hub"),
    @SerialName("Other") Other("Other");

    val isDevice: Boolean
        get() = this in setOf(
            Monitor, Laptop, DesktopComputer, Keyboard, Mouse,
            Speaker, Headphones, Dock, Charger, Controller, MicrophoneStand, CableHub
        )

    companion object {
        fun fromNameOrDefault(name: String?): SetupItemType =
            entries.firstOrNull { it.name == name } ?: Other
    }
}

@Serializable
enum class SetupItemStatus(val label: String) {
    @SerialName("Placed") Placed("Placed"),
    @SerialName("Unplaced") Unplaced("Unplaced"),
    @SerialName("Planned") Planned("Planned"),
    @SerialName("Archived") Archived("Archived");

    companion object {
        fun fromNameOrDefault(name: String?): SetupItemStatus =
            entries.firstOrNull { it.name == name } ?: Unplaced
    }
}

@Serializable
enum class CableType(val label: String) {
    @SerialName("Power") Power("Power"),
    @SerialName("Display") Display("Display"),
    @SerialName("Data") Data("Data"),
    @SerialName("Audio") Audio("Audio"),
    @SerialName("Charging") Charging("Charging"),
    @SerialName("Network") Network("Network"),
    @SerialName("Other") Other("Other");

    companion object {
        fun fromNameOrDefault(name: String?): CableType =
            entries.firstOrNull { it.name == name } ?: Other
    }
}

@Serializable
enum class CleaningCategory(val label: String) {
    @SerialName("Surface") Surface("Surface"),
    @SerialName("Screen") Screen("Screen"),
    @SerialName("Keyboard") Keyboard("Keyboard"),
    @SerialName("Mouse") Mouse("Mouse"),
    @SerialName("CableArea") CableArea("Cable Area"),
    @SerialName("Storage") Storage("Storage"),
    @SerialName("Accessories") Accessories("Accessories"),
    @SerialName("General") General("General"),
    @SerialName("Other") Other("Other");

    companion object {
        fun fromNameOrDefault(name: String?): CleaningCategory =
            entries.firstOrNull { it.name == name } ?: General
    }
}

@Serializable
enum class CleaningFrequencyType(val label: String) {
    @SerialName("Manual") Manual("Manual"),
    @SerialName("Daily") Daily("Daily"),
    @SerialName("SelectedDays") SelectedDays("Selected Days"),
    @SerialName("Weekly") Weekly("Weekly"),
    @SerialName("EveryNumberOfDays") EveryNumberOfDays("Every N Days"),
    @SerialName("Monthly") Monthly("Monthly");

    companion object {
        fun fromNameOrDefault(name: String?): CleaningFrequencyType =
            entries.firstOrNull { it.name == name } ?: Manual
    }
}

@Serializable
enum class WeekDay(val label: String, val isoValue: Int) {
    @SerialName("Monday") Monday("Mon", 1),
    @SerialName("Tuesday") Tuesday("Tue", 2),
    @SerialName("Wednesday") Wednesday("Wed", 3),
    @SerialName("Thursday") Thursday("Thu", 4),
    @SerialName("Friday") Friday("Fri", 5),
    @SerialName("Saturday") Saturday("Sat", 6),
    @SerialName("Sunday") Sunday("Sun", 7);

    companion object {
        fun fromIso(value: Int): WeekDay = entries.firstOrNull { it.isoValue == value } ?: Monday
    }
}

/** Computed status label for a cleaning task. Never uses alarming wording. */
enum class CleaningStatus(val label: String) {
    Due("Due"),
    Upcoming("Upcoming"),
    Complete("Complete"),
    Manual("Manual"),
    Disabled("Disabled")
}

@Serializable
enum class NeededItemCategory(val label: String) {
    @SerialName("Device") Device("Device"),
    @SerialName("Accessory") Accessory("Accessory"),
    @SerialName("Cable") Cable("Cable"),
    @SerialName("Storage") Storage("Storage"),
    @SerialName("Lighting") Lighting("Lighting"),
    @SerialName("Cleaning") Cleaning("Cleaning"),
    @SerialName("Stationery") Stationery("Stationery"),
    @SerialName("Other") Other("Other");

    companion object {
        fun fromNameOrDefault(name: String?): NeededItemCategory =
            entries.firstOrNull { it.name == name } ?: Other
    }
}

@Serializable
enum class NeededItemPriority(val label: String) {
    @SerialName("Normal") Normal("Normal"),
    @SerialName("High") High("High");

    companion object {
        fun fromNameOrDefault(name: String?): NeededItemPriority =
            entries.firstOrNull { it.name == name } ?: Normal
    }
}

@Serializable
enum class ItemSizePreset(val label: String, val fraction: Float) {
    @SerialName("Small") Small("Small", 0.10f),
    @SerialName("Medium") Medium("Medium", 0.16f),
    @SerialName("Large") Large("Large", 0.24f)
}

@Serializable
enum class MapControlStep(val label: String, val step: Float) {
    @SerialName("Small") Small("Small", 0.02f),
    @SerialName("Medium") Medium("Medium", 0.05f),
    @SerialName("Large") Large("Large", 0.10f)
}
