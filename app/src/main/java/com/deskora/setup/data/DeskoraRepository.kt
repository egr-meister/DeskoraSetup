package com.deskora.setup.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.deskora.setup.model.AppData
import com.deskora.setup.model.AppSettings
import com.deskora.setup.model.CableRoute
import com.deskora.setup.model.CleaningFrequencyType
import com.deskora.setup.model.CleaningTask
import com.deskora.setup.model.DeskSetup
import com.deskora.setup.model.DeskZone
import com.deskora.setup.model.NeededItem
import com.deskora.setup.model.SetupItem
import com.deskora.setup.model.SetupItemStatus
import com.deskora.setup.model.SetupItemType
import com.deskora.setup.model.SetupTemplate
import com.deskora.setup.model.SetupType
import com.deskora.setup.util.BuiltInTemplates
import com.deskora.setup.util.ChecklistDates
import com.deskora.setup.util.Geometry
import com.deskora.setup.util.Ids
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalDate

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "deskora_setup")

/**
 * Single local repository backed by DataStore Preferences with serialized JSON.
 * All operations are guarded and never throw into the UI. Built-in templates are
 * merged in on read and are never persisted as user-editable copies.
 */
class DeskoraRepository(private val context: Context) {

    private object Keys {
        val setups = stringPreferencesKey("desk_setups_json")
        val zones = stringPreferencesKey("desk_zones_json")
        val items = stringPreferencesKey("setup_items_json")
        val cables = stringPreferencesKey("cable_routes_json")
        val tasks = stringPreferencesKey("cleaning_tasks_json")
        val needed = stringPreferencesKey("needed_items_json")
        val templates = stringPreferencesKey("setup_templates_json") // user-created only
        val settings = stringPreferencesKey("settings_json")
    }

    val appData: Flow<AppData> = context.dataStore.data
        .catch { e ->
            // Corrupted preferences file: emit empty and recover gracefully.
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs -> readAppData(prefs) }

    private fun readAppData(prefs: Preferences): AppData {
        val setups = AppJson.decodeListSafely(prefs[Keys.setups], DeskSetup.serializer())
        val zones = AppJson.decodeListSafely(prefs[Keys.zones], DeskZone.serializer())
        val items = AppJson.decodeListSafely(prefs[Keys.items], SetupItem.serializer())
        val cables = AppJson.decodeListSafely(prefs[Keys.cables], CableRoute.serializer())
        val tasks = AppJson.decodeListSafely(prefs[Keys.tasks], CleaningTask.serializer())
        val needed = AppJson.decodeListSafely(prefs[Keys.needed], NeededItem.serializer())
        val userTemplates = AppJson.decodeListSafely(prefs[Keys.templates], SetupTemplate.serializer())
        val settings = AppJson.decodeObjectSafely(prefs[Keys.settings], AppSettings.serializer(), AppSettings())

        // Merge built-in templates (read-only) with user templates, de-duplicated by id.
        val userTemplatesFiltered = userTemplates.filter { it.id !in BuiltInTemplates.builtInIds }
        val templates = BuiltInTemplates.all() + userTemplatesFiltered

        return AppData(
            setups = setups,
            zones = zones,
            items = items,
            cables = cables,
            cleaningTasks = tasks,
            neededItems = needed,
            templates = templates,
            settings = settings
        )
    }

    // ---- Core mutation helper -------------------------------------------------

    private suspend fun mutate(transform: (AppData) -> AppData) {
        context.dataStore.edit { prefs ->
            val current = readAppData(prefs)
            val next = transform(current)
            writeAppData(prefs, next)
        }
    }

    private fun writeAppData(prefs: androidx.datastore.preferences.core.MutablePreferences, data: AppData) {
        prefs[Keys.setups] = AppJson.encodeList(
            kotlinx.serialization.builtins.ListSerializer(DeskSetup.serializer()), data.setups
        )
        prefs[Keys.zones] = AppJson.encodeList(
            kotlinx.serialization.builtins.ListSerializer(DeskZone.serializer()), data.zones
        )
        prefs[Keys.items] = AppJson.encodeList(
            kotlinx.serialization.builtins.ListSerializer(SetupItem.serializer()), data.items
        )
        prefs[Keys.cables] = AppJson.encodeList(
            kotlinx.serialization.builtins.ListSerializer(CableRoute.serializer()), data.cables
        )
        prefs[Keys.tasks] = AppJson.encodeList(
            kotlinx.serialization.builtins.ListSerializer(CleaningTask.serializer()), data.cleaningTasks
        )
        prefs[Keys.needed] = AppJson.encodeList(
            kotlinx.serialization.builtins.ListSerializer(NeededItem.serializer()), data.neededItems
        )
        // Persist only user-created templates; built-ins are always merged on read.
        val userTemplates = data.templates.filter { it.userCreated && it.id !in BuiltInTemplates.builtInIds }
        prefs[Keys.templates] = AppJson.encodeList(
            kotlinx.serialization.builtins.ListSerializer(SetupTemplate.serializer()), userTemplates
        )
        prefs[Keys.settings] = try {
            AppJson.instance.encodeToString(AppSettings.serializer(), data.settings)
        } catch (e: Exception) {
            AppJson.instance.encodeToString(AppSettings.serializer(), AppSettings())
        }
    }

    // ---- Settings -------------------------------------------------------------

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) = mutate { data ->
        data.copy(settings = transform(data.settings))
    }

    suspend fun completeOnboarding() = updateSettings { it.copy(onboardingCompleted = true) }

    suspend fun setActiveSetup(id: String?) = updateSettings { it.copy(activeSetupId = id) }

    // ---- Setups ---------------------------------------------------------------

    suspend fun createSetup(
        name: String,
        setupType: SetupType,
        deskShape: com.deskora.setup.model.DeskShape,
        widthCm: Double?,
        depthCm: Double?,
        description: String,
        templateSourceId: String? = null,
        makeActive: Boolean = true
    ): String {
        val id = Ids.newId("setup")
        val now = Ids.nowIso()
        mutate { data ->
            val setup = DeskSetup(
                id = id, name = name.trim(), setupType = setupType, deskShape = deskShape,
                widthCm = widthCm, depthCm = depthCm, description = description.trim(),
                templateSourceId = templateSourceId, archived = false, createdAt = now, updatedAt = now
            )
            val settings = if (makeActive) data.settings.copy(activeSetupId = id) else data.settings
            data.copy(setups = data.setups + setup, settings = settings)
        }
        return id
    }

    suspend fun updateSetup(setup: DeskSetup) = mutate { data ->
        data.copy(setups = data.setups.map {
            if (it.id == setup.id) setup.copy(updatedAt = Ids.nowIso()) else it
        })
    }

    suspend fun duplicateSetup(sourceId: String): String? {
        val newId = Ids.newId("setup")
        var created = false
        mutate { data ->
            val source = data.setups.firstOrNull { it.id == sourceId } ?: return@mutate data
            created = true
            val now = Ids.nowIso()
            val idMap = mutableMapOf<String, String>() // oldZoneId -> newZoneId

            val newSetup = source.copy(
                id = newId, name = source.name + " (Copy)", archived = false,
                createdAt = now, updatedAt = now
            )
            val newZones = data.zones.filter { it.setupId == sourceId }.map { z ->
                val nz = Ids.newId("zone")
                idMap[z.id] = nz
                z.copy(id = nz, setupId = newId, createdAt = now, updatedAt = now)
            }
            val itemIdMap = mutableMapOf<String, String>()
            val newItems = data.items.filter { it.setupId == sourceId }.map { item ->
                val ni = Ids.newId("item")
                itemIdMap[item.id] = ni
                item.copy(
                    id = ni, setupId = newId,
                    zoneId = item.zoneId?.let { idMap[it] },
                    createdAt = now, updatedAt = now
                )
            }
            val newCables = data.cables.filter { it.setupId == sourceId }.map { c ->
                c.copy(
                    id = Ids.newId("cable"), setupId = newId,
                    startItemId = c.startItemId?.let { itemIdMap[it] },
                    endItemId = c.endItemId?.let { itemIdMap[it] },
                    createdAt = now, updatedAt = now
                )
            }
            val newTasks = data.cleaningTasks.filter { it.setupId == sourceId }.map { t ->
                t.copy(id = Ids.newId("task"), setupId = newId, createdAt = now, updatedAt = now)
            }
            val newNeeded = data.neededItems.filter { it.setupId == sourceId }.map { n ->
                n.copy(
                    id = Ids.newId("need"), setupId = newId,
                    plannedZoneId = n.plannedZoneId?.let { idMap[it] },
                    createdAt = now, updatedAt = now
                )
            }
            data.copy(
                setups = data.setups + newSetup,
                zones = data.zones + newZones,
                items = data.items + newItems,
                cables = data.cables + newCables,
                cleaningTasks = data.cleaningTasks + newTasks,
                neededItems = data.neededItems + newNeeded,
                settings = data.settings.copy(activeSetupId = newId)
            )
        }
        return if (created) newId else null
    }

    suspend fun archiveSetup(id: String, archived: Boolean) = mutate { data ->
        val setups = data.setups.map {
            if (it.id == id) it.copy(archived = archived, updatedAt = Ids.nowIso()) else it
        }
        val settings = if (archived && data.settings.activeSetupId == id) {
            data.settings.copy(activeSetupId = setups.firstOrNull { !it.archived }?.id)
        } else data.settings
        data.copy(setups = setups, settings = settings)
    }

    suspend fun deleteSetup(id: String) = mutate { data ->
        val remainingSetups = data.setups.filter { it.id != id }
        val settings = if (data.settings.activeSetupId == id) {
            data.settings.copy(activeSetupId = remainingSetups.firstOrNull { !it.archived }?.id)
        } else data.settings
        data.copy(
            setups = remainingSetups,
            zones = data.zones.filter { it.setupId != id },
            items = data.items.filter { it.setupId != id },
            cables = data.cables.filter { it.setupId != id },
            cleaningTasks = data.cleaningTasks.filter { it.setupId != id },
            neededItems = data.neededItems.filter { it.setupId != id },
            settings = settings
        )
    }

    // ---- Templates ------------------------------------------------------------

    /** Applies a template to a brand-new setup, copying zones and generic items. */
    suspend fun applyTemplate(templateId: String, setupName: String): String? {
        val newSetupId = Ids.newId("setup")
        var ok = false
        mutate { data ->
            val template = data.templates.firstOrNull { it.id == templateId } ?: return@mutate data
            ok = true
            val now = Ids.nowIso()
            val setup = DeskSetup(
                id = newSetupId,
                name = setupName.ifBlank { template.name },
                setupType = template.setupType,
                deskShape = template.deskShape,
                description = template.description,
                templateSourceId = template.id,
                createdAt = now, updatedAt = now
            )
            val zoneIdMap = mutableMapOf<String, String>()
            val zones = template.defaultZones.mapIndexed { index, tz ->
                val nz = Ids.newId("zone")
                zoneIdMap[tz.id] = nz
                DeskZone(
                    id = nz, setupId = newSetupId, name = tz.name, zoneType = tz.zoneType,
                    normalizedX = tz.normalizedX, normalizedY = tz.normalizedY,
                    normalizedWidth = tz.normalizedWidth, normalizedHeight = tz.normalizedHeight,
                    sortOrder = if (tz.sortOrder != 0) tz.sortOrder else index,
                    colorKey = tz.colorKey, createdAt = now, updatedAt = now
                )
            }
            val items = template.defaultItems.map { ti ->
                val type = safeItemType(ti.itemType)
                SetupItem(
                    id = Ids.newId("item"), setupId = newSetupId,
                    zoneId = ti.zoneTemplateId?.let { zoneIdMap[it] },
                    name = ti.name, itemType = type,
                    normalizedX = ti.normalizedX, normalizedY = ti.normalizedY,
                    normalizedWidth = ti.normalizedWidth, normalizedHeight = ti.normalizedHeight,
                    rotationDegrees = Geometry.normalizeRotation(ti.rotationDegrees),
                    colorKey = ti.colorKey, status = SetupItemStatus.Placed,
                    createdAt = now, updatedAt = now
                )
            }
            data.copy(
                setups = data.setups + setup,
                zones = data.zones + zones,
                items = data.items + items,
                settings = data.settings.copy(activeSetupId = newSetupId)
            )
        }
        return if (ok) newSetupId else null
    }

    private fun safeItemType(type: SetupItemType): SetupItemType = type

    suspend fun saveCurrentSetupAsTemplate(setupId: String, templateName: String): String? {
        val templateId = Ids.newId("tpl")
        var ok = false
        mutate { data ->
            val setup = data.setups.firstOrNull { it.id == setupId } ?: return@mutate data
            ok = true
            val now = Ids.nowIso()
            val zones = data.zones.filter { it.setupId == setupId }
            val zoneIdToTemplateId = zones.associate { it.id to Ids.newId("tz") }
            val templateZones = zones.map { z ->
                com.deskora.setup.model.TemplateZone(
                    id = zoneIdToTemplateId.getValue(z.id), name = z.name, zoneType = z.zoneType,
                    normalizedX = z.normalizedX, normalizedY = z.normalizedY,
                    normalizedWidth = z.normalizedWidth, normalizedHeight = z.normalizedHeight,
                    sortOrder = z.sortOrder, colorKey = z.colorKey
                )
            }
            val templateItems = data.items
                .filter { it.setupId == setupId && it.status != SetupItemStatus.Archived }
                .map { item ->
                    com.deskora.setup.model.TemplateItem(
                        id = Ids.newId("ti"), name = item.name, itemType = item.itemType,
                        zoneTemplateId = item.zoneId?.let { zoneIdToTemplateId[it] },
                        normalizedX = item.normalizedX, normalizedY = item.normalizedY,
                        normalizedWidth = item.normalizedWidth, normalizedHeight = item.normalizedHeight,
                        rotationDegrees = item.rotationDegrees, colorKey = item.colorKey
                    )
                }
            val template = SetupTemplate(
                id = templateId, name = templateName.ifBlank { setup.name + " Template" },
                setupType = setup.setupType, deskShape = setup.deskShape,
                defaultZones = templateZones, defaultItems = templateItems,
                description = "Custom template saved from " + setup.name,
                userCreated = true, createdAt = now, updatedAt = now
            )
            data.copy(templates = data.templates + template)
        }
        return if (ok) templateId else null
    }

    suspend fun deleteTemplate(id: String) = mutate { data ->
        if (id in BuiltInTemplates.builtInIds) return@mutate data // built-ins are read-only
        data.copy(templates = data.templates.filter { it.id != id })
    }

    suspend fun duplicateTemplate(id: String): String? {
        val newId = Ids.newId("tpl")
        var ok = false
        mutate { data ->
            val source = data.templates.firstOrNull { it.id == id } ?: return@mutate data
            ok = true
            val now = Ids.nowIso()
            val copy = source.copy(
                id = newId, name = source.name + " (Copy)", userCreated = true,
                createdAt = now, updatedAt = now
            )
            data.copy(templates = data.templates + copy)
        }
        return if (ok) newId else null
    }

    suspend fun restoreBuiltInTemplates() {
        // Built-ins are always merged on read; this is a no-op that also clears any
        // stale user copy that collided with a built-in id.
        mutate { data ->
            data.copy(templates = data.templates.filter { it.id !in BuiltInTemplates.builtInIds || !it.userCreated })
        }
    }

    // ---- Zones ----------------------------------------------------------------

    suspend fun createZone(zone: DeskZone): String {
        val id = if (zone.id.isBlank()) Ids.newId("zone") else zone.id
        val now = Ids.nowIso()
        mutate { data ->
            val order = if (zone.sortOrder != 0) zone.sortOrder
            else (data.zones.filter { it.setupId == zone.setupId }.maxOfOrNull { it.sortOrder } ?: -1) + 1
            val b = Geometry.clampZoneBounds(zone.normalizedX, zone.normalizedY, zone.normalizedWidth, zone.normalizedHeight)
            val newZone = zone.copy(
                id = id, sortOrder = order,
                normalizedX = b[0], normalizedY = b[1], normalizedWidth = b[2], normalizedHeight = b[3],
                createdAt = now, updatedAt = now
            )
            data.copy(zones = data.zones + newZone)
        }
        return id
    }

    suspend fun updateZone(zone: DeskZone) = mutate { data ->
        val b = Geometry.clampZoneBounds(zone.normalizedX, zone.normalizedY, zone.normalizedWidth, zone.normalizedHeight)
        data.copy(zones = data.zones.map {
            if (it.id == zone.id) zone.copy(
                normalizedX = b[0], normalizedY = b[1], normalizedWidth = b[2], normalizedHeight = b[3],
                updatedAt = Ids.nowIso()
            ) else it
        })
    }

    suspend fun reorderZone(zoneId: String, direction: Int) = mutate { data ->
        val zone = data.zones.firstOrNull { it.id == zoneId } ?: return@mutate data
        val siblings = data.zones.filter { it.setupId == zone.setupId }.sortedBy { it.sortOrder }.toMutableList()
        val index = siblings.indexOfFirst { it.id == zoneId }
        val target = index + direction
        if (index < 0 || target < 0 || target >= siblings.size) return@mutate data
        val tmp = siblings[index]
        siblings[index] = siblings[target]
        siblings[target] = tmp
        val reindexed = siblings.mapIndexed { i, z -> z.copy(sortOrder = i, updatedAt = Ids.nowIso()) }
        val others = data.zones.filter { it.setupId != zone.setupId }
        data.copy(zones = others + reindexed)
    }

    /** Deletes an empty zone. Returns false (no change) if the zone still holds items. */
    suspend fun deleteZoneIfEmpty(zoneId: String): Boolean {
        var deleted = false
        mutate { data ->
            val hasItems = data.items.any { it.zoneId == zoneId }
            if (hasItems) return@mutate data
            deleted = true
            data.copy(zones = data.zones.filter { it.id != zoneId })
        }
        return deleted
    }

    /** Reassigns all items in [zoneId] to [targetZoneId] (or unplaced) then deletes the zone. */
    suspend fun reassignAndDeleteZone(zoneId: String, targetZoneId: String?) = mutate { data ->
        val now = Ids.nowIso()
        val items = data.items.map { item ->
            if (item.zoneId == zoneId) {
                if (targetZoneId == null) {
                    item.copy(zoneId = null, status = SetupItemStatus.Unplaced, updatedAt = now)
                } else {
                    item.copy(zoneId = targetZoneId, updatedAt = now)
                }
            } else item
        }
        data.copy(items = items, zones = data.zones.filter { it.id != zoneId })
    }

    // ---- Items ----------------------------------------------------------------

    suspend fun createItem(item: SetupItem): String {
        val id = if (item.id.isBlank()) Ids.newId("item") else item.id
        val now = Ids.nowIso()
        mutate { data ->
            val b = Geometry.clampItemBounds(item.normalizedX, item.normalizedY, item.normalizedWidth, item.normalizedHeight)
            val newItem = item.copy(
                id = id, normalizedX = b[0], normalizedY = b[1], normalizedWidth = b[2], normalizedHeight = b[3],
                rotationDegrees = Geometry.normalizeRotation(item.rotationDegrees),
                createdAt = now, updatedAt = now
            )
            data.copy(items = data.items + newItem)
        }
        return id
    }

    suspend fun updateItem(item: SetupItem) = mutate { data ->
        val b = Geometry.clampItemBounds(item.normalizedX, item.normalizedY, item.normalizedWidth, item.normalizedHeight)
        data.copy(items = data.items.map {
            if (it.id == item.id) item.copy(
                normalizedX = b[0], normalizedY = b[1], normalizedWidth = b[2], normalizedHeight = b[3],
                rotationDegrees = Geometry.normalizeRotation(item.rotationDegrees),
                updatedAt = Ids.nowIso()
            ) else it
        })
    }

    suspend fun duplicateItem(itemId: String): String? {
        val newId = Ids.newId("item")
        var ok = false
        mutate { data ->
            val source = data.items.firstOrNull { it.id == itemId } ?: return@mutate data
            ok = true
            val now = Ids.nowIso()
            val moved = Geometry.moveItem(source, 0.03f, 0.03f)
            val copy = moved.copy(id = newId, name = source.name + " (Copy)", createdAt = now, updatedAt = now)
            data.copy(items = data.items + copy)
        }
        return if (ok) newId else null
    }

    suspend fun moveItemToZone(itemId: String, zoneId: String?) = mutate { data ->
        data.copy(items = data.items.map {
            if (it.id == itemId) it.copy(
                zoneId = zoneId,
                status = if (it.status == SetupItemStatus.Unplaced && zoneId != null) SetupItemStatus.Placed else it.status,
                updatedAt = Ids.nowIso()
            ) else it
        })
    }

    suspend fun setItemStatus(itemId: String, status: SetupItemStatus) = mutate { data ->
        data.copy(items = data.items.map {
            if (it.id == itemId) it.copy(
                status = status,
                zoneId = if (status == SetupItemStatus.Unplaced) null else it.zoneId,
                updatedAt = Ids.nowIso()
            ) else it
        })
    }

    suspend fun nudgeItem(itemId: String, dx: Float, dy: Float) = mutate { data ->
        data.copy(items = data.items.map {
            if (it.id == itemId) Geometry.moveItem(it, dx, dy).copy(updatedAt = Ids.nowIso()) else it
        })
    }

    suspend fun resizeItem(itemId: String, dw: Float, dh: Float) = mutate { data ->
        data.copy(items = data.items.map {
            if (it.id == itemId) Geometry.resizeItem(it, dw, dh).copy(updatedAt = Ids.nowIso()) else it
        })
    }

    suspend fun rotateItem(itemId: String, delta: Int) = mutate { data ->
        data.copy(items = data.items.map {
            if (it.id == itemId) Geometry.rotateItem(it, delta).copy(updatedAt = Ids.nowIso()) else it
        })
    }

    suspend fun deleteItem(itemId: String) = mutate { data ->
        // Do not delete cables; mark them as missing endpoints instead.
        val cables = data.cables.map { c ->
            when {
                c.startItemId == itemId -> c.copy(startItemId = null, updatedAt = Ids.nowIso())
                c.endItemId == itemId -> c.copy(endItemId = null, updatedAt = Ids.nowIso())
                else -> c
            }
        }
        data.copy(items = data.items.filter { it.id != itemId }, cables = cables)
    }

    // ---- Cables ---------------------------------------------------------------

    suspend fun createCable(cable: CableRoute): String {
        val id = if (cable.id.isBlank()) Ids.newId("cable") else cable.id
        val now = Ids.nowIso()
        mutate { data ->
            data.copy(cables = data.cables + cable.copy(id = id, createdAt = now, updatedAt = now))
        }
        return id
    }

    suspend fun updateCable(cable: CableRoute) = mutate { data ->
        data.copy(cables = data.cables.map {
            if (it.id == cable.id) cable.copy(updatedAt = Ids.nowIso()) else it
        })
    }

    suspend fun setCableHidden(cableId: String, hidden: Boolean) = mutate { data ->
        data.copy(cables = data.cables.map {
            if (it.id == cableId) it.copy(hidden = hidden, updatedAt = Ids.nowIso()) else it
        })
    }

    suspend fun deleteCable(cableId: String) = mutate { data ->
        data.copy(cables = data.cables.filter { it.id != cableId })
    }

    // ---- Cleaning tasks -------------------------------------------------------

    suspend fun createTask(task: CleaningTask): String {
        val id = if (task.id.isBlank()) Ids.newId("task") else task.id
        val now = Ids.nowIso()
        mutate { data ->
            val due = if (task.nextDueDate.isBlank()) {
                ChecklistDates.computeInitialDueDate(
                    task.frequencyType, task.intervalValue, task.selectedDays, ChecklistDates.today()
                )
            } else task.nextDueDate
            data.copy(cleaningTasks = data.cleaningTasks + task.copy(id = id, nextDueDate = due, createdAt = now, updatedAt = now))
        }
        return id
    }

    suspend fun updateTask(task: CleaningTask) = mutate { data ->
        data.copy(cleaningTasks = data.cleaningTasks.map {
            if (it.id == task.id) task.copy(updatedAt = Ids.nowIso()) else it
        })
    }

    suspend fun completeTask(taskId: String, on: LocalDate = ChecklistDates.today()) = mutate { data ->
        data.copy(cleaningTasks = data.cleaningTasks.map { task ->
            if (task.id != taskId) return@map task
            val nextDue = ChecklistDates.computeNextDueDate(task, on)
            task.copy(
                lastCompletedDate = ChecklistDates.format(on),
                nextDueDate = nextDue,
                updatedAt = Ids.nowIso()
            )
        })
    }

    suspend fun undoTaskCompletion(taskId: String) = mutate { data ->
        data.copy(cleaningTasks = data.cleaningTasks.map { task ->
            if (task.id != taskId) return@map task
            val prevDue = if (task.frequencyType == CleaningFrequencyType.Manual) "" else ChecklistDates.format(ChecklistDates.today())
            task.copy(lastCompletedDate = "", nextDueDate = prevDue, updatedAt = Ids.nowIso())
        })
    }

    suspend fun setTaskEnabled(taskId: String, enabled: Boolean) = mutate { data ->
        data.copy(cleaningTasks = data.cleaningTasks.map {
            if (it.id == taskId) it.copy(enabled = enabled, updatedAt = Ids.nowIso()) else it
        })
    }

    suspend fun deleteTask(taskId: String) = mutate { data ->
        data.copy(cleaningTasks = data.cleaningTasks.filter { it.id != taskId })
    }

    suspend fun clearCompletedHistory(setupId: String) = mutate { data ->
        data.copy(cleaningTasks = data.cleaningTasks.map {
            if (it.setupId == setupId) it.copy(lastCompletedDate = "", updatedAt = Ids.nowIso()) else it
        })
    }

    // ---- Needed items ---------------------------------------------------------

    suspend fun createNeededItem(item: NeededItem): String {
        val id = if (item.id.isBlank()) Ids.newId("need") else item.id
        val now = Ids.nowIso()
        mutate { data ->
            data.copy(neededItems = data.neededItems + item.copy(id = id, createdAt = now, updatedAt = now))
        }
        return id
    }

    suspend fun updateNeededItem(item: NeededItem) = mutate { data ->
        data.copy(neededItems = data.neededItems.map {
            if (it.id == item.id) item.copy(updatedAt = Ids.nowIso()) else it
        })
    }

    suspend fun setNeededAcquired(itemId: String, acquired: Boolean) = mutate { data ->
        data.copy(neededItems = data.neededItems.map {
            if (it.id == itemId) it.copy(acquired = acquired, updatedAt = Ids.nowIso()) else it
        })
    }

    suspend fun deleteNeededItem(itemId: String) = mutate { data ->
        data.copy(neededItems = data.neededItems.filter { it.id != itemId })
    }

    suspend fun clearAcquiredNeededItems(setupId: String) = mutate { data ->
        data.copy(neededItems = data.neededItems.filterNot { it.setupId == setupId && it.acquired })
    }

    /** Converts an acquired needed item into a new SetupItem. Optionally removes the needed item. */
    suspend fun convertNeededToItem(
        neededId: String,
        itemType: SetupItemType,
        zoneId: String?,
        removeNeeded: Boolean
    ): String? {
        val newItemId = Ids.newId("item")
        var ok = false
        mutate { data ->
            val needed = data.neededItems.firstOrNull { it.id == neededId } ?: return@mutate data
            ok = true
            val now = Ids.nowIso()
            val status = if (zoneId != null) SetupItemStatus.Placed else SetupItemStatus.Unplaced
            val item = SetupItem(
                id = newItemId, setupId = needed.setupId, zoneId = zoneId,
                name = needed.title, itemType = itemType, status = status,
                note = needed.note, createdAt = now, updatedAt = now
            )
            val neededList = if (removeNeeded) data.neededItems.filter { it.id != neededId } else data.neededItems
            data.copy(items = data.items + item, neededItems = neededList)
        }
        return if (ok) newItemId else null
    }

    // ---- Global reset ---------------------------------------------------------

    suspend fun deleteArchivedSetups() = mutate { data ->
        val archivedIds = data.setups.filter { it.archived }.map { it.id }.toSet()
        data.copy(
            setups = data.setups.filterNot { it.id in archivedIds },
            zones = data.zones.filterNot { it.setupId in archivedIds },
            items = data.items.filterNot { it.setupId in archivedIds },
            cables = data.cables.filterNot { it.setupId in archivedIds },
            cleaningTasks = data.cleaningTasks.filterNot { it.setupId in archivedIds },
            neededItems = data.neededItems.filterNot { it.setupId in archivedIds }
        )
    }

    suspend fun resetAllData() {
        context.dataStore.edit { it.clear() }
    }
}
