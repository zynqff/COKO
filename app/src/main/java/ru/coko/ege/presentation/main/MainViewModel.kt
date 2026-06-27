package ru.coko.ege.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.coko.ege.data.repository.CokoRepository
import ru.coko.ege.domain.model.CokoResult
import ru.coko.ege.domain.model.ExamCard
import ru.coko.ege.domain.model.StudentProfile
import javax.inject.Inject

data class MainUiState(
    val profile: StudentProfile? = null,
    val exams: List<ExamCard> = emptyList(),
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Общий ViewModel для всех вкладок нижней навигации (Главная/Результаты/Профиль).
 * Так данные о профиле и списке экзаменов не нужно загружать заново
 * при переключении вкладок.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: CokoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

    fun setInitialData(dashboard: CokoRepository.DashboardData) {
        _uiState.update {
            it.copy(profile = dashboard.profile, exams = dashboard.exams)
        }
    }

    /** Свайп-обновление: повторно логинимся по сохранённым данным и тащим свежие данные. */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }

            when (val result = repository.refreshDashboard()) {
                is CokoResult.Success -> _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        profile = result.data.profile,
                        exams = result.data.exams
                    )
                }
                is CokoResult.Error -> _uiState.update {
                    it.copy(isRefreshing = false, errorMessage = result.message)
                }
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.logout()
            onComplete()
        }
    }
}
