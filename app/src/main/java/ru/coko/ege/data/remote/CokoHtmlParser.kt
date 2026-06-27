package ru.coko.ege.data.remote

import org.jsoup.nodes.Document
import ru.coko.ege.domain.model.ExamCard
import ru.coko.ege.domain.model.ExamStatus
import ru.coko.ege.domain.model.StudentProfile

/**
 * =====================================================================================
 * Парсер HTML страниц coko.tomsk.ru/exam2026 — основан на реальной верстке сайта.
 * =====================================================================================
 *
 * Структура подтверждена по живому HTML трёх страниц:
 *   default.aspx       — страница логина / лента новостей
 *   personal_data.aspx — ФИО, класс, школа, расписание назначенных экзаменов (с ППЭ)
 *   res_exams.aspx     — таблица результатов с баллами и ссылками "Просмотреть"
 */
object CokoHtmlParser {

    /**
     * После успешного входа сервер рисует блок #ctl00_LoggedUserPanel с фамилией
     * в #ctl00_lbLastName. Если его нет — значит мы по-прежнему на странице логина
     * (неверные данные или сессия не установилась).
     */
    fun isLoginSuccessful(doc: Document): Boolean {
        return doc.selectFirst("#ctl00_LoggedUserPanel") != null &&
            doc.selectFirst("#ctl00_lbLastName") != null
    }

    /**
     * Явного текстового сообщения об ошибке вида "Неверная фамилия или паспорт"
     * на сайте не обнаружено — он просто показывает форму логина снова.
     * Возвращаем понятное сообщение по умолчанию.
     */
    fun extractErrorMessage(doc: Document): String? {
        val visibleValidatorError = doc.select("span[style*=visibility:visible]")
            .firstOrNull { it.text().isNotBlank() }
            ?.attr("title")

        return visibleValidatorError?.takeIf { it.isNotBlank() }
            ?: "Проверьте правильность фамилии, серии и номера паспорта. " +
                "Если данные верны, возможно результаты по вашему экзамену ещё не загружены в систему."
    }

    /**
     * Парсит блок ФИО/класс/школа. Доступен и на странице логина (после входа,
     * в правом верхнем блоке #ctl00_LoggedUserPanel), и подробнее — на personal_data.aspx.
     * Предпочитаем personal_data.aspx, если он передан, иначе берём из шапки.
     */
    fun parseProfile(doc: Document): StudentProfile? {
        val fioFromPersonalData = doc.selectFirst("#ctl00_ContentPlaceHolder1_lbFIO")?.text()?.trim()
        if (fioFromPersonalData != null) {
            val schoolClass = doc.selectFirst("#ctl00_ContentPlaceHolder1_lbClass")?.text()?.trim()
            val school = doc.selectFirst("#ctl00_ContentPlaceHolder1_lbSchool")?.text()?.trim()
            return StudentProfile(fullName = fioFromPersonalData, schoolClass = schoolClass, school = school)
        }

        val lastName = doc.selectFirst("#ctl00_lbLastName")?.text()?.trim() ?: return null
        val nameMidName = doc.selectFirst("#ctl00_lbNameMidName")?.text()?.trim().orEmpty()
        val classText = doc.selectFirst("#ctl00_lbClass")?.text()?.trim()?.removePrefix("Класс:")?.trim()
        val schoolText = doc.selectFirst("#ctl00_lbSchool")?.text()?.trim()

        return StudentProfile(
            fullName = listOf(lastName, nameMidName).filter { it.isNotBlank() }.joinToString(" "),
            schoolClass = classText,
            school = schoolText
        )
    }

    /**
     * Парсит расписание назначенных экзаменов со страницы personal_data.aspx —
     * таблица #ctl00_ContentPlaceHolder1_ChExams с колонками:
     * № п/п | Экзамен (название + дата + время) | ППЭ | Аудитория | Место.
     */
    fun parseScheduledExams(doc: Document): List<ExamCard> {
        val table = doc.selectFirst("#ctl00_ContentPlaceHolder1_ChExams") ?: return emptyList()
        val rows = table.select("tr").drop(1)

        return rows.mapIndexedNotNull { index, row ->
            val cells = row.select("td")
            if (cells.size < 3) return@mapIndexedNotNull null

            val examCell = cells.getOrNull(1)?.text()?.trim() ?: return@mapIndexedNotNull null
            val parts = examCell.split(",").map { it.trim() }
            val subject = parts.getOrNull(0) ?: examCell
            val date = parts.getOrNull(1) ?: ""
            val time = parts.getOrNull(2)

            val location = cells.getOrNull(2)?.text()?.trim()

            ExamCard(
                id = "scheduled_$index",
                subjectName = subjectFullName(subject),
                date = date,
                time = time,
                location = location,
                status = ExamStatus.UPCOMING
            )
        }
    }

    /**
     * Парсит таблицу результатов со страницы res_exams.aspx —
     * #ctl00_ContentPlaceHolder1_ResExams с колонками:
     * Экзамен | Вариант | Балл | КГ | Отметка (результат) | Статус | Апелляция | Подробно.
     *
     * Из колонки "Подробно" достаём имя ссылки __doPostBack(...) — это и есть
     * detailLinkTarget, который нужен CokoWebClient.openResultDetail().
     * Кладём его в ExamCard.id, чтобы ResultDetailViewModel мог использовать
     * его напрямую при открытии "Подробнее".
     */
    fun parseResultExams(doc: Document): List<ExamCard> {
        val table = doc.selectFirst("#ctl00_ContentPlaceHolder1_ResExams") ?: return emptyList()
        val rows = table.select("tr")

        return rows.mapIndexedNotNull { index, row ->
            val cells = row.select("td")
            if (cells.size < 6) return@mapIndexedNotNull null
            if (row.selectFirst("strong") != null) return@mapIndexedNotNull null

            val examText = cells.getOrNull(0)?.text()?.trim() ?: return@mapIndexedNotNull null
            val examParts = examText.split(",").map { it.trim() }
            val subject = examParts.getOrNull(0) ?: examText
            val date = examParts.getOrNull(1) ?: ""

            val scoreText = cells.getOrNull(4)?.text()?.trim()
            val scoreValue = scoreText?.filter { it.isDigit() }?.toIntOrNull()
            val statusText = cells.getOrNull(5)?.text()?.trim().orEmpty()

            val detailLink = row.selectFirst("a[href^=javascript:__doPostBack]")
            val detailLinkTarget = detailLink?.attr("href")?.let { extractPostBackTarget(it) }

            ExamCard(
                id = detailLinkTarget ?: "result_$index",
                subjectName = subjectFullName(subject),
                date = date,
                status = resolveResultStatus(statusText, scoreValue),
                score = scoreValue
            )
        }
    }

    /** Достаёт первый аргумент __doPostBack('ИМЯ_ЦЕЛИ', '...') из строки href. */
    private fun extractPostBackTarget(href: String): String? {
        val match = Regex("""__doPostBack\('([^']+)'""").find(href)
        return match?.groupValues?.getOrNull(1)
    }

    private fun resolveResultStatus(statusText: String, score: Int?): ExamStatus {
        val normalized = statusText.lowercase()
        return when {
            normalized.contains("активный результат") || score != null -> ExamStatus.RESULT_READY
            normalized.contains("аннулир") -> ExamStatus.UNKNOWN
            else -> ExamStatus.UNKNOWN
        }
    }

    /** Расшифровка коротких кодов предметов сайта ЦОКО в читаемые названия. */
    private fun subjectFullName(code: String): String {
        val cleanCode = code.substringBefore("-").trim().uppercase()
        return subjectNames[cleanCode] ?: code
    }

    private val subjectNames = mapOf(
        "РУС" to "Русский язык",
        "МАТП" to "Математика (профильная)",
        "МАТБ" to "Математика (базовая)",
        "МАТ_ГВЭ" to "Математика (ГВЭ)",
        "ФИЗ" to "Физика",
        "ФИЗ_ГВЭ" to "Физика (ГВЭ)",
        "ХИМ" to "Химия",
        "ХИМ_ГВЭ" to "Химия (ГВЭ)",
        "БИО" to "Биология",
        "БИО_ГВЭ" to "Биология (ГВЭ)",
        "ИНФ" to "Информатика",
        "ИНФ_КЕГЭ" to "Информатика (КЕГЭ)",
        "ИНФ_ГВЭ" to "Информатика (ГВЭ)",
        "ИСТ" to "История",
        "ИСТ_ГВЭ" to "История (ГВЭ)",
        "ОБЩ" to "Обществознание",
        "ОБЩ_ГВЭ" to "Обществознание (ГВЭ)",
        "ГЕО" to "География",
        "ГЕО_ГВЭ" to "География (ГВЭ)",
        "ЛИТ" to "Литература",
        "ЛИТ_ГВЭ" to "Литература (ГВЭ)",
        "АНГ" to "Английский язык",
        "НЕМ" to "Немецкий язык",
        "ФРА" to "Французский язык",
        "ИСП" to "Испанский язык",
        "КИТ" to "Китайский язык",
        "АЯУ" to "Английский язык (устная часть)",
        "НЯУ" to "Немецкий язык (устная часть)",
        "ФЯУ" to "Французский язык (устная часть)",
        "ИЯУ" to "Испанский язык (устная часть)",
        "КЯУ" to "Китайский язык (устная часть)",
        "СОЧИН" to "Итоговое сочинение",
        "ИЗЛОЖ" to "Итоговое изложение",
        "РУС_ГВЭ" to "Русский язык (ГВЭ)"
    )
}
