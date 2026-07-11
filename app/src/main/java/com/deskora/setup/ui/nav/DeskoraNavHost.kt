package com.deskora.setup.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.deskora.setup.model.AppData
import com.deskora.setup.ui.screens.CableFormScreen
import com.deskora.setup.ui.screens.CableListScreen
import com.deskora.setup.ui.screens.CleaningChecklistScreen
import com.deskora.setup.ui.screens.DeskMapEditorScreen
import com.deskora.setup.ui.screens.DeskMapScreen
import com.deskora.setup.ui.screens.ItemDetailScreen
import com.deskora.setup.ui.screens.ItemFormScreen
import com.deskora.setup.ui.screens.ItemListScreen
import com.deskora.setup.ui.screens.NeededFormScreen
import com.deskora.setup.ui.screens.NeededItemsScreen
import com.deskora.setup.ui.screens.OnboardingScreen
import com.deskora.setup.ui.screens.SearchScreen
import com.deskora.setup.ui.screens.SettingsScreen
import com.deskora.setup.ui.screens.SetupFormScreen
import com.deskora.setup.ui.screens.SetupListScreen
import com.deskora.setup.ui.screens.TaskFormScreen
import com.deskora.setup.ui.screens.TemplateGalleryScreen
import com.deskora.setup.ui.screens.TemplatePreviewScreen
import com.deskora.setup.ui.screens.ZoneFormScreen
import com.deskora.setup.ui.screens.ZoneManagementScreen
import com.deskora.setup.ui.vm.DeskoraViewModel

private data class BottomTab(val route: String, val label: String, val icon: ImageVector)

private val bottomTabs = listOf(
    BottomTab(Routes.DESK, "Desk", Icons.Outlined.GridView),
    BottomTab(Routes.ITEMS, "Items", Icons.Outlined.Inventory2),
    BottomTab(Routes.CHECKLIST, "Checklist", Icons.Outlined.Checklist),
    BottomTab(Routes.NEEDED, "Needed", Icons.Outlined.ListAlt),
    BottomTab(Routes.SETTINGS, "Settings", Icons.Outlined.Settings)
)

@Composable
fun DeskoraNavHost(viewModel: DeskoraViewModel, appData: AppData) {
    val navController = rememberNavController()

    val startDestination = if (appData.settings.onboardingCompleted) Routes.DESK else Routes.ONBOARDING

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomTabs.map { it.route }

    androidx.compose.material3.Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    val currentDestination = backStackEntry?.destination
                    bottomTabs.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = androidx.compose.ui.Modifier.padding(innerPadding)
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(vm = viewModel, data = appData, navController = navController)
            }
            composable(Routes.DESK) {
                DeskMapScreen(vm = viewModel, data = appData, navController = navController)
            }
            composable(Routes.ITEMS) {
                ItemListScreen(vm = viewModel, data = appData, navController = navController)
            }
            composable(Routes.CHECKLIST) {
                CleaningChecklistScreen(vm = viewModel, data = appData, navController = navController)
            }
            composable(Routes.NEEDED) {
                NeededItemsScreen(vm = viewModel, data = appData, navController = navController)
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(vm = viewModel, data = appData, navController = navController)
            }
            composable(Routes.EDITOR) {
                DeskMapEditorScreen(vm = viewModel, data = appData, navController = navController)
            }
            composable(Routes.SEARCH) {
                SearchScreen(vm = viewModel, data = appData, navController = navController)
            }
            composable(Routes.SETUP_LIST) {
                SetupListScreen(vm = viewModel, data = appData, navController = navController)
            }
            composable(
                route = Routes.SETUP_FORM,
                arguments = listOf(navArgument("setupId") { type = NavType.StringType; defaultValue = "" })
            ) { entry ->
                val id = entry.arguments?.getString("setupId").orEmptyToNull()
                SetupFormScreen(vm = viewModel, data = appData, navController = navController, setupId = id)
            }
            composable(Routes.TEMPLATE_GALLERY) {
                TemplateGalleryScreen(vm = viewModel, data = appData, navController = navController)
            }
            composable(
                route = Routes.TEMPLATE_PREVIEW,
                arguments = listOf(navArgument("templateId") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("templateId").orEmptyToNull()
                TemplatePreviewScreen(vm = viewModel, data = appData, navController = navController, templateId = id)
            }
            composable(
                route = Routes.ITEM_FORM,
                arguments = listOf(navArgument("itemId") { type = NavType.StringType; defaultValue = "" })
            ) { entry ->
                val id = entry.arguments?.getString("itemId").orEmptyToNull()
                ItemFormScreen(vm = viewModel, data = appData, navController = navController, itemId = id)
            }
            composable(
                route = Routes.ITEM_DETAIL,
                arguments = listOf(navArgument("itemId") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("itemId").orEmptyToNull()
                ItemDetailScreen(vm = viewModel, data = appData, navController = navController, itemId = id)
            }
            composable(Routes.ZONES) {
                ZoneManagementScreen(vm = viewModel, data = appData, navController = navController)
            }
            composable(
                route = Routes.ZONE_FORM,
                arguments = listOf(navArgument("zoneId") { type = NavType.StringType; defaultValue = "" })
            ) { entry ->
                val id = entry.arguments?.getString("zoneId").orEmptyToNull()
                ZoneFormScreen(vm = viewModel, data = appData, navController = navController, zoneId = id)
            }
            composable(Routes.CABLES) {
                CableListScreen(vm = viewModel, data = appData, navController = navController)
            }
            composable(
                route = Routes.CABLE_FORM,
                arguments = listOf(navArgument("cableId") { type = NavType.StringType; defaultValue = "" })
            ) { entry ->
                val id = entry.arguments?.getString("cableId").orEmptyToNull()
                CableFormScreen(vm = viewModel, data = appData, navController = navController, cableId = id)
            }
            composable(
                route = Routes.TASK_FORM,
                arguments = listOf(navArgument("taskId") { type = NavType.StringType; defaultValue = "" })
            ) { entry ->
                val id = entry.arguments?.getString("taskId").orEmptyToNull()
                TaskFormScreen(vm = viewModel, data = appData, navController = navController, taskId = id)
            }
            composable(
                route = Routes.NEEDED_FORM,
                arguments = listOf(navArgument("neededId") { type = NavType.StringType; defaultValue = "" })
            ) { entry ->
                val id = entry.arguments?.getString("neededId").orEmptyToNull()
                NeededFormScreen(vm = viewModel, data = appData, navController = navController, neededId = id)
            }
        }
    }
}

private fun String?.orEmptyToNull(): String? = if (this.isNullOrBlank()) null else this
