package com.example.kvantroium.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.kvantroium.ui.components.kvantTopScreenInset
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kvantroium.R
import com.example.kvantroium.ui.theme.Beige
import com.example.kvantroium.ui.theme.DarkBlue
import com.example.kvantroium.ui.theme.Gothic
import com.example.kvantroium.ui.theme.Light
import com.example.kvantroium.ui.theme.Montserrat

@Composable
fun FeatureScreenScaffold(
    title: String,
    background: androidx.compose.ui.graphics.Color = Light,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .verticalScroll(rememberScrollState())
            .kvantTopScreenInset()
            .padding(horizontal = 15.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.round_arrow_back_24),
                contentDescription = "Назад",
                modifier = Modifier
                    .size(40.dp)
                    .clickable(onClick = onBack),
                tint = DarkBlue
            )
        }
        Text(
            text = title,
            modifier = Modifier.padding(top = 12.dp, bottom = 18.dp),
            color = DarkBlue,
            fontFamily = Gothic,
            fontSize = 30.sp
        )
        content()
    }
}

@Composable
fun ProfileField(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = DarkBlue,
            fontSize = 20.sp,
            fontFamily = Montserrat,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            modifier = Modifier.padding(top = 7.dp)
        )
        Text(
            text = value.ifBlank { "—" },
            color = DarkBlue,
            fontSize = 20.sp,
            fontFamily = Montserrat,
            modifier = Modifier.padding(start = 25.dp, top = 5.dp)
        )
    }
}

@Composable
fun ProfileScreenBackground(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Beige)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 15.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        content()
        Spacer(modifier = Modifier.height(50.dp))
    }
}
