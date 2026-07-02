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
import ru.coko.ege.data.remote.CokoHtmlParser
import ru.coko.ege.data.remote.CokoWebClient
import ru.coko.ege.data.remote.ServerErrorException
import ru.coko.ege.domain.model.ExamResultDetail
import ru.coko.ege.presentation.navigation.Routes
import java.io.IOException
import javax.inject.Inject

data class ResultDetailUiState(
    val isLoading: Boolean = true,
    val detail: ExamResultDetail? = null,
    val errorMessage: String? = null
)

/**
 * Загружает детализацию по конкретному результату экзамена через AJAX-постбэк
 * "Просмотреть" на res_exams.aspx. Структура подтверждена реальным HTML
 * (пример: ОГЭ по математике с полной разбивкой по заданиям).
 */
@HiltViewModel
class ResultDetailViewModel @Inject constructor(
    private val webClient: CokoWebClient,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val examId: String = savedStateHandle.get<String>("examId")
        ?.let { Routes.decodeExamId(it) }
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
                    CokoHtmlParser.parseResultDetail(doc, examId)
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
                        errorMessage = "Не удалось загрузить детализацию: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * URL страницы результатов на сайте — бланки ответов (сканы с персональными
     * данными) рендерятся через отдельный AJAX-постбэк с бинарным изображением,
     * прокладывать его через приложение нет смысла, поэтому "Бланк ответов №N"
     * в UI помечен звёздочкой и ведёт пользователя сюда, на сайт.
     */
    fun resultsPageUrl(): String = CokoWebClient.RESULTS_URL
}
