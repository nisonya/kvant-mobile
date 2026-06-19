package com.example.kvantroium.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.kvantroium.R
import com.example.kvantroium.features.events.pickerMillisToUiDate
import com.example.kvantroium.features.events.uiDateToPickerMillis
import com.example.kvantroium.ui.theme.Montserrat

private const val CALENDAR_MIN_WIDTH_DP = 360

@Composable
fun KvantDatePickerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "дд.мм.гггг"
) {
    var showPicker by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label, fontFamily = Montserrat) },
        placeholder = { Text(placeholder, fontFamily = Montserrat) },
        singleLine = true,
        trailingIcon = {
            IconButton(
                onClick = { showPicker = true },
                enabled = enabled
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_calendar_month_24),
                    contentDescription = "Выбрать дату"
                )
            }
        },
        colors = kvantTextFieldColors()
    )

    if (showPicker) {
        KvantDatePickerDialog(
            initialDate = value,
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
private fun KvantDatePickerDialog(
    initialDate: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val useCalendarMode = screenWidthDp >= CALENDAR_MIN_WIDTH_DP
    val state = rememberDatePickerState(
        initialSelectedDateMillis = uiDateToPickerMillis(initialDate),
        yearRange = IntRange(2000, 2100),
        initialDisplayMode = if (useCalendarMode) DisplayMode.Picker else DisplayMode.Input
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                DatePicker(
                    state = state,
                    modifier = Modifier.fillMaxWidth(),
                    showModeToggle = useCalendarMode,
                    title = null,
                    headline = null
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена", fontFamily = Montserrat)
                    }
                    TextButton(
                        onClick = {
                            state.selectedDateMillis?.let { onConfirm(pickerMillisToUiDate(it)) }
                        },
                        enabled = state.selectedDateMillis != null
                    ) {
                        Text("OK", fontFamily = Montserrat)
                    }
                }
            }
        }
    }
}
