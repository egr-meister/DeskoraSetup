package com.deskora.setup.ui.screens

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.deskora.setup.model.AppData
import com.deskora.setup.model.ItemSizePreset
import com.deskora.setup.model.MapControlStep
import com.deskora.setup.model.WeekDay
import com.deskora.setup.ui.Disclaimers
import com.deskora.setup.ui.components.ConfirmDialog
import com.deskora.setup.ui.components.DeskoraDropdown
import com.deskora.setup.ui.components.DisclaimerBox
import com.deskora.setup.ui.nav.Routes
import com.deskora.setup.ui.theme.DeskoraColors
import com.deskora.setup.ui.vm.DeskoraViewModel

@Composable
fun SettingsScreen(vm: DeskoraViewModel, data: AppData, navController: NavController) {
    var confirmReset by remember { mutableStateOf(false) }
    var confirmDeleteActive by remember { mutableStateOf(false) }
    var confirmDeleteArchived by remember { mutableStateOf(false) }
    val settings = data.settings
    val active = data.activeSetup

    Scaffold(topBar = { com.deskora.setup.ui.components.DeskoraTopBar("Settings") }) { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Group("Setup")
            val setupOptions = listOf<String?>(null) + data.setups.filter { !it.archived }.map { it.id }
            DeskoraDropdown("Active setup", setupOptions, settings.activeSetupId, { id -> data.setups.firstOrNull { it.id == id }?.name ?: "None" }, { vm.setActiveSetup(it) })
            val templateOptions = listOf<String?>(null) + data.templates.map { it.id }
            DeskoraDropdown("Default template", templateOptions, settings.defaultTemplateId, { id -> data.templates.firstOrNull { it.id == id }?.name ?: "None" }, { id -> vm.updateSettings { it.copy(defaultTemplateId = id) } })

            Group("Map display")
            ToggleRow("Show desk grid", settings.showDeskGrid) { v -> vm.updateSettings { it.copy(showDeskGrid = v) } }
            ToggleRow("Show cable labels", settings.showCableLabels) { v -> vm.updateSettings { it.copy(showCableLabels = v) } }
            ToggleRow("Show zone labels", settings.showZoneLabels) { v -> vm.updateSettings { it.copy(showZoneLabels = v) } }
            DeskoraDropdown("Default item size", ItemSizePreset.entries, settings.defaultItemSize, { it.label }, { s -> vm.updateSettings { it.copy(defaultItemSize = s) } })
            DeskoraDropdown("Map control step size", MapControlStep.entries, settings.mapControlStep, { it.label }, { s -> vm.updateSettings { it.copy(mapControlStep = s) } })
            DeskoraDropdown("First day of week", WeekDay.entries, settings.firstDayOfWeek, { it.label }, { d -> vm.updateSettings { it.copy(firstDayOfWeek = d) } })

            Group("Templates & data")
            OutlinedButton(onClick = { navController.navigate(Routes.TEMPLATE_GALLERY) }, modifier = Modifier.fillMaxWidth()) { Text("Open Template Gallery") }
            OutlinedButton(onClick = { vm.restoreBuiltInTemplates() }, modifier = Modifier.fillMaxWidth()) { Text("Restore Built-in Templates") }
            OutlinedButton(onClick = { active?.let { vm.clearCompletedHistory(it.id) } }, modifier = Modifier.fillMaxWidth()) { Text("Clear Completed Checklist History") }
            OutlinedButton(onClick = { active?.let { vm.clearAcquiredNeededItems(it.id) } }, modifier = Modifier.fillMaxWidth()) { Text("Clear Acquired Needed Items") }
            OutlinedButton(onClick = { vm.updateSettings { it.copy(onboardingCompleted = false) }; navController.navigate(Routes.ONBOARDING) }, modifier = Modifier.fillMaxWidth()) { Text("Show Onboarding Again") }

            Group("Destructive")
            OutlinedButton(onClick = { confirmDeleteArchived = true }, modifier = Modifier.fillMaxWidth()) { Text("Delete Archived Setups", color = DeskoraColors.ErrorRed) }
            if (active != null) {
                OutlinedButton(onClick = { confirmDeleteActive = true }, modifier = Modifier.fillMaxWidth()) { Text("Delete Active Setup", color = DeskoraColors.ErrorRed) }
            }
            OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) { Text("Reset All Local Data", color = DeskoraColors.ErrorRed) }

            Group("About")
            Text("Deskora Setup", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text("Version 1.0.0 · Fully offline · No permissions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider()
            LabeledDisclaimer("Manual setup disclaimer", Disclaimers.MANUAL_SETUP)
            LabeledDisclaimer("Hardware and safety disclaimer", Disclaimers.HARDWARE_SAFETY)
            LabeledDisclaimer("Privacy note", Disclaimers.PRIVACY)
            Spacer(Modifier.height(60.dp))
        }
    }

    if (confirmReset) {
        ConfirmDialog(
            title = "Reset all local data?",
            message = "This will permanently remove every desk setup, zone, item, cable route, checklist task, needed item, note, template, and setting stored by Deskora Setup.",
            confirmLabel = "Reset Everything", destructive = true,
            onConfirm = { vm.resetAllData(); confirmReset = false; navController.navigate(Routes.ONBOARDING) { popUpTo(0) } },
            onDismiss = { confirmReset = false }
        )
    }
    if (confirmDeleteActive && active != null) {
        ConfirmDialog(
            title = "Delete this desk setup?",
            message = "This will also remove its zones, items, cable routes, checklist tasks, needed items, and notes.",
            confirmLabel = "Delete", destructive = true,
            onConfirm = { vm.deleteSetup(active.id); confirmDeleteActive = false },
            onDismiss = { confirmDeleteActive = false }
        )
    }
    if (confirmDeleteArchived) {
        ConfirmDialog(
            title = "Delete archived setups?",
            message = "This permanently removes all archived setups and their data.",
            confirmLabel = "Delete", destructive = true,
            onConfirm = { vm.deleteArchivedSetups(); confirmDeleteArchived = false },
            onDismiss = { confirmDeleteArchived = false }
        )
    }
}

@Composable
private fun Group(title: String) {
    Text(title.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 10.dp))
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun LabeledDisclaimer(title: String, body: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        DisclaimerBox(body)
    }
}
