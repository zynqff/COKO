package ru.coko.ege.presentation.results

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.coko.ege.domain.model.ExamResultDetail
import ru.coko.ege.domain.model.TaskAnswer
import ru.coko.ege.presentation.common.scoreColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultDetailScreen(
    viewModel: ResultDetailViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.detail?.subjectName ?: "Детализация", fontWeight = FontWeight.Black)
                        uiState.detail?.examType?.let {
                            Text(
                                it.displayName.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                },
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
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.errorMessage != null -> {
                    Text(
                        text = uiState.errorMessage.orEmpty(),
                        color = Color(0xFFEF4444),
                        modifier = Modifier.padding(24.dp)
                    )
                }
                uiState.detail != null -> {
                    DetailContent(
                        detail = uiState.detail!!,
                        onOpenOnSite = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(viewModel.resultsPageUrl()))
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailContent(detail: ExamResultDetail, onOpenOnSite: () -> Unit) {
    var selectedPart by remember { mutableIntStateOf(1) }
    val hasPart1 = detail.shortAnswerTasks.isNotEmpty()
    val hasPart2 = detail.typeBTasks != null || detail.typeCTasks != null || detail.literacyCriterion != null

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item { ScoreBadgeCard(detail) }

        item { InfoCard(detail) }

        if (hasPart1 && hasPart2) {
            item {
                PartSwitcher(selectedPart = selectedPart, onSelect = { selectedPart = it })
            }
        }

        if (hasPart1 && (selectedPart == 1 || !hasPart2)) {
            items(detail.shortAnswerTasks) { task -> TaskRow(task) }
        }

        if (hasPart2 && (selectedPart == 2 || !hasPart1)) {
            if (detail.typeBTasks != null || detail.typeCTasks != null) {
                item { TypeTasksCard(detail) }
            }
            detail.literacyCriterion?.let {
                item { LabeledRow("Критерий грамотности", it) }
            }
        }

        if (!detail.additionalInfo.isNullOrBlank()) {
            item { AdditionalInfoCard(detail.additionalInfo) }
        }

        detail.appeal?.let {
            item { LabeledRow("Апелляция", it) }
        }

        if (detail.answerSheets.isNotEmpty()) {
            item { AnswerSheetsCard(detail, onOpenOnSite) }
        }
    }
}

/** Карточка с первичным баллом и круглым бейджем итоговой отметки — по образцу прототипа. */
@Composable
private fun ScoreBadgeCard(detail: ExamResultDetail) {
    val colors = scoreColors(detail.finalScoreDisplay)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Набранные первичные баллы", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                val primaryText = if (detail.primaryScore != null && detail.maxScoreForSubject != null) {
                    "${detail.primaryScore} / ${detail.maxScoreForSubject}"
                } else {
                    detail.primaryScore?.toString() ?: "—"
                }
                Text(primaryText, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
            }

            if (detail.finalScoreDisplay != null) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(colors.background),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        detail.finalScoreDisplay,
                        color = colors.foreground,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoCard(detail: ExamResultDetail) {
    Column {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                detail.dateText?.let { InfoRow("Дата", it) }
                detail.variant?.let { InfoRow("Вариант", it) }
                detail.ppe?.let { InfoRow("ППЭ", it) }
                detail.auditorium?.let { InfoRow("Аудитория*", it) }
                detail.status?.let { InfoRow("Статус", it) }
            }
        }

        if (detail.auditorium != null) {
            Text(
                "* Данные об аудитории и месте сообщаются на входе в ППЭ. " +
                    "На сайте отображаются после окончания экзамена.",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp)
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

/** Переключатель "Часть 1 / Часть 2" — пилюля как в прототипе. */
@Composable
private fun PartSwitcher(selectedPart: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF1F5F9))
            .padding(4.dp)
    ) {
        PartTab("Часть 1", selectedPart == 1, Modifier.weight(1f)) { onSelect(1) }
        PartTab("Часть 2", selectedPart == 2, Modifier.weight(1f)) { onSelect(2) }
    }
}

@Composable
private fun PartTab(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color = if (selected) Color(0xFF1E293B) else Color.Gray
        )
    }
}

@Composable
private fun LabeledRow(label: String, value: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
            Text(value, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun TaskRow(task: TaskAnswer) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Задание ${task.taskNumber}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                task.taskHint?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Text("Ответ: ${task.answer}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${task.scoreEarned}/${task.scoreMax}",
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.padding(start = 6.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (task.scoreEarned >= task.scoreMax) Color(0xFF10B981) else Color(0xFFF59E0B))
                )
            }
        }
    }
}

@Composable
private fun TypeTasksCard(detail: ExamResultDetail) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            detail.typeBTasks?.let {
                Text("Задания типа B", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Text(it, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            if (detail.typeBTasks != null && detail.typeCTasks != null) {
                Spacer(modifier = Modifier.padding(top = 10.dp))
            }
            detail.typeCTasks?.let {
                Text("Задания типа C", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Text(it, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun AdditionalInfoCard(additionalInfo: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB))) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Дополнительная информация", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.padding(top = 6.dp))
            Text(additionalInfo, style = MaterialTheme.typography.bodySmall, color = Color(0xFF92400E))
        }
    }
}

@Composable
private fun AnswerSheetsCard(detail: ExamResultDetail, onOpenOnSite: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Бланки *", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.padding(top = 4.dp))
            Text(
                "${detail.answerSheets.size} бланк(а/ов) доступны для просмотра на сайте ЦОКО",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.padding(top = 8.dp))
            detail.answerSheets.forEachIndexed { index, sheet ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Description,
                        contentDescription = null,
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.padding(end = 8.dp).size(16.dp)
                    )
                    Text(sheet.title, style = MaterialTheme.typography.bodySmall)
                }
                if (index != detail.answerSheets.lastIndex) {
                    Spacer(modifier = Modifier.padding(top = 4.dp))
                }
            }
            Spacer(modifier = Modifier.padding(top = 10.dp))
            TextButton(onClick = onOpenOnSite) {
                Text("Открыть на сайте ЦОКО →")
            }
            Text(
                "* просмотр бланков доступен только на сайте",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}
