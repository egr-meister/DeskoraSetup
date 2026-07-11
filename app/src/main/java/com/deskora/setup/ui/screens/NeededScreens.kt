package com.deskora.setup.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.deskora.setup.model.NeededItem
import com.deskora.setup.model.NeededItemCategory
import com.deskora.setup.model.NeededItemPriority
import com.deskora.setup.model.SetupItemType
import com.deskora.setup.ui.components.ConfirmDialog
import com.deskora.setup.ui.components.DeskoraDropdown
import com.deskora.setup.ui.components.DeskoraTextField
import com.deskora.setup.ui.components.DeskoraTopBar
import com.deskora.setup.ui.components.EmptyState
import com.deskora.setup.ui.nav.Routes
import com.deskora.setup.ui.theme.DeskoraColors
import com.deskora.setup.ui.vm.DeskoraViewModel
import com.deskora.setup.util.Validation

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NeededItemsScreen(vm: DeskoraViewModel, data: AppData, navController: NavController) {
    val setup = data.activeSetup
    var categoryFilter by remember { mutableStateOf<NeededItemCategory?>(null) }
    var highOnly by remember { mutableStateOf(false) }
    var convertItem by remember { mutableStateOf<NeededItem?>(null) }

    Scaffold(
        topBar = { DeskoraTopBar("Needed Items") },
        floatingActionButton = {
            if (setup != null) FloatingActionButton(onClick = { navController.navigate(Routes.neededForm()) }) {
                Icon(Icons.Filled.Add, contentDescription = "Add needed item")
            }
        }
    ) { pad ->
        if (setup == null) { EmptyState("No active setup", "Create a setup first.", Modifier.padding(pad)); return@Scaffold }
        val all = data.neededFor(setup.id)
        val filtered = all.filter { (categoryFilter == null || it.category == categoryFilter) && (!highOnly || it.priority == NeededItemPriority.High) }
        val needed = filtered.filter { !it.acquired }
        val acquired = filtered.filter { it.acquired }

        Column(Modifier.fillMaxSize().padding(pad)) {
            FlowRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = categoryFilter == null, onClick = { categoryFilter = null }, label = { Text("All") })
                NeededItemCategory.entries.forEach { c ->
                    FilterChip(selected = categoryFilter == c, onClick = { categoryFilter = c }, label = { Text(c.label) })
                }
                FilterChip(selected = highOnly, onClick = { highOnly = !highOnly }, label = { Text("High priority") })
            }
            if (all.isEmpty()) {
                EmptyState("No needed items yet", "Keep a personal list of items you may want.")
                return@Column
            }
            LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (needed.isNotEmpty()) {
                    item { GroupHeader("Needed") }
                    items(needed.size, key = { needed[it].id }) { idx ->
                        NeededRow(needed[idx], vm, onOpen = { navController.navigate(Routes.neededForm(needed[idx].id)) }, onConvert = null)
                    }
                }
                if (acquired.isNotEmpty()) {
                    item { GroupHeader("Acquired") }
                    items(acquired.size, key = { acquired[it].id }) { idx ->
                        NeededRow(acquired[idx], vm, onOpen = { navController.navigate(Routes.neededForm(acquired[idx].id)) }, onConvert = { convertItem = acquired[idx] })
                    }
                }
                item { Spacer(Modifier.height(70.dp)) }
            }
        }
    }

    val toConvert = convertItem
    if (toConvert != null) {
        ConvertDialog(
            needed = toConvert,
            zones = data.zonesFor(toConvert.setupId),
            onDismiss = { convertItem = null },
            onConfirm = { type, zoneId, remove ->
                vm.convertNeededToItem(toConvert.id, type, zoneId, remove) { }
                convertItem = null
            }
        )
    }
}

@Composable
private fun GroupHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun NeededRow(item: NeededItem, vm: DeskoraViewModel, onOpen: () -> Unit, onConvert: (() -> Unit)?) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxWidth().clickable { onOpen() }.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.title.ifBlank { "Item" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (item.priority == NeededItemPriority.High) Text("High", style = MaterialTheme.typography.labelMedium, color = DeskoraColors.HighPriority)
            }
            val qty = if (item.quantityLabel.isNotBlank()) " · ${item.quantityLabel}" else ""
            Text("${item.category.label}$qty", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row {
                if (item.acquired) {
                    TextButton(onClick = { vm.setNeededAcquired(item.id, false) }) { Text("Return to Needed") }
                    onConvert?.let { TextButton(onClick = it) { Text("Add to Desk") } }
                } else {
                    TextButton(onClick = { vm.setNeededAcquired(item.id, true) }) { Text("Mark Acquired") }
                }
                TextButton(onClick = { vm.deleteNeededItem(item.id) }) { Text("Delete", color = DeskoraColors.ErrorRed) }
            }
        }
    }
}

@Composable
private fun ConvertDialog(
    needed: NeededItem,
    zones: List<com.deskora.setup.model.DeskZone>,
    onDismiss: () -> Unit,
    onConfirm: (SetupItemType, String?, Boolean) -> Unit
) {
    var type by remember { mutableStateOf(SetupItemType.Other) }
    var zoneId by remember { mutableStateOf<String?>(null) }
    var removeNeeded by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Desk") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Create a setup item from \"${needed.title}\".", style = MaterialTheme.typography.bodyMedium)
                DeskoraDropdown("Item type", SetupItemType.entries, type, { it.label }, { type = it })
                val zoneOptions = listOf<String?>(null) + zones.map { it.id }
                DeskoraDropdown("Zone", zoneOptions, zoneId, { id -> if (id == null) "Unplaced tray" else zones.firstOrNull { it.id == id }?.name ?: "Missing Zone" }, { zoneId = it })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(checked = removeNeeded, onCheckedChange = { removeNeeded = it })
                    Text("Remove from needed list")
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(type, zoneId, removeNeeded) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun NeededFormScreen(vm: DeskoraViewModel, data: AppData, navController: NavController, neededId: String?) {
    val setup = data.activeSetup
    val existing = remember(neededId, data) { data.neededItems.firstOrNull { it.id == neededId } }
    val zones = remember(setup, data) { setup?.let { data.zonesFor(it.id) } ?: emptyList() }
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: NeededItemCategory.Device) }
    var quantity by remember { mutableStateOf(existing?.quantityLabel ?: "") }
    var priority by remember { mutableStateOf(existing?.priority ?: NeededItemPriority.Normal) }
    var zoneId by remember { mutableStateOf(existing?.plannedZoneId) }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var titleError by remember { mutableStateOf(false) }

    Scaffold(topBar = { DeskoraTopBar(if (existing == null) "Add Needed Item" else "Edit Needed Item", onBack = { navController.popBackStack() }) }) { pad ->
        if (setup == null) { EmptyState("No active setup", "Create a setup first.", Modifier.padding(pad)); return@Scaffold }
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            DeskoraTextField(title, { title = Validation.trimTo(it, Validation.NAME_MAX); titleError = false }, "Title (required)", isError = titleError, supportingText = if (titleError) "Title is required" else null)
            DeskoraDropdown("Category", NeededItemCategory.entries, category, { it.label }, { category = it })
            DeskoraTextField(quantity, { quantity = Validation.trimTo(it, 40) }, "Quantity (optional free text)")
            DeskoraDropdown("Priority", NeededItemPriority.entries, priority, { it.label }, { priority = it })
            val zoneOptions = listOf<String?>(null) + zones.map { it.id }
            DeskoraDropdown("Planned zone (optional)", zoneOptions, zoneId, { id -> if (id == null) "None" else zones.firstOrNull { it.id == id }?.name ?: "Missing Zone" }, { zoneId = it })
            DeskoraTextField(note, { note = Validation.trimTo(it, Validation.NEEDED_NOTE_MAX) }, "Note (optional)", singleLine = false, minLines = 2)

            Button(
                onClick = {
                    if (Validation.isBlank(title)) { titleError = true; return@Button }
                    if (existing == null) {
                        vm.createNeededItem(NeededItem(setupId = setup.id, title = title.trim(), category = category, quantityLabel = quantity, priority = priority, plannedZoneId = zoneId, note = note)) { navController.popBackStack() }
                    } else {
                        vm.updateNeededItem(existing.copy(title = title.trim(), category = category, quantityLabel = quantity, priority = priority, plannedZoneId = zoneId, note = note))
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save") }
            Spacer(Modifier.height(30.dp))
        }
    }
}
