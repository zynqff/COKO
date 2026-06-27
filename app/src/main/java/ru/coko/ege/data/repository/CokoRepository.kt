package ru.coko.ege.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.coko.ege.data.local.SecureCredentialsStore
import ru.coko.ege.data.remote.CokoHtmlParser
import ru.coko.ege.data.remote.CokoWebClient
import ru.coko.ege.data.remote.ServerErrorException
import ru.coko.ege.data.remote.SiteStructureChangedException
import ru.coko.ege.domain.model.CokoResult
import ru.coko.ege.domain.model.ErrorType
import ru.coko.ege.domain.model.ExamCard
import ru.coko.ege.domain.model.StudentProfile
import ru.coko.ege.domain.model.UserCredentials
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Единая точка входа для UI/ViewModel.
 *
 * После успешного входа на default.aspx (см. CokoWebClient.login) приложение
 * дополнительно GET'ит personal_data.aspx (расписание + ФИО/класс/школа)
 * и res_exams.aspx (результаты с баллами) — на сайте ЦОКО это две отдельные
 * страницы личного кабинета, а не одна.
 */
@Singleton
class CokoRepository @Inject constructor(
    private val webClient: CokoWebClient,
    private val credentialsStore: SecureCredentialsStore
) {

    data class DashboardData(
        val profile: StudentProfile?,
        val exams: List<ExamCard> // объединённые: расписание (без баллов) + результаты (с баллами)
    )

    suspend fun login(credentials: UserCredentials): CokoResult<DashboardData> =
        withContext(Dispatchers.IO) {
            try {
                val loginResultDoc = webClient.login(
                    lastName = credentials.lastName,
                    series = credentials.passportSeries,
                    number = credentials.passportNumber
                )

                if (!CokoHtmlParser.isLoginSuccessful(loginResultDoc)) {
                    val errorText = CokoHtmlParser.extractErrorMessage(loginResultDoc)
                        ?: "Проверьте правильность фамилии, серии и номера паспорта"
                    return@withContext CokoResult.Error(ErrorType.INVALID_CREDENTIALS, errorText)
                }

                credentialsStore.saveCredentials(credentials)

                val dashboard = loadDashboard()
                CokoResult.Success(dashboard)
            } catch (e: SiteStructureChangedException) {
                CokoResult.Error(
                    ErrorType.SITE_STRUCTURE_CHANGED,
                    "Сайт ЦОКО изменил структуру страницы. Сообщите разработчику: ${e.message}"
                )
            } catch (e: ServerErrorException) {
                CokoResult.Error(ErrorType.SERVER_ERROR, e.message ?: "Сервер ЦОКО недоступен")
            } catch (e: IOException) {
                CokoResult.Error(ErrorType.NO_INTERNET, "Нет подключения к интернету")
            } catch (e: Exception) {
                CokoResult.Error(ErrorType.UNKNOWN, e.message ?: "Неизвестная ошибка")
            }
        }

    /**
     * После успешного входа сессия уже установлена (cookie сохранены в CokoWebClient),
     * поэтому просто GET'им personal_data.aspx и res_exams.aspx и объединяем данные.
     */
    private fun loadDashboard(): DashboardData {
        val personalDataDoc = webClient.fetchAuthenticatedPage(CokoWebClient.PERSONAL_DATA_URL)
        val resultsDoc = webClient.fetchAuthenticatedPage(CokoWebClient.RESULTS_URL)

        val profile = CokoHtmlParser.parseProfile(personalDataDoc)
        val scheduledExams = CokoHtmlParser.parseScheduledExams(personalDataDoc)
        val resultExams = CokoHtmlParser.parseResultExams(resultsDoc)

        // Объединяем: для предметов, по которым уже есть результат, показываем
        // карточку с баллом; остальные — как "скоро" из расписания.
        val resultSubjects = resultExams.map { it.subjectName to it.date }.toSet()
        val onlyScheduled = scheduledExams.filterNot { (it.subjectName to it.date) in resultSubjects }

        return DashboardData(profile = profile, exams = resultExams + onlyScheduled)
    }

    /**
     * "Тихий" автологин по сохранённым данным — вызывается на сплеш-экране
     * и при ручном обновлении (свайп вниз на главном экране). У ASP.NET-сессии
     * нет долгоживущего токена, поэтому проще и надёжнее заново пройти полный
     * цикл входа по тем же сохранённым данным, чем пытаться продлевать cookie.
     */
    suspend fun refreshDashboard(): CokoResult<DashboardData> {
        val saved = credentialsStore.getCredentials()
            ?: return CokoResult.Error(ErrorType.INVALID_CREDENTIALS, "Нет сохранённых данных для входа")
        return login(saved)
    }

    suspend fun hasStoredCredentials(): Boolean = credentialsStore.hasStoredCredentials()

    suspend fun logout() = credentialsStore.clearCredentials()
}
