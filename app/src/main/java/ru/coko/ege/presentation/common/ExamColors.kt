package ru.coko.ege.presentation.common

import androidx.compose.ui.graphics.Color
import ru.coko.ege.domain.model.ExamType

/** Цвета бейджа типа экзамена (КЕГЭ/ОГЭ/ЕГЭ/...), по образцу дизайн-прототипа. */
data class ExamTypeColors(val background: Color, val foreground: Color)

fun examTypeColors(type: ExamType): ExamTypeColors = when (type) {
    ExamType.KEGE -> ExamTypeColors(Color(0xFFEEF2FF), Color(0xFF4338CA))   // indigo
    ExamType.EGE -> ExamTypeColors(Color(0xFFF1F5F9), Color(0xFF475569))    // slate
    ExamType.OGE -> ExamTypeColors(Color(0xFFF1F5F9), Color(0xFF475569))    // slate
    ExamType.GVE -> ExamTypeColors(Color(0xFFF1F5F9), Color(0xFF475569))    // slate
    ExamType.FINAL_INTERVIEW -> ExamTypeColors(Color(0xFFF5F3FF), Color(0xFF7E22CE)) // purple
    ExamType.FINAL_ESSAY -> ExamTypeColors(Color(0xFFF5F3FF), Color(0xFF7E22CE))     // purple
    ExamType.UNKNOWN -> ExamTypeColors(Color(0xFFF1F5F9), Color(0xFF475569))
}

/** Цвета круглого/овального бейджа с оценкой — зависят от итоговой школьной отметки (1-5) или "Зачёт". */
data class ScoreColors(val background: Color, val foreground: Color)

fun scoreColors(finalScoreDisplay: String?): ScoreColors {
    val normalized = finalScoreDisplay?.trim()?.lowercase()
    val numericMark = finalScoreDisplay?.trim()?.toIntOrNull()

    return when {
        normalized == "зачёт" || normalized == "зачет" || numericMark == 5 ->
            ScoreColors(Color(0xFFD1FAE5), Color(0xFF065F46)) // emerald
        numericMark == 4 -> ScoreColors(Color(0xFFE0F2FE), Color(0xFF075985)) // sky
        numericMark == 3 -> ScoreColors(Color(0xFFFEF3C7), Color(0xFF92400E)) // amber
        numericMark == 2 || normalized == "незачёт" || normalized == "незачет" ->
            ScoreColors(Color(0xFFFFE4E6), Color(0xFF9F1239)) // rose
        else -> ScoreColors(Color(0xFFF1F5F9), Color(0xFF475569)) // neutral fallback
    }
}
