package com.lorenzo.caltracker.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Text field with a numeric keyboard. The value stays a string so partial input like "1," is allowed.
 * Focusing it selects the whole value, so typing replaces it.
 */
@Composable
fun DecimalField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    var field by remember { mutableStateOf(TextFieldValue(value)) }
    // Set on focus: the tap that focuses the field also moves the cursor, which would undo the selection.
    var selectAllOnTap by remember { mutableStateOf(false) }
    if (field.text != value) field = TextFieldValue(value, TextRange(value.length))

    OutlinedTextField(
        value = field,
        onValueChange = { new ->
            when {
                !new.text.all { it.isDigit() || it == '.' || it == ',' } -> Unit
                new.text == field.text && selectAllOnTap -> {
                    selectAllOnTap = false
                    field = new.copy(selection = TextRange(0, new.text.length))
                }
                else -> {
                    selectAllOnTap = false
                    field = new
                    if (new.text != value) onValueChange(new.text)
                }
            }
        },
        label = { Text(label) },
        suffix = suffix?.let { { Text(it) } },
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        modifier = modifier.onFocusChanged { state ->
            if (state.isFocused && !selectAllOnTap) {
                field = field.copy(selection = TextRange(0, field.text.length))
                selectAllOnTap = true
            } else if (!state.isFocused) {
                selectAllOnTap = false
            }
        },
    )
}
