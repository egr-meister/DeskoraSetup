package com.deskora.setup.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.deskora.setup.model.AppData
import com.deskora.setup.ui.components.DeskoraTextField
import com.deskora.setup.ui.components.DeskoraTopBar
import com.deskora.setup.ui.components.EmptyState
import com.deskora.setup.ui.nav.Routes
import com.deskora.setup.ui.vm.DeskoraViewModel
import com.deskora.setup.util.Search

@Composable
fun SearchScreen(vm: DeskoraViewModel, data: AppData, navController: NavController) {
    val setup = data.activeSetup
    var query by remember { mutableStateOf("") }
    val results = remember(query, data, setup?.id) { Search.run(data, setup?.id, query) }

    Scaffold(topBar = { DeskoraTopBar("Search", onBack = { navController.popBackStack() }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DeskoraTextField(query, { query = it }, "Search this setup")
            if (setup == null) { EmptyState("No active setup", "Create a setup to search it."); return@Column }
            if (query.isBlank()) { Text("Type to search zones, items, cables, tasks, and needed items.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); return@Column }
            if (results.isEmpty) { Text("No matches for \"$query\".", style = MaterialTheme.typography.bodyMedium); return@Column }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (results.items.isNotEmpty()) {
                    item { ResultHeader("Items (${results.items.size})") }
                    items(results.items.size) { i ->
                        val it = results.items[i]
                        ResultRow(it.name, it.itemType.label) { navController.navigate(Routes.itemDetail(it.id)) }
                    }
                }
                if (results.zones.isNotEmpty()) {
                    item { ResultHeader("Zones (${results.zones.size})") }
                    items(results.zones.size) { i ->
                        val it = results.zones[i]
                        ResultRow(it.name, it.zoneType.label) { navController.navigate(Routes.zoneForm(it.id)) }
                    }
                }
                if (results.cables.isNotEmpty()) {
                    item { ResultHeader("Cables (${results.cables.size})") }
                    items(results.cables.size) { i ->
                        val it = results.cables[i]
                        ResultRow(it.name.ifBlank { "Cable" }, it.cableType.label) { navController.navigate(Routes.cableForm(it.id)) }
                    }
                }
                if (results.tasks.isNotEmpty()) {
                    item { ResultHeader("Checklist (${results.tasks.size})") }
                    items(results.tasks.size) { i ->
                        val it = results.tasks[i]
                        ResultRow(it.title, it.category.label) { navController.navigate(Routes.taskForm(it.id)) }
                    }
                }
                if (results.needed.isNotEmpty()) {
                    item { ResultHeader("Needed (${results.needed.size})") }
                    items(results.needed.size) { i ->
                        val it = results.needed[i]
                        ResultRow(it.title, it.category.label) { navController.navigate(Routes.neededForm(it.id)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun ResultRow(title: String, subtitle: String, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 6.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
