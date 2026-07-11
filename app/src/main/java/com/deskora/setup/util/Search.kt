package com.deskora.setup.util

import com.deskora.setup.model.AppData
import com.deskora.setup.model.CableRoute
import com.deskora.setup.model.CleaningTask
import com.deskora.setup.model.DeskZone
import com.deskora.setup.model.NeededItem
import com.deskora.setup.model.SetupItem

/** Result of a fully local search across a single active setup. */
data class SearchResults(
    val zones: List<DeskZone> = emptyList(),
    val items: List<SetupItem> = emptyList(),
    val cables: List<CableRoute> = emptyList(),
    val tasks: List<CleaningTask> = emptyList(),
    val needed: List<NeededItem> = emptyList()
) {
    val isEmpty: Boolean
        get() = zones.isEmpty() && items.isEmpty() && cables.isEmpty() && tasks.isEmpty() && needed.isEmpty()

    val total: Int
        get() = zones.size + items.size + cables.size + tasks.size + needed.size
}

/** Purely local, in-memory search. Nothing is sent anywhere. */
object Search {

    fun run(data: AppData, setupId: String?, rawQuery: String): SearchResults {
        val q = rawQuery.trim().lowercase()
        if (q.isEmpty() || setupId == null) return SearchResults()

        val zones = data.zonesFor(setupId).filter {
            it.name.lowercase().contains(q) ||
                it.zoneType.label.lowercase().contains(q) ||
                it.note.lowercase().contains(q)
        }
        val items = data.itemsFor(setupId).filter {
            it.name.lowercase().contains(q) ||
                it.itemType.label.lowercase().contains(q) ||
                it.customTypeName.lowercase().contains(q) ||
                it.status.label.lowercase().contains(q) ||
                it.note.lowercase().contains(q)
        }
        val cables = data.cablesFor(setupId).filter {
            it.name.lowercase().contains(q) ||
                it.label.lowercase().contains(q) ||
                it.cableType.label.lowercase().contains(q) ||
                it.note.lowercase().contains(q)
        }
        val tasks = data.tasksFor(setupId).filter {
            it.title.lowercase().contains(q) ||
                it.category.label.lowercase().contains(q) ||
                it.note.lowercase().contains(q)
        }
        val needed = data.neededFor(setupId).filter {
            it.title.lowercase().contains(q) ||
                it.category.label.lowercase().contains(q) ||
                it.note.lowercase().contains(q)
        }
        return SearchResults(zones, items, cables, tasks, needed)
    }
}
