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
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.coko.ege.domain.model.ExamCard
import ru.coko.ege.domain.model.ExamStatus

@Composable
fun ResultsScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onOpenDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val resultsOnly = uiState.exams.filter { it.status == ExamStatus.RESULT_READY }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 24.dp))
        Text(
            text = "Результаты",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black
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
            ScoreBadge(score = exam.score, maxScore = exam.maxScore ?: 100)

            Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                Text(exam.subjectName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(exam.date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }

            OutlinedButton(onClick = onClick) {
                Text("Подробнее")
            }
        }
    }
}

@Composable
private fun ScoreBadge(score: Int?, maxScore: Int) {
    val isHigh = score != null && score >= (maxScore * 0.7)
    val bg = if (isHigh) Color(0xFFD1FAE5) else Color(0xFFFEF3C7)
    val fg = if (isHigh) Color(0xFF047857) else Color(0xFFB45309)

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = score?.toString() ?: "—",
            color = fg,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.titleMedium
        )
    }
}
