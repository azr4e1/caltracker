package com.lorenzo.caltracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lorenzo.caltracker.ui.navigation.CalTrackerNavHost
import com.lorenzo.caltracker.ui.theme.CalTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalTrackerTheme {
                CalTrackerNavHost()
            }
        }
    }
}
