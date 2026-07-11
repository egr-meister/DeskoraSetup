package com.deskora.setup.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material3.FilterChip
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
import com.deskora.setup.model.CleaningCategory
import com.deskora.setup.model.CleaningFrequencyType
import com.deskora.setup.model.CleaningStatus
import com.deskora.setup.model.CleaningTask
import com.deskora.setup.model.WeekDay
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
import com.deskora.setup.util.ChecklistDates
import com.deskora.setup.util.Validation

@Composable
fun CleaningChecklistScreen(vm: DeskoraViewModel, data: AppData, navController: NavController) {
    val setup = data.activeSetup
    Scaffold(
        topBar = { DeskoraTopBar("Cleaning Checklist") },
        floatingActionButton = {
            if (setup != null) FloatingActionButton(onClick = { navController.navigate(Routes.taskForm()) }) {
                Icon(Icons.Filled.Add, contentDescription = "Add task")
            }
        }
    ) { pad ->
        if (setup == null) { EmptyState("No active setup", "Create a setup first.", Modifier.padding(pad)); return@Scaffold }
        val today = ChecklistDates.today()
        val tasks = data.tasksFor(setup.id)
        if (tasks.isEmpty()) {
            Column(Modifier.padding(pad)) {
                EmptyState("No cleaning tasks yet", "Add a task like \"Wipe monitor exterior\".")
                DisclaimerBox(Disclaimers.CLEANING, Modifier.padding(16.dp))
            }
            return@Scaffold
        }
        val due = tasks.filter { ChecklistDates.statusOf(it, today) == CleaningStatus.Due }
        val upcoming = tasks.filter { ChecklistDates.statusOf(it, today) == CleaningStatus.Upcoming }
        val manual = tasks.filter { ChecklistDates.statusOf(it, today) == CleaningStatus.Manual }
        val complete = tasks.filter { ChecklistDates.statusOf(it, today) == CleaningStatus.Complete }
        val disabled = tasks.filter { ChecklistDates.statusOf(it, today) == CleaningStatus.Disabled }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(pad).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { DisclaimerBox(Disclaimers.CLEANING) }
            section("Due", due, vm, navController)
            section("Upcoming", upcoming, vm, navController)
            section("Manual", manual, vm, navController)
            section("Completed recently", complete, vm, navController)
            section("Disabled", disabled, vm, navController)
            item { Spacer(Modifier.height(70.dp)) }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    title: String,
    tasks: List<CleaningTask>,
    vm: DeskoraViewModel,
    navController: NavController
) {
    if (tasks.isEmpty()) return
    item(key = "header_$title") {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp))
    }
    items(tasks.size, key = { tasks[it].id }) { idx ->
        TaskRow(tasks[idx], vm, onOpen = { navController.navigate(Routes.taskForm(tasks[idx].id)) })
    }
}

@Composable
private fun TaskRow(task: CleaningTask, vm: DeskoraViewModel, onOpen: () -> Unit) {
    val status = ChecklistDates.statusOf(task, ChecklistDates.today())
    val statusColor = when (status) {
        CleaningStatus.Due -> DeskoraColors.ChecklistDue
        CleaningStatus.Complete -> DeskoraColors.ChecklistComplete
        CleaningStatus.Disabled -> DeskoraColors.Archived
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxWidth().clickable { onOpen() }.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(task.title.ifBlank { "Task" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(status.label, style = MaterialTheme.typography.labelMedium, color = statusColor)
            }
            val due = if (task.nextDueDate.isNotBlank()) "Next: ${task.nextDueDate}" else "No scheduled date"
            Text("${task.category.label} · ${task.frequencyType.label} · $due", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row {
                if (status == CleaningStatus.Complete) {
                    TextButton(onClick = { vm.undoTask(task.id) }) { Text("Undo") }
                } else if (task.enabled) {
                    TextButton(onClick = { vm.completeTask(task.id) }) { Text("Mark Complete") }
                }
                TextButton(onClick = onOpen) { Text("Edit") }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskFormScreen(vm: DeskoraViewModel, data: AppData, navController: NavController, taskId: String?) {
    val setup = data.activeSetup
    val existing = remember(taskId, data) { data.cleaningTasks.firstOrNull { it.id == taskId } }
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: CleaningCategory.Surface) }
    var frequency by remember { mutableStateOf(existing?.frequencyType ?: CleaningFrequencyType.Weekly) }
    var interval by remember { mutableStateOf(existing?.intervalValue?.toString() ?: "3") }
    var selectedDays by remember { mutableStateOf(existing?.selectedDays?.toSet() ?: emptySet()) }
    var enabled by remember { mutableStateOf(existing?.enabled ?: true) }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var titleError by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    val previewDue = remember(frequency, interval, selectedDays) {
        ChecklistDates.computeInitialDueDate(
            frequency, Validation.parsePositiveIntOrNull(interval), selectedDays.toList(), ChecklistDates.today()
        ).ifBlank { "No scheduled date (manual)" }
    }

    Scaffold(topBar = { DeskoraTopBar(if (existing == null) "Add Task" else "Edit Task", onBack = { navController.popBackStack() }) }) { pad ->
        if (setup == null) { EmptyState("No active setup", "Create a setup first.", Modifier.padding(pad)); return@Scaffold }
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            DeskoraTextField(title, { title = Validation.trimTo(it, Validation.NAME_MAX); titleError = false }, "Title (required)", isError = titleError, supportingText = if (titleError) "Title is required" else null)
            DeskoraDropdown("Category", CleaningCategory.entries, category, { it.label }, { category = it })
            DeskoraDropdown("Recurrence", CleaningFrequencyType.entries, frequency, { it.label }, { frequency = it })
            if (frequency == CleaningFrequencyType.EveryNumberOfDays) {
                DeskoraTextField(interval, { interval = it }, "Every N days", keyboardNumeric = true)
            }
            if (frequency == CleaningFrequencyType.SelectedDays) {
                Text("Selected days", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WeekDay.entries.forEach { day ->
                        FilterChip(
                            selected = day in selectedDays,
                            onClick = { selectedDays = if (day in selectedDays) selectedDays - day else selectedDays + day },
                            label = { Text(day.label) }
                        )
                    }
                }
            }
            Text("Next due date preview: $previewDue", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Enabled", modifier = Modifier.weight(1f))
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }
            DeskoraTextField(note, { note = Validation.trimTo(it, Validation.CHECKLIST_NOTE_MAX) }, "Note (optional)", singleLine = false, minLines = 2)
            DisclaimerBox(Disclaimers.CLEANING)

            Button(
                onClick = {
                    if (Validation.isBlank(title)) { titleError = true; return@Button }
                    val intervalVal = Validation.parsePositiveIntOrNull(interval)
                    if (existing == null) {
                        val task = CleaningTask(
                            setupId = setup.id, title = title.trim(), category = category, frequencyType = frequency,
                            intervalValue = intervalVal, selectedDays = selectedDays.toList(), enabled = enabled, note = note
                        )
                        vm.createTask(task) { navController.popBackStack() }
                    } else {
                        // Recompute due date if scheduling changed.
                        val newDue = ChecklistDates.computeInitialDueDate(frequency, intervalVal, selectedDays.toList(), ChecklistDates.today())
                        vm.updateTask(existing.copy(title = title.trim(), category = category, frequencyType = frequency, intervalValue = intervalVal, selectedDays = selectedDays.toList(), enabled = enabled, note = note, nextDueDate = newDue))
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save Task") }

            if (existing != null) {
                TextButton(onClick = { showDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete Task", color = DeskoraColors.ErrorRed)
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }

    if (showDelete && existing != null) {
        ConfirmDialog(
            title = "Delete task?", message = "This removes the cleaning task.",
            confirmLabel = "Delete", destructive = true,
            onConfirm = { vm.deleteTask(existing.id); showDelete = false; navController.popBackStack() },
            onDismiss = { showDelete = false }
        )
    }
}
