package ru.coko.ege.domain.model

data class UserCredentials(
    val lastName: String,
    val passportSeries: String,
    val passportNumber: String
)

data class StudentProfile(
    val fullName: String,
    val schoolClass: String? = null,
    val school: String? = null,
    val schoolCode: String? = null,
    val area: String? = null
)

enum class ExamStatus {
    UPCOMING,
    COMPLETED,
    RESULT_READY,
    UNKNOWN
}

/**
 * Тип аттестации — определяется по коду предмета с сайта ЦОКО, а не по баллам
 * (баллы используются только для отображения "X из Y", шкалы у разных типов
 * пересекаются и сами по себе не надёжны для классификации).
 *
 * Правила (по суффиксу/префиксу кода предмета сайта ЦОКО):
 *   "-9"  в конце кода   → ОГЭ  (например "МАТП-9", "РУС-9")
 *   "-11" в конце кода   → ЕГЭ  (например "РУС-11", "ФИЗ-11")
 *   "_КЕГЭ" в коде       → КЕГЭ (компьютерный ЕГЭ, например "ИНФ_КЕГЭ-11")
 *   "_ГВЭ" в коде        → ГВЭ  (государственный выпускной экзамен)
 *   "СОЧИН"/"ИЗЛОЖ"      → Итоговое сочинение/изложение (допуск к ЕГЭ)
 *   "СОБ" (УСТНОЕ СОБ.)  → Итоговое собеседование (допуск к ОГЭ)
 */
enum class ExamType(val displayName: String) {
    OGE("ОГЭ"),
    EGE("ЕГЭ"),
    KEGE("КЕГЭ"),
    GVE("ГВЭ"),
    FINAL_ESSAY("Итоговое сочинение"),
    FINAL_INTERVIEW("Итоговое собеседование"),
    UNKNOWN("ГИА")
}

data class ExamCard(
    val id: String,
    val subjectName: String,
    val subjectCode: String,       // исходный код с сайта, нужен для определения ExamType и поиска max-балла
    val examType: ExamType,
    val date: String,
    val time: String? = null,
    val location: String? = null,
    val status: ExamStatus,
    val scoreDisplay: String? = null,
    val primaryScore: Int? = null,
    val maxScoreForSubject: Int? = null,
    val variant: String? = null
)

data class AnswerSheet(
    val title: String,
    val postBackTarget: String?
)

data class ExamResultDetail(
    val examId: String,
    val subjectName: String,
    val subjectCode: String,
    val examType: ExamType,
    val dateText: String? = null,
    val ppe: String? = null,
    val auditorium: String? = null,
    val variant: String? = null,
    val status: String? = null,
    val shortAnswerTasks: List<TaskAnswer> = emptyList(),
    val typeBTasks: String? = null,
    val typeCTasks: String? = null,
    val literacyCriterion: String? = null,
    val primaryScore: Int? = null,
    val finalScoreDisplay: String? = null,
    val maxScoreForSubject: Int? = null,
    val appeal: String? = null,
    val additionalInfo: String? = null,
    val answerSheets: List<AnswerSheet> = emptyList()
)

data class TaskAnswer(
    val taskNumber: String,
    val taskHint: String?,
    val answer: String,
    val scoreEarned: Int,
    val scoreMax: Int,
    val isExtended: Boolean = false // true для заданий части 2 (развёрнутый ответ)
)
