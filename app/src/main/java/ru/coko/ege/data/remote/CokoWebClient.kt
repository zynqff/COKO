package ru.coko.ege.data.remote

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * =====================================================================================
 * Реальная структура сайта coko.tomsk.ru/exam2026 (проверено по живому HTML).
 * =====================================================================================
 *
 * Сайт — классический ASP.NET WebForms с AJAX UpdatePanel (Microsoft Ajax,
 * Sys.WebForms.PageRequestManager). Это значит, что часть "переходов" внутри
 * личного кабинета (например, кнопка "Просмотреть" у результата экзамена)
 * НЕ открывает новую страницу, а делает partial postback на ту же самую
 * res_exams.aspx через __doPostBack('ctl00$ContentPlaceHolder1$lb_<guid>', '').
 *
 * Алгоритм входа:
 *   1. GET default.aspx → достаём __VIEWSTATE/__EVENTVALIDATION + cookie сессии
 *      (ASP.NET сессия живёт на cookie, поэтому OkHttp-клиент с общим CookieJar
 *      обязателен — все дальнейшие запросы идут в той же сессии).
 *   2. POST на default.aspx с полями ctl00$LastName / ctl00$Seria / ctl00$Number
 *      + токенами формы + ctl00$LoginButton=Выполнить вход.
 *   3. Если вход успешен, сервер в ответе уже отрисовывает блок
 *      <div id="ctl00_LoggedUserPanel"> с ФИО — дальше просто GET'им
 *      personal_data.aspx и res_exams.aspx в этой же сессии.
 *   4. Для постбэка "Просмотреть" — берём __VIEWSTATE/__EVENTVALIDATION
 *      СО СТРАНИЦЫ res_exams.aspx (не со страницы логина!) и POST'им туда же
 *      с __EVENTTARGET = имя конкретной ссылки (например
 *      "ctl00$ContentPlaceHolder1$lb_3572c50f_d668_4161_a1a6_f3008ef5acee").
 */
@Singleton
class CokoWebClient @Inject constructor() {

    companion object {
        const val BASE_URL = "https://coko.tomsk.ru/exam2026"
        const val LOGIN_PAGE_URL = "$BASE_URL/default.aspx"
        const val PERSONAL_DATA_URL = "$BASE_URL/personal_data.aspx"
        const val RESULTS_URL = "$BASE_URL/res_exams.aspx"

        // --- Реальные имена полей формы входа (взято из живого HTML сайта) ---
        const val FIELD_LASTNAME = "ctl00\$LastName"
        const val FIELD_SERIES = "ctl00\$Seria"
        const val FIELD_NUMBER = "ctl00\$Number"
        const val FIELD_LOGIN_BUTTON = "ctl00\$LoginButton"
        const val FIELD_LOGIN_BUTTON_VALUE = "Выполнить вход"
        const val FIELD_LOGOFF_TARGET = "ctl00\$LogOff"

        const val HIDDEN_VIEWSTATE = "__VIEWSTATE"
        const val HIDDEN_VIEWSTATEGENERATOR = "__VIEWSTATEGENERATOR"
        const val HIDDEN_EVENTVALIDATION = "__EVENTVALIDATION"
        const val HIDDEN_EVENTTARGET = "__EVENTTARGET"
        const val HIDDEN_EVENTARGUMENT = "__EVENTARGUMENT"
        const val HIDDEN_LASTFOCUS = "__LASTFOCUS"

        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 12; Mobile) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }

    /** Снимок скрытых ASP.NET-полей, снятый с конкретной страницы. */
    data class AspNetFormState(
        val viewState: String,
        val viewStateGenerator: String,
        val eventValidation: String
    )

    // Cookie-хранилище на время жизни процесса — здесь живёт ASP.NET_SessionId.
    // Без него после первого же запроса сервер "забывает", что мы залогинены.
    private val cookieStore = mutableMapOf<String, List<Cookie>>()

    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            if (cookies.isNotEmpty()) cookieStore[url.host] = cookies
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host] ?: emptyList()
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /** Достаёт __VIEWSTATE/__VIEWSTATEGENERATOR/__EVENTVALIDATION с произвольной страницы. */
    fun extractFormState(doc: Document): AspNetFormState {
        val viewState = doc.selectFirst("input#$HIDDEN_VIEWSTATE, input[name=$HIDDEN_VIEWSTATE]")
            ?.attr("value")
            ?: throw SiteStructureChangedException("Не найдено поле __VIEWSTATE на странице")

        val viewStateGenerator = doc.selectFirst(
            "input#$HIDDEN_VIEWSTATEGENERATOR, input[name=$HIDDEN_VIEWSTATEGENERATOR]"
        )?.attr("value") ?: ""

        val eventValidation = doc.selectFirst(
            "input#$HIDDEN_EVENTVALIDATION, input[name=$HIDDEN_EVENTVALIDATION]"
        )?.attr("value") ?: throw SiteStructureChangedException("Не найдено поле __EVENTVALIDATION на странице")

        return AspNetFormState(viewState, viewStateGenerator, eventValidation)
    }

    private fun baseRequest(url: String): Request.Builder =
        Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept-Language", "ru-RU,ru;q=0.9")

    private fun executeGet(url: String): Document {
        val request = baseRequest(url).get().build()
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            response.close()
            throw ServerErrorException("Сервер ЦОКО вернул код ${response.code} для $url")
        }
        val html = response.body?.string() ?: throw ServerErrorException("Пустой ответ сервера ($url)")
        response.close()
        return Jsoup.parse(html, url)
    }

    private fun executePost(url: String, formBuilder: FormBody.Builder): Document {
        val request = baseRequest(url)
            .post(formBuilder.build())
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Referer", url)
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            response.close()
            throw ServerErrorException("Сервер ЦОКО вернул код ${response.code} при POST на $url")
        }
        val html = response.body?.string() ?: throw ServerErrorException("Пустой ответ сервера при POST на $url")
        response.close()
        return Jsoup.parse(html, url)
    }

    /**
     * Полный цикл входа: GET default.aspx → достаём токены → POST с фамилией/серией/номером.
     * Возвращает HTML-ответ — личный кабинет (успех) либо снова форму логина (ошибка).
     */
    fun login(lastName: String, series: String, number: String): Document {
        val loginPage = executeGet(LOGIN_PAGE_URL)
        val formState = extractFormState(loginPage)

        val form = FormBody.Builder()
            .add(HIDDEN_EVENTTARGET, "")
            .add(HIDDEN_EVENTARGUMENT, "")
            .add(HIDDEN_LASTFOCUS, "")
            .add(HIDDEN_VIEWSTATE, formState.viewState)
            .apply { if (formState.viewStateGenerator.isNotEmpty()) add(HIDDEN_VIEWSTATEGENERATOR, formState.viewStateGenerator) }
            .add(HIDDEN_EVENTVALIDATION, formState.eventValidation)
            .add(FIELD_LASTNAME, lastName.trim())
            .add(FIELD_SERIES, series.trim())
            .add(FIELD_NUMBER, number.trim())
            .add(FIELD_LOGIN_BUTTON, FIELD_LOGIN_BUTTON_VALUE)

        return executePost(LOGIN_PAGE_URL, form)
    }

    /**
     * "Тихий" повторный вход по сохранённым данным — используется на сплеш-экране
     * и при свайп-обновлении, так как у ASP.NET-сессии нет долгоживущего токена:
     * единственный надёжный способ "не спрашивать данные каждый раз" — это хранить
     * их зашифрованно и логиниться автоматически при каждом запуске приложения.
     */
    fun refreshSession(lastName: String, series: String, number: String): Document =
        login(lastName, series, number)

    /** GET произвольной страницы личного кабинета в текущей сессии (после login()). */
    fun fetchAuthenticatedPage(url: String): Document = executeGet(url)

    /**
     * Открывает детализацию конкретного результата экзамена.
     *
     * ВАЖНО: на res_exams.aspx кнопка "Просмотреть" — это не ссылка на отдельную
     * страницу, а AJAX-постбэк внутри той же страницы:
     *   javascript:__doPostBack('ctl00$ContentPlaceHolder1$lb_<guid>','')
     *
     * Поэтому detailLinkTarget — это имя конкретной ссылки (значение __EVENTTARGET),
     * которое нужно сначала вытащить из href кнопки "Просмотреть" на странице
     * результатов (см. CokoHtmlParser.parseExamCards — туда кладём это имя как id).
     * Токены __VIEWSTATE/__EVENTVALIDATION берутся СО СТРАНИЦЫ РЕЗУЛЬТАТОВ —
     * это критично, токены страницы логина здесь не подойдут.
     */
    fun openResultDetail(detailLinkTarget: String): Document {
        val resultsPage = executeGet(RESULTS_URL)
        val formState = extractFormState(resultsPage)

        val form = FormBody.Builder()
            .add(HIDDEN_EVENTTARGET, detailLinkTarget)
            .add(HIDDEN_EVENTARGUMENT, "")
            .add(HIDDEN_VIEWSTATE, formState.viewState)
            .apply { if (formState.viewStateGenerator.isNotEmpty()) add(HIDDEN_VIEWSTATEGENERATOR, formState.viewStateGenerator) }
            .add(HIDDEN_EVENTVALIDATION, formState.eventValidation)

        return executePost(RESULTS_URL, form)
    }

    /**
     * Выход из аккаунта на самом сайте (необязательно вызывать — мы просто
     * стираем локальные данные, — но корректно завершает серверную сессию,
     * если потребуется).
     */
    fun logOff(): Document {
        val page = executeGet(LOGIN_PAGE_URL)
        val formState = extractFormState(page)

        val form = FormBody.Builder()
            .add(HIDDEN_EVENTTARGET, FIELD_LOGOFF_TARGET)
            .add(HIDDEN_EVENTARGUMENT, "")
            .add(HIDDEN_VIEWSTATE, formState.viewState)
            .apply { if (formState.viewStateGenerator.isNotEmpty()) add(HIDDEN_VIEWSTATEGENERATOR, formState.viewStateGenerator) }
            .add(HIDDEN_EVENTVALIDATION, formState.eventValidation)

        return executePost(LOGIN_PAGE_URL, form)
    }
}

class SiteStructureChangedException(message: String) : Exception(message)
class ServerErrorException(message: String) : Exception(message)
class InvalidCredentialsException(message: String) : Exception(message)
