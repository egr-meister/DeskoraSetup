package com.deskora.setup.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.deskora.setup.model.AppData
import com.deskora.setup.model.DeskSetup
import com.deskora.setup.model.DeskShape
import com.deskora.setup.model.DeskZone
import com.deskora.setup.model.SetupItem
import com.deskora.setup.model.SetupItemStatus
import com.deskora.setup.model.SetupItemType
import com.deskora.setup.ui.Disclaimers
import com.deskora.setup.ui.components.DeskMap
import com.deskora.setup.ui.components.DisclaimerBox
import com.deskora.setup.ui.nav.Routes
import com.deskora.setup.ui.vm.DeskoraViewModel

@Composable
fun OnboardingScreen(vm: DeskoraViewModel, data: AppData, navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Deskora Setup", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "Build a clear map of your workspace.",
            style = MaterialTheme.typography.titleMedium
        )

        // Simplified top-down desk map preview.
        val previewSetup = DeskSetup(id = "preview", name = "Preview", deskShape = DeskShape.Rectangle)
        val previewZones = listOf(
            DeskZone(id = "z1", setupId = "preview", name = "Center", normalizedX = 0.28f, normalizedY = 0.18f, normalizedWidth = 0.44f, normalizedHeight = 0.6f, colorKey = "zone"),
            DeskZone(id = "z2", setupId = "preview", name = "Cable", normalizedX = 0.05f, normalizedY = 0.02f, normalizedWidth = 0.9f, normalizedHeight = 0.13f, colorKey = "cable_area")
        )
        val previewItems = listOf(
            SetupItem(id = "i1", setupId = "preview", name = "Monitor", itemType = SetupItemType.Monitor, normalizedX = 0.34f, normalizedY = 0.2f, normalizedWidth = 0.3f, normalizedHeight = 0.12f, status = SetupItemStatus.Placed, colorKey = "monitor"),
            SetupItem(id = "i2", setupId = "preview", name = "Keyboard", itemType = SetupItemType.Keyboard, normalizedX = 0.33f, normalizedY = 0.55f, normalizedWidth = 0.34f, normalizedHeight = 0.1f, status = SetupItemStatus.Placed, colorKey = "accessory"),
            SetupItem(id = "i3", setupId = "preview", name = "Lamp", itemType = SetupItemType.Lamp, normalizedX = 0.72f, normalizedY = 0.2f, normalizedWidth = 0.12f, normalizedHeight = 0.12f, status = SetupItemStatus.Placed, colorKey = "lighting")
        )
        DeskMap(
            setup = previewSetup,
            zones = previewZones,
            items = previewItems,
            cables = emptyList(),
            showGrid = true,
            showZoneLabels = true,
            showCableLabels = false
        )

        Text(
            "Place generic devices and accessories into desk zones. Document cable routes and keep " +
                "an unplaced-items tray. Maintain a cleaning checklist and a list of needed items. " +
                "Your setup stays on this device.",
            style = MaterialTheme.typography.bodyMedium
        )

        OnboardingPoint("Top-down desk map", "See your whole workspace from above.")
        OnboardingPoint("Zones", "Divide the desk into Main, Left, Cable, Storage and more.")
        OnboardingPoint("Generic devices & accessories", "Abstract shapes, never real brands.")
        OnboardingPoint("Manual cable routes", "Draw simple visual connections between items.")
        OnboardingPoint("Cleaning checklist", "Track recurring desk cleaning tasks locally.")
        OnboardingPoint("Needed-items list", "Keep a personal list of items you may need.")
        OnboardingPoint("Setup templates", "Start from Work, Study, Gaming, or a Blank desk.")
        OnboardingPoint("Offline storage", "No account, no cloud, no internet.")

        DisclaimerBox(
            "Deskora Setup does not connect to devices, recommend products, provide electrical " +
                "guidance, or guarantee ergonomic outcomes."
        )
        DisclaimerBox(Disclaimers.MANUAL_SETUP)

        Spacer(Modifier.height(4.dp))
        Button(
            onClick = {
                vm.completeOnboarding()
                navController.navigate(Routes.TEMPLATE_GALLERY)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Create Desk Setup") }

        OutlinedButton(
            onClick = {
                vm.completeOnboarding()
                vm.applyTemplate(com.deskora.setup.util.BuiltInTemplates.BLANK_ID, "Blank Desk") {
                    navController.navigate(Routes.DESK) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Explore Blank Desk") }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun OnboardingPoint(title: String, body: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
