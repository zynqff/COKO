package ru.coko.ege.presentation.splash

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.coko.ege.data.repository.CokoRepository

private val SplashBackground = Color(0xFF4F46E5)
private val SplashTextSecondary = Color(0xFFC7D2FE)

@Composable
fun SplashScreen(
    viewModel: SplashViewModel = hiltViewModel(),
    onNavigateToLogin: () -> Unit,
    onNavigateToMain: (CokoRepository.DashboardData) -> Unit
) {
    val status by viewModel.status.collectAsStateWithLifecycle()

    LaunchedEffect(status) {
        when (val s = status) {
            is SplashStatus.NoCredentialsFound -> onNavigateToLogin()
            is SplashStatus.Success -> onNavigateToMain(s.dashboard)
            is SplashStatus.Failed -> onNavigateToLogin()
            else -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(96.dp),
                    color = Color.White,
                    strokeWidth = 4.dp,
                    trackColor = Color.White.copy(alpha = 0.25f)
                )
                Icon(
                    imageVector = Icons.Filled.Description,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 32.dp))

            Text(
                text = "ЦОКО Томск",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 8.dp))

            AnimatedContent(
                targetState = statusText(status),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "splash_status_text"
            ) { text ->
                Text(
                    text = text,
                    color = SplashTextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private fun statusText(status: SplashStatus): String = when (status) {
    is SplashStatus.CheckingStorage -> "Ищем сохранённые данные..."
    is SplashStatus.NoCredentialsFound -> "Переходим ко входу..."
    is SplashStatus.CredentialsFound -> "Данные найдены!"
    is SplashStatus.ConnectingToServer -> "Подключение к серверу..."
    is SplashStatus.Success -> "Данные обновлены!"
    is SplashStatus.Failed -> "Не удалось обновить данные..."
}
