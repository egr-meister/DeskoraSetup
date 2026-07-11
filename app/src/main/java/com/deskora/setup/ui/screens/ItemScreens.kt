package com.deskora.setup.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.deskora.setup.model.AppData
import com.deskora.setup.model.ItemSizePreset
import com.deskora.setup.model.SetupItem
import com.deskora.setup.model.SetupItemStatus
import com.deskora.setup.model.SetupItemType
import com.deskora.setup.ui.components.ColorKeyPicker
import com.deskora.setup.ui.components.ConfirmDialog
import com.deskora.setup.ui.components.DeskoraDropdown
import com.deskora.setup.ui.components.DeskoraTextField
import com.deskora.setup.ui.components.DeskoraTopBar
import com.deskora.setup.ui.components.EmptyState
import com.deskora.setup.ui.nav.Routes
import com.deskora.setup.ui.theme.DeskoraColors
import com.deskora.setup.ui.theme.Palette
import com.deskora.setup.ui.vm.DeskoraViewModel
import com.deskora.setup.util.Validation

@Composable
fun ItemListScreen(vm: DeskoraViewModel, data: AppData, navController: NavController) {
    val setup = data.activeSetup
    Scaffold(
        topBar = { DeskoraTopBar("Items") },
        floatingActionButton = {
            if (setup != null) FloatingActionButton(onClick = { navController.navigate(Routes.itemForm()) }) {
                Icon(Icons.Filled.Add, contentDescription = "Add item")
            }
        }
    ) { pad ->
        if (setup == null) {
            EmptyState("No active setup", "Create a setup to add items.", Modifier.padding(pad)); return@Scaffold
        }
        val items = data.itemsFor(setup.id)
        val zones = data.zonesFor(setup.id)
        val groups = listOf(
            "Devices" to items.filter { it.itemType.isDevice && it.status != SetupItemStatus.Archived && it.status != SetupItemStatus.Unplaced && it.status != SetupItemStatus.Planned },
            "Accessories" to items.filter { !it.itemType.isDevice && it.status != SetupItemStatus.Archived && it.status != SetupItemStatus.Unplaced && it.status != SetupItemStatus.Planned },
            "Unplaced" to items.filter { it.status == SetupItemStatus.Unplaced },
            "Planned" to items.filter { it.status == SetupItemStatus.Planned },
            "Archived" to items.filter { it.status == SetupItemStatus.Archived }
        )
        if (items.isEmpty()) {
            EmptyState("No items yet", "Add a device or accessory to your desk.", Modifier.padding(pad)); return@Scaffold
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(pad).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            groups.forEach { (title, list) ->
                if (list.isNotEmpty()) {
                    item(key = "h_$title") {
                        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp))
                    }
                    items(list.size, key = { list[it].id }) { idx ->
                        val item = list[idx]
                        ItemRow(item, zoneName = zones.firstOrNull { it.id == item.zoneId }?.name,
                            cableCount = data.cables.count { it.startItemId == item.id || it.endItemId == item.id },
                            onOpen = { navController.navigate(Routes.itemDetail(item.id)) })
                    }
                }
            }
            item { Spacer(Modifier.height(70.dp)) }
        }
    }
}

@Composable
private fun ItemRow(item: SetupItem, zoneName: String?, cableCount: Int, onOpen: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.fillMaxWidth().clickable { onOpen() }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name.ifBlank { "Missing Item" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "${item.itemType.label}${if (item.itemType == SetupItemType.Other && item.customTypeName.isNotBlank()) " (${item.customTypeName})" else ""} · ${zoneName ?: "Unplaced"} · ${item.status.label}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val extras = buildString {
                    if (cableCount > 0) append("$cableCount cable${if (cableCount == 1) "" else "s"}")
                    if (item.note.isNotBlank()) { if (isNotEmpty()) append(" · "); append("note") }
                }
                if (extras.isNotEmpty()) Text(extras, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ItemFormScreen(vm: DeskoraViewModel, data: AppData, navController: NavController, itemId: String?) {
    val setup = data.activeSetup
    val existing = remember(itemId, data) { data.items.firstOrNull { it.id == itemId } }
    val zones = remember(setup, data) { setup?.let { data.zonesFor(it.id) } ?: emptyList() }

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var type by remember { mutableStateOf(existing?.itemType ?: SetupItemType.Monitor) }
    var customType by remember { mutableStateOf(existing?.customTypeName ?: "") }
    var zoneId by remember { mutableStateOf(existing?.zoneId) }
    var status by remember { mutableStateOf(existing?.status ?: SetupItemStatus.Placed) }
    var sizePreset by remember { mutableStateOf(data.settings.defaultItemSize) }
    var rotation by remember { mutableStateOf(existing?.rotationDegrees ?: 0) }
    var colorKey by remember { mutableStateOf(existing?.colorKey ?: Palette.defaultColorKey(SetupItemType.Monitor)) }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var nameError by remember { mutableStateOf(false) }

    Scaffold(topBar = { DeskoraTopBar(if (existing == null) "Add Item" else "Edit Item", onBack = { navController.popBackStack() }) }) { pad ->
        if (setup == null) { EmptyState("No active setup", "Create a setup first.", Modifier.padding(pad)); return@Scaffold }
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            DeskoraTextField(name, { name = Validation.trimTo(it, Validation.NAME_MAX); nameError = false }, "Item name (required)", isError = nameError, supportingText = if (nameError) "Name is required" else null)
            DeskoraDropdown("Type (required)", SetupItemType.entries, type, { it.label }, { type = it; colorKey = Palette.defaultColorKey(it) })
            if (type == SetupItemType.Other) {
                DeskoraTextField(customType, { customType = Validation.trimTo(it, Validation.NAME_MAX) }, "Custom type label (optional)")
            }
            val zoneOptions = listOf<String?>(null) + zones.map { it.id }
            DeskoraDropdown("Zone (optional)", zoneOptions, zoneId, { id -> if (id == null) "Unplaced tray" else zones.firstOrNull { it.id == id }?.name ?: "Missing Zone" }, { zoneId = it })
            DeskoraDropdown("Status", SetupItemStatus.entries, status, { it.label }, { status = it })
            if (existing == null) {
                DeskoraDropdown("Visual size", ItemSizePreset.entries, sizePreset, { it.label }, { sizePreset = it })
            }
            DeskoraDropdown("Rotation", listOf(0, 90, 180, 270), rotation, { "$it°" }, { rotation = it })
            ColorKeyPicker("Color", Palette.itemColorKeys, colorKey, { colorKey = it })
            DeskoraTextField(note, { note = Validation.trimTo(it, Validation.ITEM_NOTE_MAX) }, "Note (optional)", singleLine = false, minLines = 3)

            Button(
                onClick = {
                    if (Validation.isBlank(name)) { nameError = true; return@Button }
                    val effectiveStatus = if (zoneId == null && status == SetupItemStatus.Placed) SetupItemStatus.Unplaced else status
                    if (existing == null) {
                        val frac = sizePreset.fraction
                        val item = SetupItem(
                            setupId = setup.id, zoneId = zoneId, name = name.trim(), itemType = type,
                            customTypeName = customType, normalizedX = 0.42f, normalizedY = 0.42f,
                            normalizedWidth = frac, normalizedHeight = frac * 0.7f, rotationDegrees = rotation,
                            colorKey = colorKey, status = effectiveStatus, note = note
                        )
                        vm.createItem(item) { navController.popBackStack() }
                    } else {
                        vm.updateItem(existing.copy(name = name.trim(), itemType = type, customTypeName = customType, zoneId = zoneId, status = effectiveStatus, rotationDegrees = rotation, colorKey = colorKey, note = note))
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save Item") }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
fun ItemDetailScreen(vm: DeskoraViewModel, data: AppData, navController: NavController, itemId: String?) {
    val item = remember(itemId, data) { data.items.firstOrNull { it.id == itemId } }
    var showDelete by remember { mutableStateOf(false) }

    Scaffold(topBar = { DeskoraTopBar(item?.name ?: "Item", onBack = { navController.popBackStack() }) }) { pad ->
        if (item == null) {
            Column(Modifier.padding(pad)) {
                EmptyState("Item not found", "It may have been deleted.")
                OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.padding(16.dp)) { Text("Back") }
            }
            return@Scaffold
        }
        val zone = data.zones.firstOrNull { it.id == item.zoneId }
        val setup = data.setups.firstOrNull { it.id == item.setupId }
        val cables = data.cables.filter { it.startItemId == item.id || it.endItemId == item.id }
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DetailRow("Type", item.itemType.label + if (item.customTypeName.isNotBlank()) " (${item.customTypeName})" else "")
            DetailRow("Setup", setup?.name ?: "Deleted Setup")
            DetailRow("Zone", zone?.name ?: "Unplaced")
            DetailRow("Status", item.status.label)
            DetailRow("Size on map", "${(item.normalizedWidth * 100).toInt()}% × ${(item.normalizedHeight * 100).toInt()}%")
            DetailRow("Rotation", "${item.rotationDegrees}°")
            DetailRow("Connected cables", cables.size.toString())
            if (item.note.isNotBlank()) DetailRow("Note", item.note)
            DetailRow("Created", item.createdAt.take(10).ifBlank { "Date unavailable" })
            DetailRow("Updated", item.updatedAt.take(10).ifBlank { "Date unavailable" })

            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { navController.navigate(Routes.itemForm(item.id)) }, modifier = Modifier.weight(1f)) { Text("Edit") }
                OutlinedButton(onClick = { navController.navigate(Routes.EDITOR) }, modifier = Modifier.weight(1f)) { Text("Placement") }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { vm.duplicateItem(item.id) }, modifier = Modifier.weight(1f)) { Text("Duplicate") }
                OutlinedButton(onClick = { vm.setItemStatus(item.id, SetupItemStatus.Unplaced) }, modifier = Modifier.weight(1f)) { Text("To Tray") }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (item.status == SetupItemStatus.Archived) {
                    OutlinedButton(onClick = { vm.setItemStatus(item.id, SetupItemStatus.Unplaced) }, modifier = Modifier.weight(1f)) { Text("Restore") }
                } else {
                    OutlinedButton(onClick = { vm.setItemStatus(item.id, SetupItemStatus.Archived) }, modifier = Modifier.weight(1f)) { Text("Archive") }
                }
                TextButton(onClick = { showDelete = true }, modifier = Modifier.weight(1f)) { Text("Delete", color = DeskoraColors.ErrorRed) }
            }
            Spacer(Modifier.height(30.dp))
        }
    }

    if (showDelete && item != null) {
        ConfirmDialog(
            title = "Delete item?",
            message = "This removes the item. Any connected cables will be kept and marked with a missing endpoint.",
            confirmLabel = "Delete", destructive = true,
            onConfirm = { vm.deleteItem(item.id); showDelete = false; navController.popBackStack() },
            onDismiss = { showDelete = false }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
