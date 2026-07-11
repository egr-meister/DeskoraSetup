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
import androidx.compose.foundation.lazy.items
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
import com.deskora.setup.model.DeskSetup
import com.deskora.setup.model.DeskShape
import com.deskora.setup.model.SetupItemStatus
import com.deskora.setup.model.SetupType
import com.deskora.setup.ui.components.ConfirmDialog
import com.deskora.setup.ui.components.DeskoraDropdown
import com.deskora.setup.ui.components.DeskoraTextField
import com.deskora.setup.ui.components.DeskoraTopBar
import com.deskora.setup.ui.components.EmptyState
import com.deskora.setup.ui.nav.Routes
import com.deskora.setup.ui.theme.DeskoraColors
import com.deskora.setup.ui.vm.DeskoraViewModel
import com.deskora.setup.util.Validation

@Composable
fun SetupListScreen(vm: DeskoraViewModel, data: AppData, navController: NavController) {
    var pendingDelete by remember { mutableStateOf<DeskSetup?>(null) }

    Scaffold(
        topBar = { DeskoraTopBar("Desk Setups", onBack = { navController.popBackStack() }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Routes.setupForm()) }) {
                Icon(Icons.Filled.Add, contentDescription = "Add setup")
            }
        }
    ) { pad ->
        val active = data.setups.filter { !it.archived }
        val archived = data.setups.filter { it.archived }
        if (data.setups.isEmpty()) {
            EmptyState("No setups yet", "Create a setup or use a template.", Modifier.padding(pad))
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(pad).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { SectionTitle("Active setups") }
            items(active, key = { it.id }) { setup ->
                SetupCard(setup, data, isActive = data.settings.activeSetupId == setup.id,
                    onSelect = { vm.setActiveSetup(setup.id); navController.navigate(Routes.DESK) },
                    onEdit = { navController.navigate(Routes.setupForm(setup.id)) },
                    onDuplicate = { vm.duplicateSetup(setup.id) },
                    onArchive = { vm.archiveSetup(setup.id, true) },
                    onRestore = null,
                    onDelete = { pendingDelete = setup })
            }
            if (archived.isNotEmpty()) {
                item { SectionTitle("Archived") }
                items(archived, key = { it.id }) { setup ->
                    SetupCard(setup, data, isActive = false,
                        onSelect = { vm.setActiveSetup(setup.id); navController.navigate(Routes.DESK) },
                        onEdit = { navController.navigate(Routes.setupForm(setup.id)) },
                        onDuplicate = { vm.duplicateSetup(setup.id) },
                        onArchive = null,
                        onRestore = { vm.archiveSetup(setup.id, false) },
                        onDelete = { pendingDelete = setup })
                }
            }
            item { Spacer(Modifier.height(60.dp)) }
        }
    }

    pendingDelete?.let { setup ->
        ConfirmDialog(
            title = "Delete this desk setup?",
            message = "This will also remove its zones, items, cable routes, checklist tasks, needed items, and notes.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { vm.deleteSetup(setup.id); pendingDelete = null },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun SetupCard(
    setup: DeskSetup,
    data: AppData,
    isActive: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onArchive: (() -> Unit)?,
    onRestore: (() -> Unit)?,
    onDelete: () -> Unit
) {
    val itemCount = data.items.count { it.setupId == setup.id && it.status != SetupItemStatus.Archived }
    val cableCount = data.cables.count { it.setupId == setup.id }
    val taskCount = data.cleaningTasks.count { it.setupId == setup.id }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxWidth().clickable { onSelect() }.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(setup.name.ifBlank { "Deleted Setup" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (isActive) Text("Active", style = MaterialTheme.typography.labelMedium, color = DeskoraColors.ChecklistComplete)
            }
            Text("${setup.setupType.label} · ${setup.deskShape.label}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$itemCount items · $cableCount cables · $taskCount tasks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDuplicate) { Text("Duplicate") }
                onArchive?.let { TextButton(onClick = it) { Text("Archive") } }
                onRestore?.let { TextButton(onClick = it) { Text("Restore") } }
                TextButton(onClick = onDelete) { Text("Delete", color = DeskoraColors.ErrorRed) }
            }
        }
    }
}

@Composable
fun SetupFormScreen(vm: DeskoraViewModel, data: AppData, navController: NavController, setupId: String?) {
    val existing = remember(setupId, data) { data.setups.firstOrNull { it.id == setupId } }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var type by remember { mutableStateOf(existing?.setupType ?: SetupType.WorkDesk) }
    var shape by remember { mutableStateOf(existing?.deskShape ?: DeskShape.Rectangle) }
    var width by remember { mutableStateOf(existing?.widthCm?.toString() ?: "") }
    var depth by remember { mutableStateOf(existing?.depthCm?.toString() ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var showNameError by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        DeskoraTopBar(if (existing == null) "New Setup" else "Edit Setup", onBack = { navController.popBackStack() })
    }) { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            DeskoraTextField(
                value = name, onValueChange = { name = Validation.trimTo(it, Validation.NAME_MAX); showNameError = false },
                label = "Setup name (required)", isError = showNameError,
                supportingText = if (showNameError) "Name is required" else null
            )
            DeskoraDropdown("Setup type (required)", SetupType.entries, type, { it.label }, { type = it })
            DeskoraDropdown("Desk shape (required)", DeskShape.entries, shape, { it.label }, { shape = it })
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DeskoraTextField(width, { width = it }, "Width (cm, optional)", modifier = Modifier.weight(1f), keyboardNumeric = true)
                DeskoraTextField(depth, { depth = it }, "Depth (cm, optional)", modifier = Modifier.weight(1f), keyboardNumeric = true)
            }
            DeskoraTextField(
                value = description, onValueChange = { description = Validation.trimTo(it, Validation.SETUP_NOTE_MAX) },
                label = "Description (optional)", singleLine = false, minLines = 3
            )
            com.deskora.setup.ui.components.DisclaimerBox(com.deskora.setup.ui.Disclaimers.MANUAL_SETUP)

            Button(
                onClick = {
                    if (Validation.isBlank(name)) { showNameError = true; return@Button }
                    val w = Validation.parsePositiveDoubleOrNull(width)
                    val d = Validation.parsePositiveDoubleOrNull(depth)
                    if (existing == null) {
                        vm.createSetup(name.trim(), type, shape, w, d, description) {
                            navController.navigate(Routes.DESK) { popUpTo(Routes.SETUP_FORM) { inclusive = true } }
                        }
                    } else {
                        vm.updateSetup(existing.copy(name = name.trim(), setupType = type, deskShape = shape, widthCm = w, depthCm = d, description = description))
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save Setup") }

            if (existing == null) {
                OutlinedButton(onClick = { navController.navigate(Routes.TEMPLATE_GALLERY) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Start From Template Instead")
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}
