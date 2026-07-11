package com.deskora.setup.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.deskora.setup.model.AppData
import com.deskora.setup.model.SetupItemStatus
import com.deskora.setup.ui.components.DeskMap
import com.deskora.setup.ui.components.DeskoraDropdown
import com.deskora.setup.ui.components.DeskoraTopBar
import com.deskora.setup.ui.components.EmptyState
import com.deskora.setup.ui.nav.Routes
import com.deskora.setup.ui.vm.DeskoraViewModel

private enum class EditorMode(val label: String) { Select("Select"), Zones("Zones"), Items("Items"), Cables("Cables") }

@Composable
fun DeskMapEditorScreen(vm: DeskoraViewModel, data: AppData, navController: NavController) {
    val setup = data.activeSetup
    var mode by remember { mutableStateOf(EditorMode.Select) }
    var selectedItemId by remember { mutableStateOf<String?>(null) }
    var selectedZoneId by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { DeskoraTopBar("Desk Map Editor", onBack = { navController.popBackStack() }) }) { pad ->
        if (setup == null) {
            EmptyState("No active setup", "Create a setup to edit its map.", Modifier.padding(pad))
            return@Scaffold
        }
        val zones = data.zonesFor(setup.id)
        val items = data.itemsFor(setup.id)
        val cables = data.cablesFor(setup.id)
        val step = data.settings.mapControlStep.step

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Mode selector
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                EditorMode.entries.forEach { m ->
                    FilterChip(
                        selected = mode == m,
                        onClick = { mode = m },
                        label = { Text(m.label) }
                    )
                }
            }

            DeskMap(
                setup = setup,
                zones = zones,
                items = items,
                cables = cables,
                showGrid = data.settings.showDeskGrid,
                showZoneLabels = data.settings.showZoneLabels,
                showCableLabels = data.settings.showCableLabels,
                selectedItemId = selectedItemId,
                selectedZoneId = selectedZoneId,
                onItemTap = { selectedItemId = it; selectedZoneId = null },
                onBackgroundTap = { selectedItemId = null; selectedZoneId = null }
            )

            when (mode) {
                EditorMode.Select, EditorMode.Items -> ItemsPanel(
                    vm = vm, data = data, setupId = setup.id, items = items,
                    selectedItemId = selectedItemId, onSelect = { selectedItemId = it },
                    step = step, navController = navController
                )
                EditorMode.Zones -> ZonesPanel(
                    vm = vm, zones = zones, selectedZoneId = selectedZoneId,
                    onSelect = { selectedZoneId = it }, step = step, navController = navController
                )
                EditorMode.Cables -> CablesPanel(navController)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ItemsPanel(
    vm: DeskoraViewModel,
    data: AppData,
    setupId: String,
    items: List<com.deskora.setup.model.SetupItem>,
    selectedItemId: String?,
    onSelect: (String?) -> Unit,
    step: Float,
    navController: NavController
) {
    val selected = items.firstOrNull { it.id == selectedItemId }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { navController.navigate(Routes.itemForm()) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.height(0.dp))
                Text(" Add Item")
            }
            OutlinedButton(onClick = { navController.navigate(Routes.ITEMS) }, modifier = Modifier.weight(1f)) {
                Text("Item List")
            }
        }

        if (selected == null) {
            Text("Tap an item on the map to select it, or pick one below.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            items.filter { it.status != SetupItemStatus.Archived }.forEach { item ->
                AssistChip(onClick = { onSelect(item.id) }, label = { Text(item.name) })
            }
            return
        }

        Text("Placement: ${selected.name}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

        // Zone assignment
        val zones = data.zonesFor(setupId)
        val zoneOptions = listOf<String?>(null) + zones.map { it.id }
        DeskoraDropdown(
            label = "Zone",
            options = zoneOptions,
            selected = selected.zoneId,
            optionLabel = { id -> if (id == null) "Unplaced tray" else zones.firstOrNull { it.id == id }?.name ?: "Missing Zone" },
            onSelected = { zoneId ->
                if (zoneId == null) vm.setItemStatus(selected.id, SetupItemStatus.Unplaced)
                else vm.moveItemToZone(selected.id, zoneId)
            }
        )

        DirectionalPad(
            onLeft = { vm.nudgeItem(selected.id, -step, 0f) },
            onRight = { vm.nudgeItem(selected.id, step, 0f) },
            onUp = { vm.nudgeItem(selected.id, 0f, -step) },
            onDown = { vm.nudgeItem(selected.id, 0f, step) }
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.resizeItem(selected.id, -step, 0f) }, modifier = Modifier.weight(1f)) { Text("W-") }
            OutlinedButton(onClick = { vm.resizeItem(selected.id, step, 0f) }, modifier = Modifier.weight(1f)) { Text("W+") }
            OutlinedButton(onClick = { vm.resizeItem(selected.id, 0f, -step) }, modifier = Modifier.weight(1f)) { Text("H-") }
            OutlinedButton(onClick = { vm.resizeItem(selected.id, 0f, step) }, modifier = Modifier.weight(1f)) { Text("H+") }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.rotateItem(selected.id, 90) }, modifier = Modifier.weight(1f)) { Text("Rotate 90°") }
            OutlinedButton(onClick = { vm.duplicateItem(selected.id) }, modifier = Modifier.weight(1f)) { Text("Duplicate") }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.setItemStatus(selected.id, SetupItemStatus.Unplaced) }, modifier = Modifier.weight(1f)) { Text("To Tray") }
            OutlinedButton(onClick = { navController.navigate(Routes.itemDetail(selected.id)) }, modifier = Modifier.weight(1f)) { Text("Details") }
        }
        Text("Rotation: ${selected.rotationDegrees}°", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ZonesPanel(
    vm: DeskoraViewModel,
    zones: List<com.deskora.setup.model.DeskZone>,
    selectedZoneId: String?,
    onSelect: (String?) -> Unit,
    step: Float,
    navController: NavController
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { navController.navigate(Routes.zoneForm()) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, contentDescription = null); Text(" Add Zone")
        }
        if (zones.isEmpty()) {
            Text("No zones yet. Add a zone to organize the desk.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return
        }
        DeskoraDropdown(
            label = "Selected zone",
            options = zones.map { it.id },
            selected = selectedZoneId ?: zones.first().id,
            optionLabel = { id -> zones.firstOrNull { it.id == id }?.name ?: "Missing Zone" },
            onSelected = { onSelect(it) }
        )
        val zoneId = selectedZoneId ?: zones.first().id
        DirectionalPad(
            onLeft = { vm.moveZone(zoneId, -step, 0f) },
            onRight = { vm.moveZone(zoneId, step, 0f) },
            onUp = { vm.moveZone(zoneId, 0f, -step) },
            onDown = { vm.moveZone(zoneId, 0f, step) }
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.resizeZone(zoneId, -step, 0f) }, modifier = Modifier.weight(1f)) { Text("W-") }
            OutlinedButton(onClick = { vm.resizeZone(zoneId, step, 0f) }, modifier = Modifier.weight(1f)) { Text("W+") }
            OutlinedButton(onClick = { vm.resizeZone(zoneId, 0f, -step) }, modifier = Modifier.weight(1f)) { Text("H-") }
            OutlinedButton(onClick = { vm.resizeZone(zoneId, 0f, step) }, modifier = Modifier.weight(1f)) { Text("H+") }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.reorderZone(zoneId, -1) }, modifier = Modifier.weight(1f)) { Text("Layer Up") }
            OutlinedButton(onClick = { vm.reorderZone(zoneId, 1) }, modifier = Modifier.weight(1f)) { Text("Layer Down") }
        }
        OutlinedButton(onClick = { navController.navigate(Routes.zoneForm(zoneId)) }, modifier = Modifier.fillMaxWidth()) {
            Text("Edit Zone Details")
        }
        OutlinedButton(onClick = { navController.navigate(Routes.ZONES) }, modifier = Modifier.fillMaxWidth()) {
            Text("Manage All Zones")
        }
    }
}

@Composable
private fun CablesPanel(navController: NavController) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .padding(4.dp)
    ) {
        Button(onClick = { navController.navigate(Routes.cableForm()) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, contentDescription = null); Text(" Add Cable")
        }
        OutlinedButton(onClick = { navController.navigate(Routes.CABLES) }, modifier = Modifier.fillMaxWidth()) {
            Text("Cable List")
        }
        Text(
            "Cable routes render behind items and connect two placed items by their centers.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DirectionalPad(onLeft: () -> Unit, onRight: () -> Unit, onUp: () -> Unit, onDown: () -> Unit) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onUp) { Text("↑ Up") }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            OutlinedButton(onClick = onLeft) { Text("← Left") }
            OutlinedButton(onClick = onRight) { Text("Right →") }
        }
        OutlinedButton(onClick = onDown) { Text("↓ Down") }
    }
}
