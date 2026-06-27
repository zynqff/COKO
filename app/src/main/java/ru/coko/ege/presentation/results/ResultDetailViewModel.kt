package ru.coko.ege.presentation.results

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.nodes.Document
import ru.coko.ege.data.remote.CokoWebClient
import ru.coko.ege.data.remote.ServerErrorException
import ru.coko.ege.domain.model.CriterionScore
import ru.coko.ege.domain.model.ExamResultDetail
import ru.coko.ege.domain.model.TaskScore
import java.io.IOException
import javax.inject.Inject

data class ResultDetailUiState(
    val isLoading: Boolean = true,
    val detail: ExamResultDetail? = null,
    val errorMessage: String? = null
)

/**
 * Загружает детализацию по конкретному результату экзамена.
 *
 * =====================================================================================
 * ВАЖНО — это самая неопределённая часть парсинга, прочти перед правкой
 * =====================================================================================
 *
 * На странице res_exams.aspx кнопка "Просмотреть" — НЕ обычная ссылка на отдельный
 * URL, а AJAX partial postback внутри той же страницы:
 *
 *   <a href="javascript:__doPostBack('ctl00$ContentPlaceHolder1$lb_<guid>','')">Просмотреть</a>
 *
 * После такого постбэка сервер подгружает содержимое в таблицу
 * #ctl00_ContentPlaceHolder1_DetailsTable (она видна в HTML res_exams.aspx,
 * но в момент снятия примера была пустой — открыта не была). Поэтому:
 *
 *   1. examId здесь — это сохранённое имя цели постбэка (detailLinkTarget),
 *      которое CokoHtmlParser.parseResultExams() положил в ExamCard.id.
 *   2. CokoWebClient.openResultDetail(examId) выполняет этот постбэк и
 *      возвращает HTML всей страницы (так как это обычный POST, а не
 *      настоящий fetch XHR — ASP.NET UpdatePanel в режиме "deferred"
 *      при обычном POST без X-MicrosoftAjax-заголовка отдаёт полную страницу
 *      с уже заполненной #DetailsTable).
 *   3. parseDetail() ищет данные именно в #ctl00_ContentPlaceHolder1_DetailsTable.
 *      Реальные подзаголовки/колонки этой таблицы (разбивка по заданиям,
 *      критерии К1/К2 для части с развёрнутым ответом) НЕ были видны в
 *      присланном примере HTML — там она была пустой, так как тестовый вход
 *      не нажимал "Просмотреть". Поэтому селекторы ниже — это первое
 *      приближение по типовой структуре таких таблиц ЦОКО/РЦОИ.
 *
 * ЕСЛИ ПОСЛЕ ЗАПУСКА ЭКРАН ДЕТАЛИЗАЦИИ ПУСТОЙ ИЛИ С ОШИБКОЙ:
 * Самый быстрый способ поправить — открыть "Просмотреть" в браузере на
 * компьютере (с DevTools), посмотреть итоговый HTML #DetailsTable и
 * прислать мне его (как ты прислал три предыдущих страницы) — я перепишу
 * именно parseDetail() без переделки остальной архитектуры.
 */
@HiltViewModel
class ResultDetailViewModel @Inject constructor(
    private val webClient: CokoWebClient,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val examId: String = savedStateHandle.get<String>("examId")
        ?.let { ru.coko.ege.presentation.navigation.Routes.decodeExamId(it) }
        ?: ""

    private val _uiState = MutableStateFlow(ResultDetailUiState())
    val uiState: StateFlow<ResultDetailUiState> = _uiState

    init {
        loadDetail()
    }

    private fun loadDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val detail = withContext(Dispatchers.IO) {
                    val doc = webClient.openResultDetail(examId)
                    parseDetail(doc)
                }
                _uiState.update { it.copy(isLoading = false, detail = detail) }
            } catch (e: ServerErrorException) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            } catch (e: IOException) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Нет подключения к интернету") }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Не удалось разобрать детализацию. Структура страницы " +
                            "отличается от ожидаемой — нужно прислать разработчику HTML открытой " +
                            "детализации для уточнения парсера. (${e.message})"
                    )
                }
            }
        }
    }

    /**
     * Парсит таблицу #ctl00_ContentPlaceHolder1_DetailsTable после постбэка.
     * См. предупреждение в комментарии класса выше насчёт неопределённости
     * точной структуры этой таблицы.
     */
    private fun parseDetail(doc: Document): ExamResultDetail {
        val detailsTable = doc.selectFirst("#ctl00_ContentPlaceHolder1_DetailsTable")

        if (detailsTable == null || detailsTable.select("tr").isEmpty()) {
            // Таблица есть, но пустая — постбэк не вернул содержимое.
            // Возвращаем "пустую" детализацию без заданий, чтобы UI показал
            // понятное сообщение, а не упал с ошибкой.
            return ExamResultDetail(
                examId = examId,
                subjectName = "Результат экзамена",
                totalScore = 0,
                maxScore = 0,
                tasks = emptyList()
            )
        }

        val rows = detailsTable.select("tr")

        // Пытаемся найти строку с итоговым баллом (часто первая строка таблицы)
        val totalRow = rows.firstOrNull { it.text().contains("Итог", ignoreCase = true) }
        val totalScoreText = totalRow?.text()
        val totalScore = totalScoreText?.let { Regex("""(\d+)""").find(it)?.groupValues?.get(1)?.toIntOrNull() } ?: 0

        val taskRows = rows.filter { row ->
            row.select("td").size >= 2 && Regex("""^\d+$""").matches(row.select("td").firstOrNull()?.text()?.trim().orEmpty())
        }

        val tasks = taskRows.mapNotNull { row ->
            val cells = row.select("td")
            val taskNumber = cells.getOrNull(0)?.text()?.trim() ?: return@mapNotNull null
            val scoreText = cells.getOrNull(1)?.text()?.trim().orEmpty()
            val (earned, max) = parseScoreFraction(scoreText)

            val criteriaCell = cells.getOrNull(2)?.text()?.trim()
            val criteria = criteriaCell?.let { parseCriteria(it) } ?: emptyList()

            TaskScore(taskNumber = taskNumber, scoreEarned = earned, scoreMax = max, criteria = criteria)
        }

        val maxScore = tasks.sumOf { it.scoreMax }.takeIf { it > 0 } ?: 100

        return ExamResultDetail(
            examId = examId,
            subjectName = "Результат экзамена",
            totalScore = totalScore,
            maxScore = maxScore,
            tasks = tasks
        )
    }

    private fun parseScoreFraction(text: String): Pair<Int, Int> {
        val match = Regex("""(\d+)\s*/\s*(\d+)""").find(text)
        return if (match != null) {
            match.groupValues[1].toInt() to match.groupValues[2].toInt()
        } else {
            0 to 1
        }
    }

    private fun parseCriteria(text: String): List<CriterionScore> {
        return Regex("""(К\d+)\s*[-:]\s*(\d+)""").findAll(text).map { m ->
            CriterionScore(code = m.groupValues[1], scoreEarned = m.groupValues[2].toInt(), scoreMax = 0)
        }.toList()
    }
}
