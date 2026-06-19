package com.example.kvantroium.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kvantroium.R
import com.example.kvantroium.ui.theme.Montserrat
import com.example.kvantroium.ui.theme.kvantContentColor
import com.example.kvantroium.ui.util.copyTextToClipboard

@Composable
fun CopyableInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val contentColor = kvantContentColor()
    val context = LocalContext.current
    val displayValue = value.ifBlank { "—" }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ColumnInfo(label = label, value = displayValue, modifier = Modifier.weight(1f))
        if (value.isNotBlank()) {
            IconButton(
                onClick = { copyTextToClipboard(context, label, value) }
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_content_copy_24),
                    contentDescription = "Копировать $label",
                    tint = contentColor
                )
            }
        }
    }
}

@Composable
private fun ColumnInfo(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val contentColor = kvantContentColor()
    androidx.compose.foundation.layout.Column(modifier = modifier) {
        Text(
            text = label,
            color = contentColor.copy(alpha = 0.7f),
            fontFamily = Montserrat,
            fontSize = 13.sp
        )
        Text(
            text = value,
            color = contentColor,
            fontFamily = Montserrat,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
