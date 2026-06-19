package com.example.kvantroium.ui.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import com.example.kvantroium.ui.components.FeatureScreenScaffold
import com.example.kvantroium.ui.theme.DarkBlue
import com.example.kvantroium.ui.theme.Montserrat

@Composable
fun PlaceholderScreen(title: String, message: String, onBack: () -> Unit) {
    FeatureScreenScaffold(title = title.uppercase(), onBack = onBack) {
        Text(message, color = DarkBlue, fontFamily = Montserrat, fontSize = 18.sp)
    }
}
