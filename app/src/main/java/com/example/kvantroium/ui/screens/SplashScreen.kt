package com.example.kvantroium.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.kvantroium.R
import com.example.kvantroium.ui.theme.kvantContentColor
import kotlinx.coroutines.delay

private const val FADE_IN_MS = 700
private const val HOLD_MS = 1800L

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var contentVisible by remember { mutableStateOf(false) }
    val isDarkTheme = isSystemInDarkTheme()

    LaunchedEffect(Unit) {
        contentVisible = true
        delay(FADE_IN_MS + HOLD_MS)
        onFinished()
    }

    val alpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(durationMillis = FADE_IN_MS),
        label = "splashFadeIn"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .alpha(alpha)
                .padding(horizontal = 32.dp)
                .widthIn(max = 360.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = null,
                modifier = Modifier.size(191.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(28.dp))
            Image(
                painter = painterResource(R.drawable.kvantorium),
                contentDescription = "Кванториум",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit,
                colorFilter = if (isDarkTheme) {
                    ColorFilter.tint(kvantContentColor())
                } else {
                    null
                }
            )
        }
    }
}
