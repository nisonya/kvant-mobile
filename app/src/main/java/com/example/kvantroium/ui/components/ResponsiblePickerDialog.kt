package com.example.kvantroium.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kvantroium.features.events.EmployeeOption
import com.example.kvantroium.ui.theme.Montserrat
import com.example.kvantroium.ui.theme.kvantContentColor

@Composable
fun ResponsiblePickerDialog(
    employees: List<EmployeeOption>,
    selectedIds: Set<Int>,
    onDismiss: () -> Unit,
    onConfirm: (Set<Int>) -> Unit
) {
    var draftSelection by remember(selectedIds) { mutableStateOf(selectedIds) }
    var search by remember { mutableStateOf("") }
    val contentColor = kvantContentColor()
    val filtered = remember(employees, search) {
        val query = search.trim().lowercase()
        if (query.isEmpty()) employees
        else employees.filter { it.name.lowercase().contains(query) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Ответственные", fontFamily = Montserrat, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Поиск", fontFamily = Montserrat) },
                    singleLine = true,
                    colors = kvantTextFieldColors()
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filtered, key = { it.id }) { employee ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = employee.id in draftSelection,
                                onCheckedChange = { checked ->
                                    draftSelection = if (checked) {
                                        draftSelection + employee.id
                                    } else {
                                        draftSelection - employee.id
                                    }
                                }
                            )
                            Text(
                                text = employee.name,
                                color = contentColor,
                                fontFamily = Montserrat,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(draftSelection) }) {
                Text("Готово", fontFamily = Montserrat)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", fontFamily = Montserrat)
            }
        }
    )
}
