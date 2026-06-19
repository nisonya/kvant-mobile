package com.example.kvantroium.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = LightBlue,
    onPrimary = DarkNavy,
    secondary = DarkCard,
    onSecondary = DarkOnSurface,
    tertiary = DarkChipBackground,
    onTertiary = DarkOnSurface,
    background = DarkNavy,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkCard,
    onSurfaceVariant = DarkOnSurface,
    outline = LightBlue.copy(alpha = 0.5f),
    error = ErrorRed,
    onError = Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary = DarkBlue,
    onPrimary = Color.White,
    secondary = Beige,
    onSecondary = DarkBlue,
    tertiary = Blue,
    onTertiary = DarkBlue,
    background = Light,
    onBackground = DarkBlue,
    surface = Light,
    onSurface = DarkBlue,
    surfaceVariant = Beige,
    onSurfaceVariant = DarkBlue,
    outline = DarkBlue.copy(alpha = 0.4f),
    error = ErrorRed,
    onError = Color.White,
)

@Composable
fun KvantroiumTheme(
    genderAccent: GenderAccent = GenderAccent.Unknown,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val kvantColors = genderAccentColors(genderAccent, darkTheme)

    CompositionLocalProvider(LocalKvantColors provides kvantColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
