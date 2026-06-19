package com.example.kvantroium.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kvantroium.ui.theme.Montserrat
import com.example.kvantroium.ui.theme.kvantContentColor

@Composable
fun <T> KvantFormSelect(
    label: String,
    selectedLabel: String,
    options: List<Pair<T, String>>,
    selectedValue: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueEquals: (T, T) -> Boolean = { a, b -> a == b }
) {
    KvantFilterChipMenu(
        chipLabel = label,
        selectedLabel = selectedLabel,
        isActive = options.firstOrNull()?.let { (value, _) -> !valueEquals(selectedValue, value) } ?: false,
        options = options,
        selectedValue = selectedValue,
        onSelected = onSelected,
        modifier = modifier.fillMaxWidth(),
        valueEquals = valueEquals
    )
}

@Composable
fun KvantFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 4,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label, fontFamily = Montserrat) },
        readOnly = readOnly,
        singleLine = singleLine,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        colors = kvantTextFieldColors()
    )
}

@Composable
fun KvantFormSectionTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        modifier = modifier.padding(top = 16.dp, bottom = 8.dp),
        color = kvantContentColor(),
        fontFamily = Montserrat,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
    )
}
