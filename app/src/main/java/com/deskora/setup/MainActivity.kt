package com.deskora.setup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deskora.setup.ui.nav.DeskoraNavHost
import com.deskora.setup.ui.theme.DeskoraTheme
import com.deskora.setup.ui.vm.DeskoraViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: DeskoraViewModel by viewModels { DeskoraViewModel.factory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            DeskoraTheme {
                val data by viewModel.appData.collectAsStateWithLifecycle()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DeskoraNavHost(viewModel = viewModel, appData = data)
                }
            }
        }
    }
}
