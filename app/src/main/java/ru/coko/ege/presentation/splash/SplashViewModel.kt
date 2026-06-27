package ru.coko.ege.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.coko.ege.data.repository.CokoRepository
import ru.coko.ege.domain.model.CokoResult
import javax.inject.Inject

sealed class SplashStatus {
    data object CheckingStorage : SplashStatus()
    data object NoCredentialsFound : SplashStatus()      // -> идём на экран логина
    data object CredentialsFound : SplashStatus()
    data object ConnectingToServer : SplashStatus()
    data class Success(val dashboard: CokoRepository.DashboardData) : SplashStatus()
    data class Failed(val message: String) : SplashStatus() // сессия истекла/сайт недоступен -> логин
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val repository: CokoRepository
) : ViewModel() {

    private val _status = MutableStateFlow<SplashStatus>(SplashStatus.CheckingStorage)
    val status: StateFlow<SplashStatus> = _status

    init {
        checkSavedSession()
    }

    private fun checkSavedSession() {
        viewModelScope.launch {
            _status.value = SplashStatus.CheckingStorage

            val hasCredentials = repository.hasStoredCredentials()
            if (!hasCredentials) {
                _status.value = SplashStatus.NoCredentialsFound
                return@launch
            }

            _status.value = SplashStatus.CredentialsFound
            _status.value = SplashStatus.ConnectingToServer

            when (val result = repository.refreshDashboard()) {
                is CokoResult.Success -> _status.value = SplashStatus.Success(result.data)
                is CokoResult.Error -> _status.value = SplashStatus.Failed(result.message)
            }
        }
    }
}
