package com.example.kvantroium.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kvantroium.ui.theme.Montserrat
import com.example.kvantroium.ui.util.rememberNetworkAvailable

@Composable
fun NetworkStatusBanner(modifier: Modifier = Modifier) {
    val isOnline = rememberNetworkAvailable()
    if (isOnline) return
    Text(
        text = "Нет подключения к интернету",
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.error)
            .padding(vertical = 6.dp, horizontal = 12.dp),
        color = MaterialTheme.colorScheme.onError,
        fontFamily = Montserrat,
        fontSize = 13.sp,
        textAlign = TextAlign.Center
    )
}
