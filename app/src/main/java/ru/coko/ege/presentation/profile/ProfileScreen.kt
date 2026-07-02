package ru.coko.ege.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.coko.ege.presentation.main.MainViewModel

@Composable
fun ProfileScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onLoggedOut: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var darkTheme by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }

    val fullName = uiState.profile?.fullName ?: "Пользователь"
    val initials = fullName.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("")
    val classLabel = uiState.profile?.schoolClass?.let { "$it класс" } ?: "Участник ГИА"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.padding(top = 24.dp))
        Text("Профиль", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text("Данные участника ГИА", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

        Spacer(modifier = Modifier.padding(top = 20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF9333EA)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        initials.uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Spacer(modifier = Modifier.padding(top = 12.dp))
                Text(fullName, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)

                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFEEF2FF))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        classLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4338CA),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.padding(top = 16.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.padding(top = 14.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    InfoRow("Категория", "Выпускник текущего года")
                    Spacer(modifier = Modifier.padding(top = 8.dp))
                    InfoRow("Образовательное учреждение", uiState.profile?.school ?: "—")
                    if (uiState.profile?.schoolCode != null) {
                        Spacer(modifier = Modifier.padding(top = 8.dp))
                        InfoRow("Код ОО", uiState.profile?.schoolCode.orEmpty(), monospace = true)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.padding(top = 16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                SettingsRow("Уведомления о публикации баллов", notificationsEnabled) { notificationsEnabled = it }
                HorizontalDivider(color = Color(0xFFF1F5F9))
                SettingsRow("Использовать тёмную тему", darkTheme) { darkTheme = it }
            }
        }

        Spacer(modifier = Modifier.padding(top = 20.dp))

        Button(
            onClick = { viewModel.logout(onLoggedOut) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFF1F2),
                contentColor = Color(0xFFE11D48)
            )
        ) {
            Text("Выйти из личного кабинета", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.padding(bottom = 24.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String, monospace: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        Text(
            value,
            fontWeight = FontWeight.SemiBold,
            style = if (monospace) MaterialTheme.typography.bodyMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace) else MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun SettingsRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(end = 8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF10B981))
        )
    }
}
