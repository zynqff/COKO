package ru.coko.ege.presentation.privacy

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Конфиденциальность") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Политика обработки персональных данных",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer()
            Text(
                "1. Введённые вами паспортные данные и фамилия используются " +
                    "исключительно для формирования запроса к официальному сайту " +
                    "Центра оценки качества образования Томской области (coko.tomsk.ru) " +
                    "и не отправляются никаким другим серверам.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer()
            Text(
                "2. Данные хранятся локально на вашем устройстве в зашифрованном " +
                    "виде с использованием Android Keystore. Передача данных на сайт " +
                    "ЦОКО происходит по защищённому каналу (HTTPS).",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer()
            Text(
                "3. Приложение является сторонним клиентом для удобного просмотра " +
                    "результатов и не имеет официальной связи с ЦОКО Томской области. " +
                    "За корректность и актуальность данных отвечает сайт coko.tomsk.ru.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer()
            Text(
                "4. Нажимая кнопку «Войти», вы соглашаетесь с условиями обработки, " +
                    "безопасности и локального хранения данных, описанными выше.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun Spacer() {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))
}
