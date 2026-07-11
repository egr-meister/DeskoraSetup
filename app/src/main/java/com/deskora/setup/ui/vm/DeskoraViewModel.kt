package com.deskora.setup.ui.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.deskora.setup.data.DeskoraRepository
import com.deskora.setup.model.AppData
import com.deskora.setup.model.AppSettings
import com.deskora.setup.model.CableRoute
import com.deskora.setup.model.CleaningTask
import com.deskora.setup.model.DeskSetup
import com.deskora.setup.model.DeskShape
import com.deskora.setup.model.DeskZone
import com.deskora.setup.model.NeededItem
import com.deskora.setup.model.SetupItem
import com.deskora.setup.model.SetupItemStatus
import com.deskora.setup.model.SetupItemType
import com.deskora.setup.model.SetupType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Single application ViewModel exposing observable app state as a StateFlow and
 * offering guarded operations that delegate to the repository. Editing map
 * geometry is done through stable, non-drag controls.
 */
class DeskoraViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = DeskoraRepository(app.applicationContext)

    val appData: StateFlow<AppData> = repository.appData.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppData()
    )

    // ---- lookups (null-safe) --------------------------------------------------

    fun currentData(): AppData = appData.value
    fun setupById(id: String?): DeskSetup? = appData.value.setups.firstOrNull { it.id == id }
    fun zoneById(id: String?): DeskZone? = appData.value.zones.firstOrNull { it.id == id }
    fun itemById(id: String?): SetupItem? = appData.value.items.firstOrNull { it.id == id }
    fun cableById(id: String?): CableRoute? = appData.value.cables.firstOrNull { it.id == id }
    fun taskById(id: String?): CleaningTask? = appData.value.cleaningTasks.firstOrNull { it.id == id }
    fun neededById(id: String?): NeededItem? = appData.value.neededItems.firstOrNull { it.id == id }
    fun templateById(id: String?) = appData.value.templates.firstOrNull { it.id == id }

    // ---- settings -------------------------------------------------------------

    fun updateSettings(transform: (AppSettings) -> AppSettings) = launch { repository.updateSettings(transform) }
    fun completeOnboarding() = launch { repository.completeOnboarding() }
    fun setActiveSetup(id: String?) = launch { repository.setActiveSetup(id) }

    // ---- setups ---------------------------------------------------------------

    fun createSetup(
        name: String, type: SetupType, shape: DeskShape,
        widthCm: Double?, depthCm: Double?, description: String,
        onCreated: (String) -> Unit = {}
    ) = launch {
        val id = repository.createSetup(name, type, shape, widthCm, depthCm, description)
        onCreated(id)
    }

    fun updateSetup(setup: DeskSetup) = launch { repository.updateSetup(setup) }
    fun duplicateSetup(id: String, onDone: (String?) -> Unit = {}) = launch { onDone(repository.duplicateSetup(id)) }
    fun archiveSetup(id: String, archived: Boolean) = launch { repository.archiveSetup(id, archived) }
    fun deleteSetup(id: String) = launch { repository.deleteSetup(id) }

    // ---- templates ------------------------------------------------------------

    fun applyTemplate(templateId: String, name: String, onDone: (String?) -> Unit = {}) =
        launch { onDone(repository.applyTemplate(templateId, name)) }

    fun saveAsTemplate(setupId: String, name: String, onDone: (String?) -> Unit = {}) =
        launch { onDone(repository.saveCurrentSetupAsTemplate(setupId, name)) }

    fun duplicateTemplate(id: String, onDone: (String?) -> Unit = {}) = launch { onDone(repository.duplicateTemplate(id)) }
    fun deleteTemplate(id: String) = launch { repository.deleteTemplate(id) }
    fun restoreBuiltInTemplates() = launch { repository.restoreBuiltInTemplates() }

    // ---- zones ----------------------------------------------------------------

    fun createZone(zone: DeskZone, onDone: (String) -> Unit = {}) = launch { onDone(repository.createZone(zone)) }
    fun updateZone(zone: DeskZone) = launch { repository.updateZone(zone) }
    fun reorderZone(id: String, direction: Int) = launch { repository.reorderZone(id, direction) }
    fun deleteZoneIfEmpty(id: String, onDone: (Boolean) -> Unit = {}) = launch { onDone(repository.deleteZoneIfEmpty(id)) }
    fun reassignAndDeleteZone(id: String, target: String?) = launch { repository.reassignAndDeleteZone(id, target) }

    fun moveZone(id: String, dx: Float, dy: Float) = launch {
        val zone = zoneById(id) ?: return@launch
        repository.updateZone(zone.copy(normalizedX = zone.normalizedX + dx, normalizedY = zone.normalizedY + dy))
    }

    fun resizeZone(id: String, dw: Float, dh: Float) = launch {
        val zone = zoneById(id) ?: return@launch
        repository.updateZone(zone.copy(normalizedWidth = zone.normalizedWidth + dw, normalizedHeight = zone.normalizedHeight + dh))
    }

    // ---- items ----------------------------------------------------------------

    fun createItem(item: SetupItem, onDone: (String) -> Unit = {}) = launch { onDone(repository.createItem(item)) }
    fun updateItem(item: SetupItem) = launch { repository.updateItem(item) }
    fun duplicateItem(id: String, onDone: (String?) -> Unit = {}) = launch { onDone(repository.duplicateItem(id)) }
    fun moveItemToZone(itemId: String, zoneId: String?) = launch { repository.moveItemToZone(itemId, zoneId) }
    fun setItemStatus(itemId: String, status: SetupItemStatus) = launch { repository.setItemStatus(itemId, status) }
    fun nudgeItem(itemId: String, dx: Float, dy: Float) = launch { repository.nudgeItem(itemId, dx, dy) }
    fun resizeItem(itemId: String, dw: Float, dh: Float) = launch { repository.resizeItem(itemId, dw, dh) }
    fun rotateItem(itemId: String, delta: Int) = launch { repository.rotateItem(itemId, delta) }
    fun deleteItem(itemId: String) = launch { repository.deleteItem(itemId) }

    // ---- cables ---------------------------------------------------------------

    fun createCable(cable: CableRoute, onDone: (String) -> Unit = {}) = launch { onDone(repository.createCable(cable)) }
    fun updateCable(cable: CableRoute) = launch { repository.updateCable(cable) }
    fun setCableHidden(id: String, hidden: Boolean) = launch { repository.setCableHidden(id, hidden) }
    fun deleteCable(id: String) = launch { repository.deleteCable(id) }

    // ---- cleaning tasks -------------------------------------------------------

    fun createTask(task: CleaningTask, onDone: (String) -> Unit = {}) = launch { onDone(repository.createTask(task)) }
    fun updateTask(task: CleaningTask) = launch { repository.updateTask(task) }
    fun completeTask(id: String) = launch { repository.completeTask(id) }
    fun undoTask(id: String) = launch { repository.undoTaskCompletion(id) }
    fun setTaskEnabled(id: String, enabled: Boolean) = launch { repository.setTaskEnabled(id, enabled) }
    fun deleteTask(id: String) = launch { repository.deleteTask(id) }
    fun clearCompletedHistory(setupId: String) = launch { repository.clearCompletedHistory(setupId) }

    // ---- needed items ---------------------------------------------------------

    fun createNeededItem(item: NeededItem, onDone: (String) -> Unit = {}) = launch { onDone(repository.createNeededItem(item)) }
    fun updateNeededItem(item: NeededItem) = launch { repository.updateNeededItem(item) }
    fun setNeededAcquired(id: String, acquired: Boolean) = launch { repository.setNeededAcquired(id, acquired) }
    fun deleteNeededItem(id: String) = launch { repository.deleteNeededItem(id) }
    fun clearAcquiredNeededItems(setupId: String) = launch { repository.clearAcquiredNeededItems(setupId) }
    fun convertNeededToItem(neededId: String, type: SetupItemType, zoneId: String?, removeNeeded: Boolean, onDone: (String?) -> Unit = {}) =
        launch { onDone(repository.convertNeededToItem(neededId, type, zoneId, removeNeeded)) }

    // ---- reset ----------------------------------------------------------------

    fun deleteArchivedSetups() = launch { repository.deleteArchivedSetups() }
    fun resetAllData() = launch { repository.resetAllData() }

    private inline fun launch(crossinline block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                    DeskoraViewModel(app) as T
            }
    }
}
