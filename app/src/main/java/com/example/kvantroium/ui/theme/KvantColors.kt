package com.example.kvantroium.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val DarkNavy = Color(0xFF0D1B2A)
val DarkSurface = Color(0xFF152536)
val DarkCard = Color(0xFF1E3348)
val DarkInnerCard = Color(0xFF253447)
val DarkOnSurface = Color(0xFFE8EDF2)
val DarkChipBackground = Color(0xFF2A4A6B)
val DarkLink = Color(0xFF9BB4E0)

@Immutable
data class KvantExtendedColors(
    val card: Color,
    val innerCard: Color,
    val chipBackground: Color,
    val chipContent: Color,
    val chipBorder: Color,
    val link: Color,
    val segmentSelected: Color,
    val segmentUnselected: Color,
    val segmentBorder: Color,
    val fabBackground: Color,
    val fabIcon: Color,
    val watermark: Color,
    val homeEditBackground: Color,
    val homeEditIcon: Color,
)

val LocalKvantColors = staticCompositionLocalOf {
    KvantExtendedColors(
        card = Beige,
        innerCard = Light,
        chipBackground = Blue,
        chipContent = DarkBlue,
        chipBorder = DarkBlue,
        link = LightBlue,
        segmentSelected = Blue,
        segmentUnselected = Light,
        segmentBorder = DarkBlue,
        fabBackground = Color.White,
        fabIcon = DarkBlue,
        watermark = Light,
        homeEditBackground = Color.White,
        homeEditIcon = DarkBlue,
    )
}

val LightKvantColors = KvantExtendedColors(
    card = Beige,
    innerCard = Light,
    chipBackground = Blue,
    chipContent = DarkBlue,
    chipBorder = DarkBlue,
    link = LightBlue,
    segmentSelected = Blue,
    segmentUnselected = Light,
    segmentBorder = DarkBlue,
    fabBackground = Color.White,
    fabIcon = DarkBlue,
    watermark = Light,
    homeEditBackground = Color.White,
    homeEditIcon = DarkBlue,
)

val DarkKvantColors = KvantExtendedColors(
    card = DarkCard,
    innerCard = DarkInnerCard,
    chipBackground = DarkChipBackground,
    chipContent = DarkOnSurface,
    chipBorder = LightBlue,
    link = DarkLink,
    segmentSelected = DarkChipBackground,
    segmentUnselected = DarkSurface,
    segmentBorder = LightBlue,
    fabBackground = DarkInnerCard,
    fabIcon = DarkOnSurface,
    watermark = DarkNavy,
    homeEditBackground = Color.White,
    homeEditIcon = DarkBlue,
)

val FemaleLightKvantColors = KvantExtendedColors(
    card = Beige,
    innerCard = Light,
    chipBackground = Beige,
    chipContent = DarkBlue,
    chipBorder = DarkBlue,
    link = LightBlue,
    segmentSelected = Beige,
    segmentUnselected = Light,
    segmentBorder = DarkBlue,
    fabBackground = Color.White,
    fabIcon = DarkBlue,
    watermark = Beige.copy(alpha = 0.35f),
    homeEditBackground = Color.White,
    homeEditIcon = DarkBlue,
)

@Composable
fun kvantColors(): KvantExtendedColors = LocalKvantColors.current

@Composable
fun kvantContentColor(): Color = MaterialTheme.colorScheme.onBackground
