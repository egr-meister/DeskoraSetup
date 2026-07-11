package com.deskora.setup.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.deskora.setup.model.AppData
import com.deskora.setup.model.CleaningStatus
import com.deskora.setup.model.SetupItemStatus
import com.deskora.setup.ui.components.DeskMap
import com.deskora.setup.ui.components.EmptyState
import com.deskora.setup.ui.nav.Routes
import com.deskora.setup.ui.theme.DeskoraColors
import com.deskora.setup.ui.theme.Palette
import com.deskora.setup.ui.vm.DeskoraViewModel
import com.deskora.setup.util.ChecklistDates

@Composable
fun DeskMapScreen(vm: DeskoraViewModel, data: AppData, navController: NavController) {
    val setup = data.activeSetup
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Compact setup selector + actions at the very top.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { navController.navigate(Routes.SETUP_LIST) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        setup?.name ?: "No setup",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        setup?.setupType?.label ?: "Create a setup to begin",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1
                    )
                }
                Icon(Icons.Filled.UnfoldMore, contentDescription = "Switch setup", tint = Color.White)
            }
            IconButton(onClick = { navController.navigate(Routes.SEARCH) }) {
                Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color.White)
            }
            if (setup != null) {
                IconButton(onClick = { navController.navigate(Routes.EDITOR) }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit map", tint = Color.White)
                }
            }
        }

        if (setup == null) {
            EmptyState(
                title = "No desk setup yet.",
                message = "Create a setup or start from a template."
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                androidx.compose.material3.Button(
                    onClick = { navController.navigate(Routes.TEMPLATE_GALLERY) },
                    modifier = Modifier.weight(1f)
                ) { Text("Use Template") }
                androidx.compose.material3.OutlinedButton(
                    onClick = { navController.navigate(Routes.setupForm()) },
                    modifier = Modifier.weight(1f)
                ) { Text("New Setup") }
            }
            return@Column
        }

        val zones = data.zonesFor(setup.id)
        val items = data.itemsFor(setup.id)
        val cables = data.cablesFor(setup.id)

        // Large top-down desk surface.
        Box(modifier = Modifier.padding(12.dp)) {
            DeskMap(
                setup = setup,
                zones = zones,
                items = items,
                cables = cables,
                showGrid = data.settings.showDeskGrid,
                showZoneLabels = data.settings.showZoneLabels,
                showCableLabels = data.settings.showCableLabels,
                onItemTap = { itemId -> navController.navigate(Routes.itemDetail(itemId)) },
                onBackgroundTap = { navController.navigate(Routes.EDITOR) }
            )
        }

        // Unplaced-items tray with an integrated Add Item action.
        UnplacedTray(
            items = items.filter { it.status == SetupItemStatus.Unplaced },
            onAdd = { navController.navigate(Routes.itemForm()) },
            onOpen = { navController.navigate(Routes.itemDetail(it)) }
        )

        // Compact cleaning strip.
        CleaningStrip(data = data, setupId = setup.id, onOpen = { navController.navigate(Routes.CHECKLIST) })

        // Needed-items drawer preview.
        NeededDrawerPreview(data = data, setupId = setup.id, onOpen = { navController.navigate(Routes.NEEDED) })

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun UnplacedTray(
    items: List<com.deskora.setup.model.SetupItem>,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Unplaced tray (${items.size})",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "Add item")
            }
        }
        if (items.isEmpty()) {
            Text(
                "No unplaced items. Add an item, then place it in a zone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items.forEach { item ->
                    Box(
                        modifier = Modifier
                            .clickable { onOpen(item.id) }
                            .background(DeskoraColors.UnplacedGray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .border(1.dp, DeskoraColors.UnplacedGray, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(item.name, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun CleaningStrip(data: AppData, setupId: String, onOpen: () -> Unit) {
    val today = ChecklistDates.today()
    val tasks = data.tasksFor(setupId)
    val due = tasks.count { ChecklistDates.statusOf(it, today) == CleaningStatus.Due }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            .clickable { onOpen() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(if (due > 0) DeskoraColors.ChecklistDue else DeskoraColors.ChecklistComplete, CircleShape)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Cleaning checklist", style = MaterialTheme.typography.titleSmall)
            Text(
                if (due > 0) "$due task${if (due == 1) "" else "s"} due" else "Nothing due right now",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text("${tasks.size} total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NeededDrawerPreview(data: AppData, setupId: String, onOpen: () -> Unit) {
    val needed = data.neededFor(setupId).filter { !it.acquired }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .background(DeskoraColors.LightDeskWood.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .clickable { onOpen() }
            .padding(12.dp)
    ) {
        Text("Needed items (${needed.size})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        if (needed.isEmpty()) {
            Text("No needed items yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            needed.take(3).forEach {
                Text("• ${it.title}", style = MaterialTheme.typography.bodyMedium)
            }
            if (needed.size > 3) Text("…and ${needed.size - 3} more", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
