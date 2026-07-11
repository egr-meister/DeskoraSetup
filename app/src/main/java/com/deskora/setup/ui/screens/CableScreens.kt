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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import com.deskora.setup.model.CableRoute
import com.deskora.setup.model.CableType
import com.deskora.setup.ui.Disclaimers
import com.deskora.setup.ui.components.ConfirmDialog
import com.deskora.setup.ui.components.DeskoraDropdown
import com.deskora.setup.ui.components.DeskoraTextField
import com.deskora.setup.ui.components.DeskoraTopBar
import com.deskora.setup.ui.components.DisclaimerBox
import com.deskora.setup.ui.components.EmptyState
import com.deskora.setup.ui.nav.Routes
import com.deskora.setup.ui.theme.DeskoraColors
import com.deskora.setup.ui.vm.DeskoraViewModel
import com.deskora.setup.util.Validation

@Composable
fun CableListScreen(vm: DeskoraViewModel, data: AppData, navController: NavController) {
    val setup = data.activeSetup
    Scaffold(
        topBar = { DeskoraTopBar("Cables", onBack = { navController.popBackStack() }) },
        floatingActionButton = {
            if (setup != null) FloatingActionButton(onClick = { navController.navigate(Routes.cableForm()) }) {
                Icon(Icons.Filled.Add, contentDescription = "Add cable")
            }
        }
    ) { pad ->
        if (setup == null) { EmptyState("No active setup", "Create a setup first.", Modifier.padding(pad)); return@Scaffold }
        val cables = data.cablesFor(setup.id)
        val items = data.itemsFor(setup.id)
        if (cables.isEmpty()) { EmptyState("No cables yet", "Add a route to connect two items.", Modifier.padding(pad)); return@Scaffold }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(pad).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(cables.size, key = { cables[it].id }) { idx ->
                val cable = cables[idx]
                val start = items.firstOrNull { it.id == cable.startItemId }
                val end = items.firstOrNull { it.id == cable.endItemId }
                val missing = start == null || end == null
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Routes.cableForm(cable.id)) }.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(cable.name.ifBlank { "Cable" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text(cable.cableType.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        val endpointText = "${start?.name ?: "Missing endpoint"} → ${end?.name ?: "Missing endpoint"}"
                        Text(endpointText, style = MaterialTheme.typography.bodySmall, color = if (missing) DeskoraColors.ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant)
                        Row {
                            if (cable.hidden) Text("Hidden", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 8.dp))
                            if (cable.note.isNotBlank()) Text("note", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(70.dp)) }
        }
    }
}

@Composable
fun CableFormScreen(vm: DeskoraViewModel, data: AppData, navController: NavController, cableId: String?) {
    val setup = data.activeSetup
    val existing = remember(cableId, data) { data.cables.firstOrNull { it.id == cableId } }
    val items = remember(setup, data) { setup?.let { data.itemsFor(it.id) } ?: emptyList() }

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var type by remember { mutableStateOf(existing?.cableType ?: CableType.Power) }
    var startId by remember { mutableStateOf(existing?.startItemId) }
    var endId by remember { mutableStateOf(existing?.endItemId) }
    var label by remember { mutableStateOf(existing?.label ?: "") }
    var hidden by remember { mutableStateOf(existing?.hidden ?: false) }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var showDelete by remember { mutableStateOf(false) }

    Scaffold(topBar = { DeskoraTopBar(if (existing == null) "Add Cable" else "Edit Cable", onBack = { navController.popBackStack() }) }) { pad ->
        if (setup == null) { EmptyState("No active setup", "Create a setup first.", Modifier.padding(pad)); return@Scaffold }
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            DisclaimerBox(Disclaimers.CABLE)
            DeskoraTextField(name, { name = Validation.trimTo(it, Validation.NAME_MAX) }, "Cable name (optional)")
            DeskoraDropdown("Cable type", CableType.entries, type, { it.label }, { type = it })

            val itemOptions = listOf<String?>(null) + items.map { it.id }
            DeskoraDropdown("Start item", itemOptions, startId, { id -> items.firstOrNull { it.id == id }?.name ?: "None" }, { startId = it })
            DeskoraDropdown("End item", itemOptions, endId, { id -> items.firstOrNull { it.id == id }?.name ?: "None" }, { endId = it })
            DeskoraTextField(label, { label = Validation.trimTo(it, Validation.NAME_MAX) }, "Label (optional)")
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Hidden on map", modifier = Modifier.weight(1f))
                Switch(checked = hidden, onCheckedChange = { hidden = it })
            }
            DeskoraTextField(note, { note = Validation.trimTo(it, Validation.CABLE_NOTE_MAX) }, "Note (optional)", singleLine = false, minLines = 2)

            Button(
                onClick = {
                    if (existing == null) {
                        val cable = CableRoute(setupId = setup.id, name = name.trim(), cableType = type, startItemId = startId, endItemId = endId, label = label, hidden = hidden, note = note, colorKey = "cable_${type.name.lowercase()}")
                        vm.createCable(cable) { navController.popBackStack() }
                    } else {
                        vm.updateCable(existing.copy(name = name.trim(), cableType = type, startItemId = startId, endItemId = endId, label = label, hidden = hidden, note = note))
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save Cable") }

            if (existing != null) {
                TextButton(onClick = { showDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete Cable", color = DeskoraColors.ErrorRed)
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }

    if (showDelete && existing != null) {
        ConfirmDialog(
            title = "Delete cable?",
            message = "This removes the cable route.",
            confirmLabel = "Delete", destructive = true,
            onConfirm = { vm.deleteCable(existing.id); showDelete = false; navController.popBackStack() },
            onDismiss = { showDelete = false }
        )
    }
}
