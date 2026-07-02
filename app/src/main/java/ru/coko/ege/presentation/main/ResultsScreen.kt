package ru.coko.ege.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.coko.ege.domain.model.ExamCard
import ru.coko.ege.domain.model.ExamStatus
import ru.coko.ege.presentation.common.examTypeColors
import ru.coko.ege.presentation.common.scoreColors

@Composable
fun ResultsScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onOpenDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val resultsOnly = uiState.exams.filter { it.status == ExamStatus.RESULT_READY }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 24.dp))
        Text("Результаты", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text(
            "Опубликованные баллы и оценки",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 16.dp))

        if (resultsOnly.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Результаты пока не опубликованы", color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(resultsOnly, key = { it.id }) { exam ->
                    ResultCardItem(exam, onClick = { onOpenDetail(exam.id) })
                }
            }
        }
    }
}

@Composable
private fun ResultCardItem(exam: ExamCard, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScoreBadge(scoreDisplay = exam.scoreDisplay, primaryScore = exam.primaryScore, maxScore = exam.maxScoreForSubject)

            Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                Text(exam.subjectName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                val variantSuffix = exam.variant?.let { " · Вариант $it" } ?: ""
                Text(
                    "${exam.date}$variantSuffix",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                ExamTypeTag(exam)
            }

            OutlinedButton(onClick = onClick) {
                Text("Подробнее")
            }
        }
    }
}

@Composable
private fun ExamTypeTag(exam: ExamCard) {
    val colors = examTypeColors(exam.examType)
    Box(
        modifier = Modifier
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(colors.background)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            exam.examType.displayName.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = colors.foreground
        )
    }
}

/**
 * Двустрочный бейдж: сверху — итоговая отметка ("5", "Зачёт"), снизу мелким
 * шрифтом — первичный балл вида "31/33" (если известны и балл, и максимум
 * по предмету). Цвет зависит от отметки (см. presentation.common.scoreColors),
 * не от процента, так как шкалы у разных предметов/типов разные.
 */
@Composable
private fun ScoreBadge(scoreDisplay: String?, primaryScore: Int?, maxScore: Int?) {
    val colors = scoreColors(scoreDisplay)
    val isLongText = (scoreDisplay?.length ?: 0) > 4
    val fraction = if (primaryScore != null && maxScore != null) "$primaryScore/$maxScore" else null

    Box(
        modifier = Modifier
            .size(width = if (isLongText) 84.dp else 56.dp, height = 56.dp)
            .clip(if (isLongText) RoundedCornerShape(16.dp) else CircleShape)
            .background(colors.background)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = scoreDisplay ?: "—",
                color = colors.foreground,
                fontWeight = FontWeight.Black,
                style = if (isLongText) MaterialTheme.typography.labelSmall else MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            if (fraction != null) {
                Text(
                    text = fraction,
                    color = colors.foreground.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
