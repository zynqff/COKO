package ru.coko.ege.presentation.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.coko.ege.domain.model.TaskScore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultDetailScreen(
    viewModel: ResultDetailViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.detail?.subjectName ?: "Детализация") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.padding(32.dp))
                }
                uiState.errorMessage != null -> {
                    Text(
                        text = uiState.errorMessage.orEmpty(),
                        color = Color(0xFFEF4444),
                        modifier = Modifier.padding(24.dp)
                    )
                }
                uiState.detail != null -> {
                    val detail = uiState.detail!!
                    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF))
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Итоговый балл", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${detail.totalScore} / ${detail.maxScore}",
                                    fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color(0xFF4F46E5)
                                )
                            }
                        }

                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 16.dp))

                        if (detail.tasks.isEmpty()) {
                            Text(
                                "Подробная разбивка по заданиям пока недоступна для этого экзамена.",
                                color = Color.Gray
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                items(detail.tasks) { task ->
                                    TaskRow(task)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(task: TaskScore) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Задание ${task.taskNumber}", fontWeight = FontWeight.SemiBold)
                Text(
                    "${task.scoreEarned}/${task.scoreMax}",
                    fontWeight = FontWeight.Bold,
                    color = if (task.scoreEarned >= task.scoreMax) Color(0xFF047857) else Color(0xFFB45309)
                )
            }
            if (task.criteria.isNotEmpty()) {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 6.dp))
                Text(
                    task.criteria.joinToString("  ·  ") { "${it.code}: ${it.scoreEarned}" },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}
