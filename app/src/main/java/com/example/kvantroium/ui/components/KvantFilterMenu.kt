package com.example.kvantroium.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kvantroium.R
import com.example.kvantroium.ui.theme.Montserrat
import com.example.kvantroium.ui.theme.kvantColors
import com.example.kvantroium.ui.theme.kvantContentColor

@Composable
fun <T> KvantFilterChipMenu(
    chipLabel: String,
    selectedLabel: String,
    isActive: Boolean,
    options: List<Pair<T, String>>,
    selectedValue: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    valueEquals: (T, T) -> Boolean = { a, b -> a == b }
) {
    var expanded by remember { mutableStateOf(false) }
    val kvant = kvantColors()
    val contentColor = kvantContentColor()

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable { expanded = true },
            shape = RoundedCornerShape(20.dp),
            color = if (isActive) kvant.chipBackground else kvant.card,
            border = BorderStroke(1.dp, kvant.segmentBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.widthIn(max = 140.dp)) {
                    Text(
                        text = chipLabel,
                        color = contentColor.copy(alpha = 0.65f),
                        fontFamily = Montserrat,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = selectedLabel,
                        color = contentColor,
                        fontFamily = Montserrat,
                        fontSize = 13.sp,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.round_arrow_drop_down_24),
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(if (expanded) 180f else 0f),
                    tint = contentColor
                )
            }
        }

        KvantStyledDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEachIndexed { index, (value, label) ->
                val isSelected = valueEquals(value, selectedValue)
                KvantDropdownMenuItem(
                    label = label,
                    isSelected = isSelected,
                    onClick = {
                        expanded = false
                        onSelected(value)
                    }
                )
                if (index < options.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )
                }
            }
        }
    }
}

@Composable
fun KvantStyledDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val kvant = kvantColors()

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
            .widthIn(min = 240.dp, max = 320.dp)
            .background(kvant.innerCard, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = 360.dp)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 6.dp),
            content = content
        )
    }
}

@Composable
private fun KvantDropdownMenuItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val kvant = kvantColors()
    val contentColor = kvantContentColor()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isSelected) kvant.chipBackground.copy(alpha = 0.45f) else kvant.innerCard
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = contentColor,
            fontFamily = Montserrat,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            lineHeight = 18.sp
        )
        if (isSelected) {
            Icon(
                painter = painterResource(R.drawable.round_check_24),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = contentColor
            )
        }
    }
}
