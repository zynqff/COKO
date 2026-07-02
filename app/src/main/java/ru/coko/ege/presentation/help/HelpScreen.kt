package ru.coko.ege.presentation.help

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class FaqItem(val question: String, val answer: String)

private val appealFaq = listOf(
    FaqItem(
        "Я не согласен с выставленными баллами. Как я могу оспорить результаты?",
        "Вы можете подать апелляцию о несогласии с выставленными баллами. " +
            "Апелляция подаётся в течение двух рабочих дней, следующих за официальным " +
            "днём объявления результатов экзамена по соответствующему предмету. " +
            "Обратитесь в свою школу (или в место регистрации на экзамен) для оформления заявления."
    ),
    FaqItem(
        "Где я могу увидеть свои отсканированные бланки ответов и результаты выполнения заданий с развёрнутым ответом?",
        "Скан-копии бланков ваших ответов (бланки №1 и №2), а также подробные оценки " +
            "экспертов по критериям проверки развёрнутых ответов доступны в полной " +
            "веб-версии личного кабинета на официальном портале ЦОКО Томской области."
    ),
    FaqItem(
        "Что делать, если результат не появился вовремя?",
        "Иногда обработка результатов задерживается. Подождите официального " +
            "объявления через Департамент общего образования Томской области " +
            "или обратитесь в свою школу за уточнением."
    )
)

@Composable
fun HelpScreen() {
    var message by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 24.dp))
        Text("Помощь", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text(
            "Ответы на частые вопросы участников ГИА",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(appealFaq) { item -> AccordionItem(item) }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Техподдержка",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 8.dp))
                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it },
                            placeholder = { Text("Опишите вашу проблему...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))
                        Button(
                            onClick = { /* TODO: интеграция с Telegram-ботом/почтой поддержки */ },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                        ) {
                            Text("Отправить обращение")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccordionItem(item: FaqItem) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item.question, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }
            if (expanded) {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 8.dp))
                Text(item.answer, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}
