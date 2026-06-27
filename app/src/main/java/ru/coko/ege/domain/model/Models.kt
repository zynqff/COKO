package ru.coko.ege.domain.model

/**
 * Учётные данные пользователя для входа в личный кабинет ЦОКО.
 * Серия и номер паспорта + фамилия — именно так устроена авторизация на сайте coko.tomsk.ru.
 */
data class UserCredentials(
    val lastName: String,
    val passportSeries: String,
    val passportNumber: String
)

/**
 * Профиль выпускника, который удаётся вытащить со страницы личного кабинета.
 */
data class StudentProfile(
    val fullName: String,
    val schoolClass: String? = null,
    val school: String? = null
)

enum class ExamStatus {
    UPCOMING,       // Скоро
    COMPLETED,      // Экзамен сдан, ждём результат
    RESULT_READY,   // Результат опубликован
    UNKNOWN
}

/**
 * Карточка экзамена на главном экране — предмет, дата, статус, место проведения (ППЭ).
 */
data class ExamCard(
    val id: String,
    val subjectName: String,
    val date: String,
    val time: String? = null,
    val location: String? = null,
    val status: ExamStatus,
    val score: Int? = null,
    val maxScore: Int? = null
)

/**
 * Детализация по одному заданию КИМ.
 */
data class TaskScore(
    val taskNumber: String,
    val scoreEarned: Int,
    val scoreMax: Int,
    val criteria: List<CriterionScore> = emptyList()
)

data class CriterionScore(
    val code: String,    // К1, К2 ...
    val scoreEarned: Int,
    val scoreMax: Int
)

/**
 * Полная детализация результата экзамена по предмету.
 */
data class ExamResultDetail(
    val examId: String,
    val subjectName: String,
    val totalScore: Int,
    val maxScore: Int,
    val tasks: List<TaskScore>
)
