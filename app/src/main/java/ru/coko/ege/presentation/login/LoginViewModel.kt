package ru.coko.ege.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.coko.ege.data.repository.CokoRepository
import ru.coko.ege.domain.model.CokoResult
import ru.coko.ege.domain.model.UserCredentials
import javax.inject.Inject

data class LoginUiState(
    val lastName: String = "",
    val series: String = "",
    val number: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val isFormValid: Boolean
        get() = lastName.trim().length >= 2 &&
            series.trim().length in 2..4 &&
            number.trim().length in 4..7
}

sealed class LoginEvent {
    data class LoginSuccess(val dashboard: CokoRepository.DashboardData) : LoginEvent()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: CokoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    private val _events = MutableStateFlow<LoginEvent?>(null)
    val events: StateFlow<LoginEvent?> = _events

    fun onLastNameChange(value: String) = _uiState.update { it.copy(lastName = value, errorMessage = null) }
    fun onSeriesChange(value: String) = _uiState.update { it.copy(series = value.filter { c -> c.isDigit() }, errorMessage = null) }
    fun onNumberChange(value: String) = _uiState.update { it.copy(number = value.filter { c -> c.isDigit() }, errorMessage = null) }

    fun onLoginClick() {
        val state = _uiState.value
        if (!state.isFormValid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = repository.login(
                UserCredentials(
                    lastName = state.lastName,
                    passportSeries = state.series,
                    passportNumber = state.number
                )
            )

            when (result) {
                is CokoResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.value = LoginEvent.LoginSuccess(result.data)
                }
                is CokoResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }

    fun consumeEvent() {
        _events.value = null
    }
}
