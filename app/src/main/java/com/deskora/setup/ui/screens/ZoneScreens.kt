package com.deskora.setup.ui.screens

import androidx.compose.foundation.background
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
import com.deskora.setup.model.DeskZone
import com.deskora.setup.model.DeskZoneType
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
fun ZoneManagementScreen(vm: DeskoraViewModel, data: AppData, navController: NavController) {
    val setup = data.activeSetup
    var reassignZone by remember { mutableStateOf<DeskZone?>(null) }

    Scaffold(
        topBar = { DeskoraTopBar("Zones", onBack = { navController.popBackStack() }) },
        floatingActionButton = {
            if (setup != null) FloatingActionButton(onClick = { navController.navigate(Routes.zoneForm()) }) {
                Icon(Icons.Filled.Add, contentDescription = "Add zone")
            }
        }
    ) { pad ->
        if (setup == null) { EmptyState("No active setup", "Create a setup first.", Modifier.padding(pad)); return@Scaffold }
        val zones = data.zonesFor(setup.id)
        if (zones.isEmpty()) { EmptyState("No zones yet", "Add a zone to organize the desk.", Modifier.padding(pad)); return@Scaffold }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(pad).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(zones.size, key = { zones[it].id }) { idx ->
                val zone = zones[idx]
                val count = data.items.count { it.zoneId == zone.id }
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier.height(16.dp).fillMaxWidth(0.05f)
                                    .padding(end = 4.dp)
                                    .background(Palette.zoneColor(zone.colorKey))
                            )
                            Text(zone.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text("$count items", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            "${zone.zoneType.label} · x${(zone.normalizedX * 100).toInt()} y${(zone.normalizedY * 100).toInt()} · " +
                                "${(zone.normalizedWidth * 100).toInt()}×${(zone.normalizedHeight * 100).toInt()}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row {
                            TextButton(onClick = { navController.navigate(Routes.zoneForm(zone.id)) }) { Text("Edit") }
                            TextButton(onClick = { vm.reorderZone(zone.id, -1) }) { Text("Up") }
                            TextButton(onClick = { vm.reorderZone(zone.id, 1) }) { Text("Down") }
                            TextButton(onClick = {
                                if (count == 0) vm.deleteZoneIfEmpty(zone.id) else reassignZone = zone
                            }) { Text("Delete", color = DeskoraColors.ErrorRed) }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(70.dp)) }
        }
    }

    val currentReassign = reassignZone
    if (currentReassign != null) {
        val zone = currentReassign
        ConfirmDialog(
            title = "Zone has items",
            message = "This zone contains items. They will be moved to the unplaced tray, then the zone will be deleted.",
            confirmLabel = "Move & Delete", destructive = true,
            onConfirm = { vm.reassignAndDeleteZone(zone.id, null); reassignZone = null },
            onDismiss = { reassignZone = null }
        )
    }
}

@Composable
fun ZoneFormScreen(vm: DeskoraViewModel, data: AppData, navController: NavController, zoneId: String?) {
    val setup = data.activeSetup
    val existing = remember(zoneId, data) { data.zones.firstOrNull { it.id == zoneId } }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var type by remember { mutableStateOf(existing?.zoneType ?: DeskZoneType.Main) }
    var colorKey by remember { mutableStateOf(existing?.colorKey ?: "zone") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var nameError by remember { mutableStateOf(false) }
    val step = data.settings.mapControlStep.step

    Scaffold(topBar = { DeskoraTopBar(if (existing == null) "Add Zone" else "Edit Zone", onBack = { navController.popBackStack() }) }) { pad ->
        if (setup == null) { EmptyState("No active setup", "Create a setup first.", Modifier.padding(pad)); return@Scaffold }
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            DeskoraTextField(name, { name = Validation.trimTo(it, Validation.NAME_MAX); nameError = false }, "Zone name (required)", isError = nameError, supportingText = if (nameError) "Name is required" else null)
            DeskoraDropdown("Type", DeskZoneType.entries, type, { it.label }, { type = it })
            ColorKeyPicker("Color", Palette.zoneColorKeys, colorKey, { colorKey = it })

            if (existing != null) {
                Text("Position & size", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { vm.moveZone(existing.id, -step, 0f) }, modifier = Modifier.weight(1f)) { Text("←") }
                    OutlinedButton(onClick = { vm.moveZone(existing.id, step, 0f) }, modifier = Modifier.weight(1f)) { Text("→") }
                    OutlinedButton(onClick = { vm.moveZone(existing.id, 0f, -step) }, modifier = Modifier.weight(1f)) { Text("↑") }
                    OutlinedButton(onClick = { vm.moveZone(existing.id, 0f, step) }, modifier = Modifier.weight(1f)) { Text("↓") }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { vm.resizeZone(existing.id, -step, 0f) }, modifier = Modifier.weight(1f)) { Text("W-") }
                    OutlinedButton(onClick = { vm.resizeZone(existing.id, step, 0f) }, modifier = Modifier.weight(1f)) { Text("W+") }
                    OutlinedButton(onClick = { vm.resizeZone(existing.id, 0f, -step) }, modifier = Modifier.weight(1f)) { Text("H-") }
                    OutlinedButton(onClick = { vm.resizeZone(existing.id, 0f, step) }, modifier = Modifier.weight(1f)) { Text("H+") }
                }
            } else {
                Text("Position and size can be adjusted after the zone is created.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            DeskoraTextField(note, { note = Validation.trimTo(it, Validation.ITEM_NOTE_MAX) }, "Note (optional)", singleLine = false, minLines = 2)

            Button(
                onClick = {
                    if (Validation.isBlank(name)) { nameError = true; return@Button }
                    if (existing == null) {
                        val zone = DeskZone(setupId = setup.id, name = name.trim(), zoneType = type, colorKey = colorKey, note = note,
                            normalizedX = 0.1f, normalizedY = 0.1f, normalizedWidth = 0.3f, normalizedHeight = 0.3f)
                        vm.createZone(zone) { navController.popBackStack() }
                    } else {
                        vm.updateZone(existing.copy(name = name.trim(), zoneType = type, colorKey = colorKey, note = note))
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save Zone") }
            Spacer(Modifier.height(30.dp))
        }
    }
}
