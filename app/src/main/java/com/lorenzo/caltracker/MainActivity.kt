package com.lorenzo.caltracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lorenzo.caltracker.ui.navigation.CalTrackerNavHost
import com.lorenzo.caltracker.ui.navigation.StartupViewModel
import com.lorenzo.caltracker.ui.theme.CalTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalTrackerTheme {
                val startup: StartupViewModel = viewModel(factory = StartupViewModel.Factory)
                val needsSetup by startup.needsSetup.collectAsStateWithLifecycle()
                needsSetup?.let { CalTrackerNavHost(needsSetup = it) }
            }
        }
    }
}
