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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.deskora.setup.model.DeskZone
import com.deskora.setup.model.SetupItem
import com.deskora.setup.model.SetupItemStatus
import com.deskora.setup.model.SetupTemplate
import com.deskora.setup.ui.components.ConfirmDialog
import com.deskora.setup.ui.components.DeskMap
import com.deskora.setup.ui.components.DeskoraTextField
import com.deskora.setup.ui.components.DeskoraTopBar
import com.deskora.setup.ui.components.EmptyState
import com.deskora.setup.ui.nav.Routes
import com.deskora.setup.ui.vm.DeskoraViewModel
import com.deskora.setup.util.BuiltInTemplates

@Composable
fun TemplateGalleryScreen(vm: DeskoraViewModel, data: AppData, navController: NavController) {
    Scaffold(topBar = { DeskoraTopBar("Templates", onBack = { navController.popBackStack() }) }) { pad ->
        val builtIn = data.templates.filter { it.id in BuiltInTemplates.builtInIds }
        val custom = data.templates.filter { it.id !in BuiltInTemplates.builtInIds }
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Built-in templates", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(
                "Templates are organizational examples with generic labels. No brands, products, or prices.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            builtIn.forEach { t -> TemplateRow(t, onOpen = { navController.navigate(Routes.templatePreview(t.id)) }) }

            Text("Your templates", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
            if (custom.isEmpty()) {
                Text("No custom templates yet. Save any setup as a template from Settings or the Item editor.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            custom.forEach { t -> TemplateRow(t, onOpen = { navController.navigate(Routes.templatePreview(t.id)) }) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TemplateRow(t: SetupTemplate, onOpen: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxWidth().clickable { onOpen() }.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(if (t.userCreated) "Custom" else "Built-in", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${t.setupType.label} · ${t.defaultZones.size} zones · ${t.defaultItems.size} items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TemplatePreviewScreen(vm: DeskoraViewModel, data: AppData, navController: NavController, templateId: String?) {
    val template = remember(templateId, data) { data.templates.firstOrNull { it.id == templateId } }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(template?.name ?: "") }

    Scaffold(topBar = { DeskoraTopBar(template?.name ?: "Template", onBack = { navController.popBackStack() }) }) { pad ->
        if (template == null) {
            Column(Modifier.padding(pad)) {
                EmptyState("Template not found", "It may have been deleted.")
                OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.padding(16.dp)) { Text("Back") }
            }
            return@Scaffold
        }
        // Build a preview map from the template.
        val previewSetup = DeskSetup(id = "tpl_preview", name = template.name, deskShape = template.deskShape)
        val zones = template.defaultZones.map {
            DeskZone(id = it.id, setupId = "tpl_preview", name = it.name, zoneType = it.zoneType, normalizedX = it.normalizedX, normalizedY = it.normalizedY, normalizedWidth = it.normalizedWidth, normalizedHeight = it.normalizedHeight, sortOrder = it.sortOrder, colorKey = it.colorKey)
        }
        val items = template.defaultItems.map {
            SetupItem(id = it.id, setupId = "tpl_preview", zoneId = it.zoneTemplateId, name = it.name, itemType = it.itemType, normalizedX = it.normalizedX, normalizedY = it.normalizedY, normalizedWidth = it.normalizedWidth, normalizedHeight = it.normalizedHeight, rotationDegrees = it.rotationDegrees, colorKey = it.colorKey, status = SetupItemStatus.Placed)
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("${template.setupType.label} · ${template.deskShape.label}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            DeskMap(previewSetup, zones, items, emptyList(), showGrid = true, showZoneLabels = true, showCableLabels = false)
            Text(template.description, style = MaterialTheme.typography.bodyMedium)

            DeskoraTextField(name, { name = it }, "New setup name")
            Button(
                onClick = {
                    vm.applyTemplate(template.id, name.ifBlank { template.name }) { newId ->
                        if (newId != null) navController.navigate(Routes.DESK) { popUpTo(Routes.TEMPLATE_GALLERY) { inclusive = true } }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Apply Template") }

            if (template.userCreated) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { vm.duplicateTemplate(template.id) }, modifier = Modifier.weight(1f)) { Text("Duplicate") }
                    TextButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.weight(1f)) {
                        Text("Delete", color = com.deskora.setup.ui.theme.DeskoraColors.ErrorRed)
                    }
                }
            } else {
                Text("Built-in templates are read-only.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDeleteConfirm && template != null) {
        ConfirmDialog(
            title = "Delete custom template?",
            message = "This removes the template. Existing setups created from it are not affected.",
            confirmLabel = "Delete", destructive = true,
            onConfirm = { vm.deleteTemplate(template.id); showDeleteConfirm = false; navController.popBackStack() },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}
