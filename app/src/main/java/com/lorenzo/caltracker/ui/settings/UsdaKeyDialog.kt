package com.lorenzo.caltracker.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private const val SIGNUP_URL = "https://fdc.nal.usda.gov/api-key-signup"

/** Lets the user paste their own USDA key, or go back to the shared one. */
@Composable
fun UsdaKeyDialog(
    currentKey: String?,
    checking: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onUseShared: () -> Unit,
) {
    val context = LocalContext.current
    var key by remember { mutableStateOf(currentKey.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("USDA API key") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Generic-food search uses USDA FoodData Central. Without your own key, a shared key is used, " +
                        "which allows only a few searches per hour. Your own key is free and allows 1,000 per hour.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SIGNUP_URL))) }) {
                    Text("Get a free key at fdc.nal.usda.gov")
                }
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it.trim() },
                    label = { Text("Your key") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (currentKey != null) {
                    TextButton(onClick = onUseShared) { Text("Remove my key and use the shared one") }
                }
            }
        },
        confirmButton = {
            if (checking) {
                CircularProgressIndicator(Modifier.padding(8.dp))
            } else {
                TextButton(onClick = { onSave(key) }, enabled = key.isNotBlank() && key != currentKey) { Text("Save") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
