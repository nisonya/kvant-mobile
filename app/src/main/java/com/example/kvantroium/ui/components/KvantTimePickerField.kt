package com.example.kvantroium.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.example.kvantroium.R
import com.example.kvantroium.features.events.formatTimeFromHourMinute
import com.example.kvantroium.features.events.parseTimeToHourMinute
import com.example.kvantroium.ui.theme.Montserrat

@Composable
fun KvantTimePickerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var showPicker by remember { mutableStateOf(false) }
    val displayValue = if (value.isBlank()) "--:--" else value

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label, fontFamily = Montserrat) },
            trailingIcon = {
                IconButton(
                    onClick = { showPicker = true },
                    enabled = enabled
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_schedule_24),
                        contentDescription = "Выбрать время"
                    )
                }
            },
            colors = kvantTextFieldColors()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(enabled = enabled) { showPicker = true }
        )
    }

    if (showPicker) {
        KvantTimePickerDialog(
            initialTime = value,
            onDismiss = { showPicker = false },
            onConfirm = { selected ->
                onValueChange(selected)
                showPicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KvantTimePickerDialog(
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val (initialHour, initialMinute) = parseTimeToHourMinute(initialTime) ?: (12 to 0)
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onConfirm(formatTimeFromHourMinute(state.hour, state.minute)) }
            ) {
                Text("OK", fontFamily = Montserrat)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", fontFamily = Montserrat)
            }
        },
        text = {
            TimePicker(state = state)
        }
    )
}
