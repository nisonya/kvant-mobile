package com.example.kvantroium.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kvantroium.R
import com.example.kvantroium.ui.theme.Gothic
import com.example.kvantroium.ui.theme.Montserrat
import com.example.kvantroium.ui.theme.kvantColors
import com.example.kvantroium.ui.theme.kvantContentColor

private val ScreenMaxWidth = 640.dp
private val ScreenHorizontalPadding = 16.dp
private val ScreenTopContentPadding = 12.dp

fun Modifier.kvantTopScreenInset(): Modifier =
    statusBarsPadding().padding(top = ScreenTopContentPadding)

fun Modifier.kvantBottomScreenInset(extra: Dp = 32.dp): Modifier =
    navigationBarsPadding().padding(bottom = extra)

@Composable
fun KvantScreenContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = ScreenMaxWidth)
                .fillMaxSize()
                .align(Alignment.TopCenter)
                .kvantTopScreenInset()
                .padding(horizontal = ScreenHorizontalPadding),
            content = content
        )
    }
}

@Composable
fun KvantScreenScaffold(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val contentColor = kvantContentColor()
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = ScreenMaxWidth)
                .fillMaxSize()
                .align(Alignment.TopCenter)
                .kvantTopScreenInset()
                .padding(horizontal = ScreenHorizontalPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.round_arrow_back_24),
                    contentDescription = "Назад",
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(onClick = onBack),
                    tint = contentColor
                )
                if (title != null) {
                    Text(
                        text = title,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp),
                        color = contentColor,
                        fontFamily = Gothic,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            content()
        }
    }
}

@Composable
fun KvantSegmentedRow(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val kvant = kvantColors()
    val contentColor = kvantContentColor()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(50),
        color = kvant.segmentUnselected,
        border = BorderStroke(1.dp, kvant.segmentBorder)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, label ->
                KvantSegment(
                    text = label,
                    selected = index == selectedIndex,
                    onClick = { onSelected(index) },
                    modifier = Modifier.weight(1f),
                    selectedColor = kvant.segmentSelected,
                    unselectedColor = kvant.segmentUnselected,
                    contentColor = contentColor
                )
            }
        }
    }
}

@Composable
private fun RowScope.KvantSegment(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    selectedColor: androidx.compose.ui.graphics.Color,
    unselectedColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(50)
    Surface(
        modifier = modifier
            .height(40.dp)
            .clip(shape)
            .clickable(onClick = onClick),
        color = if (selected) selectedColor else unselectedColor,
        shape = shape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected) {
                Icon(
                    painter = painterResource(R.drawable.outline_check_24),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = contentColor
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(4.dp))
            }
            Text(
                text = text,
                color = contentColor,
                fontFamily = Montserrat,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun KvantCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    cornerRadius: Dp = 10.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val kvant = kvantColors()
    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = kvant.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = content
    )
}

@Composable
fun KvantInnerCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val kvant = kvantColors()
    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = kvant.innerCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = content
    )
}

@Composable
fun PersonChip(name: String, modifier: Modifier = Modifier) {
    val kvant = kvantColors()
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = kvant.chipBackground,
        border = BorderStroke(1.dp, kvant.chipBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.round_person),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = kvant.chipContent
            )
            Text(
                text = name,
                color = kvant.chipContent,
                fontFamily = Montserrat,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PersonChipsRow(
    names: List<String>,
    modifier: Modifier = Modifier
) {
    if (names.isEmpty()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        names.forEach { name ->
            PersonChip(name = name)
        }
    }
}

@Composable
fun KvantDetailSection(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val contentColor = kvantContentColor()
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = contentColor,
            fontSize = 20.sp,
            fontFamily = Montserrat,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 7.dp, bottom = 4.dp)
        )
        Text(
            text = value.ifBlank { "—" },
            color = contentColor,
            fontSize = 18.sp,
            fontFamily = Montserrat,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)
        )
    }
}

@Composable
fun kvantTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = kvantContentColor(),
    unfocusedTextColor = kvantContentColor(),
    focusedBorderColor = kvantContentColor(),
    unfocusedBorderColor = kvantContentColor().copy(alpha = 0.4f),
    cursorColor = kvantContentColor(),
    focusedPlaceholderColor = kvantContentColor().copy(alpha = 0.6f),
    unfocusedPlaceholderColor = kvantContentColor().copy(alpha = 0.6f),
)
