package com.lorenzo.caltracker.ui.components

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.lorenzo.caltracker.ui.formatHm
import java.time.LocalTime

/** A button showing the time; tapping it opens a 24-hour time picker. */
@Composable
fun TimeField(time: LocalTime, onTimeChange: (LocalTime) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = modifier) { Text("Time  ${time.formatHm()}") }
    if (open) {
        TimeSelectDialog(
            initial = time,
            onDismiss = { open = false },
            onConfirm = {
                onTimeChange(it)
                open = false
            },
        )
    }
}
