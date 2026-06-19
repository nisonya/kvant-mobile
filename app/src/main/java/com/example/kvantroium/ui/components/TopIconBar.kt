package com.example.kvantroium.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.kvantroium.R
import com.example.kvantroium.ui.theme.kvantContentColor

@Composable
fun TopIconBar(
    onBack: (() -> Unit)? = null,
    onTrailingClick: (() -> Unit)? = null,
    trailingIcon: Painter? = null,
    trailingContentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .kvantTopScreenInset(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            Icon(
                painter = painterResource(R.drawable.round_arrow_back_24),
                contentDescription = "Назад",
                modifier = Modifier
                    .size(40.dp)
                    .clickable(onClick = onBack),
                tint = kvantContentColor()
            )
        } else {
            Box(modifier = Modifier.size(40.dp))
        }

        if (onTrailingClick != null && trailingIcon != null) {
            Icon(
                painter = trailingIcon,
                contentDescription = trailingContentDescription,
                modifier = Modifier
                    .size(40.dp)
                    .clickable(onClick = onTrailingClick),
                tint = kvantContentColor()
            )
        } else {
            Box(modifier = Modifier.size(40.dp))
        }
    }
}

@Composable
fun TopEndIcon(
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = kvantContentColor()
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier
                .size(50.dp)
                .clickable(onClick = onClick),
            tint = tint
        )
    }
}
