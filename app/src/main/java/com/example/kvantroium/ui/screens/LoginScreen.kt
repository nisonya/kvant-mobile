package com.example.kvantroium.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.example.kvantroium.ui.components.kvantTopScreenInset
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.kvantroium.R
import com.example.kvantroium.api.ApiClient
import com.example.kvantroium.ui.components.KvantOutlinedField
import com.example.kvantroium.ui.theme.Montserrat
import com.example.kvantroium.ui.theme.MontserratMedium
import com.example.kvantroium.ui.theme.kvantContentColor
import com.example.kvantroium.ui.util.userMessage
import com.example.kvantroium.ui.util.validateLogin

private data class LoginRequest(
    val serverUrl: String,
    val login: String,
    val password: String
)

@Composable
fun LoginScreen(
    initialServerUrl: String,
    apiClient: ApiClient,
    onLoggedIn: () -> Unit
) {
    var serverUrl by remember { mutableStateOf(initialServerUrl) }
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loginRequest by remember { mutableStateOf<LoginRequest?>(null) }
    val isDarkTheme = isSystemInDarkTheme()

    LaunchedEffect(loginRequest) {
        val request = loginRequest ?: return@LaunchedEffect
        isLoading = true
        error = null
        runCatching {
            apiClient.login(request.serverUrl, request.login, request.password)
        }.onSuccess {
            loginRequest = null
            onLoggedIn()
        }.onFailure { throwable ->
            error = throwable.userMessage()
            isLoading = false
            loginRequest = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .kvantTopScreenInset()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.kvantorium),
                contentDescription = "Кванториум",
                modifier = Modifier
                    .padding(top = 100.dp)
                    .fillMaxWidth()
                    .height(91.dp),
                contentScale = ContentScale.Fit,
                colorFilter = if (isDarkTheme) {
                    ColorFilter.tint(kvantContentColor())
                } else {
                    null
                }
            )

            Spacer(modifier = Modifier.height(53.dp))

            KvantOutlinedField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                hint = "Адрес сервера",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )

            Spacer(modifier = Modifier.height(30.dp))

            KvantOutlinedField(
                value = login,
                onValueChange = { login = it },
                hint = "Логин"
            )

            Spacer(modifier = Modifier.height(30.dp))

            KvantOutlinedField(
                value = password,
                onValueChange = { password = it },
                hint = "Пароль",
                visualTransformation = PasswordVisualTransformation()
            )

            if (error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = error.orEmpty(), color = MaterialTheme.colorScheme.error, fontFamily = Montserrat)
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                enabled = !isLoading,
                onClick = {
                    error = validateLogin(serverUrl, login, password)
                    if (error != null) return@Button
                    loginRequest = LoginRequest(serverUrl, login, password)
                }
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("ВОЙТИ", fontFamily = MontserratMedium, fontSize = MaterialTheme.typography.labelLarge.fontSize)
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}
