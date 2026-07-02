package ru.coko.ege.data.remote

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import ru.coko.ege.domain.model.AnswerSheet
import ru.coko.ege.domain.model.ExamCard
import ru.coko.ege.domain.model.ExamResultDetail
import ru.coko.ege.domain.model.ExamStatus
import ru.coko.ege.domain.model.ExamType
import ru.coko.ege.domain.model.StudentProfile
import ru.coko.ege.domain.model.TaskAnswer

object CokoHtmlParser {

    fun isLoginSuccessful(doc: Document): Boolean {
        return doc.selectFirst("#ctl00_LoggedUserPanel") != null &&
            doc.selectFirst("#ctl00_lbLastName") != null
    }

    fun extractErrorMessage(doc: Document): String? {
        val visibleValidatorError = doc.select("span[style*=visibility:visible]")
            .firstOrNull { it.text().isNotBlank() }
            ?.attr("title")

        return visibleValidatorError?.takeIf { it.isNotBlank() }
            ?: "Проверьте правильность фамилии, серии и номера паспорта. " +
                "Если данные верны, возможно результаты по вашему экзамену ещё не загружены в систему."
    }

    fun parseProfile(doc: Document): StudentProfile? {
        val fioFromPersonalData = doc.selectFirst("#ctl00_ContentPlaceHolder1_lbFIO")?.text()?.trim()
        if (fioFromPersonalData != null) {
            val schoolClass = doc.selectFirst("#ctl00_ContentPlaceHolder1_lbClass")?.text()?.trim()
            val schoolRaw = doc.selectFirst("#ctl00_ContentPlaceHolder1_lbSchool")?.text()?.trim()
            val area = doc.selectFirst("#ctl00_ContentPlaceHolder1_lbArea")?.text()?.trim()

            val schoolCodeMatch = schoolRaw?.let { Regex("""^\((\d+)\)\s*(.+)$""").find(it) }
            val schoolCode = schoolCodeMatch?.groupValues?.getOrNull(1)
            val schoolName = schoolCodeMatch?.groupValues?.getOrNull(2) ?: schoolRaw

            return StudentProfile(
                fullName = fioFromPersonalData,
                schoolClass = schoolClass,
                school = schoolName,
                schoolCode = schoolCode,
                area = area
            )
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

    fun parseScheduledExams(doc: Document): List<ExamCard> {
        val table = doc.selectFirst("#ctl00_ContentPlaceHolder1_ChExams") ?: return emptyList()
        val rows = table.select("tr").drop(1)

        return rows.mapIndexedNotNull { index, row ->
            val cells = row.select("td")
            if (cells.size < 3) return@mapIndexedNotNull null

            val examCell = cells.getOrNull(1)?.text()?.trim() ?: return@mapIndexedNotNull null
            val parts = examCell.split(",").map { it.trim() }
            val code = parts.getOrNull(0) ?: examCell
            val date = parts.getOrNull(1) ?: ""
            val time = parts.getOrNull(2)

            val location = cells.getOrNull(2)?.text()?.trim()

            ExamCard(
                id = "scheduled_$index",
                subjectName = subjectFullName(code),
                subjectCode = code,
                examType = resolveExamType(code),
                date = date,
                time = time,
                location = location,
                status = ExamStatus.UPCOMING
            )
        }
    }

    fun parseResultExams(doc: Document): List<ExamCard> {
        val table = doc.selectFirst("#ctl00_ContentPlaceHolder1_ResExams") ?: return emptyList()
        val rows = table.select("tr")

        return rows.mapIndexedNotNull { index, row ->
            val cells = row.select("td")
            if (cells.size < 6) return@mapIndexedNotNull null
            if (row.selectFirst("strong") != null) return@mapIndexedNotNull null

            val examText = cells.getOrNull(0)?.text()?.trim() ?: return@mapIndexedNotNull null
            val examParts = examText.split(",").map { it.trim() }
            val code = examParts.getOrNull(0) ?: examText
            val date = examParts.getOrNull(1) ?: ""

            val variant = cells.getOrNull(1)?.text()?.trim()?.takeIf { it.isNotEmpty() && it != "---" }
            val primaryScoreText = cells.getOrNull(2)?.text()?.trim()
            val primaryScore = primaryScoreText?.filter { it.isDigit() }?.toIntOrNull()

            val scoreDisplay = cells.getOrNull(4)?.text()?.trim()?.takeIf { it.isNotEmpty() && it != "---" }
            val statusText = cells.getOrNull(5)?.text()?.trim().orEmpty()

            val detailLink = row.selectFirst("a[href^=javascript:__doPostBack]")
            val detailLinkTarget = detailLink?.attr("href")?.let { extractPostBackTarget(it) }

            val examType = resolveExamType(code)
            val maxScore = resolveMaxScoreForSubject(code, examType)

            ExamCard(
                id = detailLinkTarget ?: "result_$index",
                subjectName = subjectFullName(code),
                subjectCode = code,
                examType = examType,
                date = date,
                status = resolveResultStatus(statusText, scoreDisplay),
                scoreDisplay = scoreDisplay,
                primaryScore = primaryScore,
                maxScoreForSubject = maxScore,
                variant = variant
            )
        }
    }

    fun parseResultDetail(doc: Document, examId: String): ExamResultDetail {
        val table = doc.selectFirst("#ctl00_ContentPlaceHolder1_DetailsTable")
            ?: return ExamResultDetail(
                examId = examId,
                subjectName = "Результат экзамена",
                subjectCode = "",
                examType = ExamType.UNKNOWN
            )

        var examFullText = ""
        var ppe: String? = null
        var auditorium: String? = null
        var variant: String? = null
        var status: String? = null
        var shortAnswerTasks: List<TaskAnswer> = emptyList()
        var typeBTasks: String? = null
        var typeCTasks: String? = null
        var literacyCriterion: String? = null
        var primaryScore: Int? = null
        var finalScoreDisplay: String? = null
        var appeal: String? = null
        var additionalInfo: String? = null
        val answerSheets = mutableListOf<AnswerSheet>()

        val topLevelRows = table.children().filter { it.tagName() == "tr" }

        for (row in topLevelRows) {
            val cells = row.children().filter { it.tagName() == "td" }
            if (cells.size < 2) continue

            val label = cells[0].text().trim()
            val valueCell = cells[1]
            val valueText = valueCell.text().trim()

            when {
                label.startsWith("Экзамен") -> examFullText = valueText
                label.startsWith("ППЭ") -> ppe = valueText
                label.startsWith("Аудитория") -> auditorium = valueText
                label.startsWith("Вариант") -> variant = valueText
                label.startsWith("Статус") -> status = valueText
                label.startsWith("Ответы на задания") -> shortAnswerTasks = parseShortAnswerTasks(valueCell)
                label.startsWith("Задания типа B") -> typeBTasks = valueText
                label.startsWith("Задания типа C") -> typeCTasks = valueText
                label.startsWith("Критерий грамотности") -> literacyCriterion = valueText
                label.startsWith("Первичный балл") -> primaryScore = valueText.filter { it.isDigit() }.toIntOrNull()
                label.startsWith("Отметка") -> finalScoreDisplay = valueText.takeIf { it.isNotEmpty() }
                label.startsWith("Апелляция") -> appeal = valueText
                label.startsWith("Доп.информация") || label.startsWith("Дополнительная информация") -> {
                    additionalInfo = valueCell.html()
                        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
                        .let { org.jsoup.Jsoup.parse(it).text() }
                        .takeIf { it.isNotBlank() }
                }
                label.startsWith("Бланки") -> {
                    valueCell.select("a[href^=javascript:__doPostBack]").forEach { link ->
                        val target = extractPostBackTarget(link.attr("href"))
                        answerSheets += AnswerSheet(title = link.text().trim(), postBackTarget = target)
                    }
                }
            }
        }

        val code = examFullText.substringBefore(",").trim()
        val datePart = examFullText.substringAfter(",", "").trim()
        val examType = resolveExamType(code)
        val maxScore = resolveMaxScoreForSubject(code, examType)

        return ExamResultDetail(
            examId = examId,
            subjectName = subjectFullName(code),
            subjectCode = code,
            examType = examType,
            dateText = datePart.takeIf { it.isNotBlank() },
            ppe = ppe,
            auditorium = auditorium,
            variant = variant,
            status = status,
            shortAnswerTasks = shortAnswerTasks,
            typeBTasks = typeBTasks,
            typeCTasks = typeCTasks,
            literacyCriterion = literacyCriterion,
            primaryScore = primaryScore,
            finalScoreDisplay = finalScoreDisplay,
            maxScoreForSubject = maxScore,
            appeal = appeal,
            additionalInfo = additionalInfo,
            answerSheets = answerSheets
        )
    }

    private fun parseShortAnswerTasks(container: Element): List<TaskAnswer> {
        val innerTable = container.selectFirst("table") ?: return emptyList()
        val rows = innerTable.select("tr").drop(1)

        return rows.mapNotNull { row ->
            val cells = row.select("td")
            if (cells.size < 3) return@mapNotNull null

            val taskCellText = cells[0].text().trim()
            val taskNumberMatch = Regex("""^(\d+)""").find(taskCellText)
            val taskNumber = taskNumberMatch?.groupValues?.get(1) ?: taskCellText
            val hintMatch = Regex("""\(([^)]+)\)""").find(taskCellText)
            val hint = hintMatch?.groupValues?.get(1)

            val answer = cells[1].text().trim()

            val scoreText = cells[2].text().trim()
            val scoreMatch = Regex("""(\d+)\s*из\s*(\d+)""").find(scoreText)
            val earned = scoreMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
            val max = scoreMatch?.groupValues?.getOrNull(2)?.toIntOrNull() ?: 1

            TaskAnswer(taskNumber = taskNumber, taskHint = hint, answer = answer, scoreEarned = earned, scoreMax = max)
        }
    }

    private fun extractPostBackTarget(href: String): String? {
        val match = Regex("""__doPostBack\('([^']+)'""").find(href)
        return match?.groupValues?.getOrNull(1)
    }

    private fun resolveResultStatus(statusText: String, scoreDisplay: String?): ExamStatus {
        val normalized = statusText.lowercase()
        return when {
            normalized.contains("активный результат") || scoreDisplay != null -> ExamStatus.RESULT_READY
            normalized.contains("аннулир") -> ExamStatus.UNKNOWN
            else -> ExamStatus.UNKNOWN
        }
    }

    /**
     * Определяет тип аттестации по коду предмета сайта ЦОКО (а не по баллам —
     * шкалы у разных типов пересекаются и сами по себе не надёжны).
     *
     * Примеры реальных кодов с сайта: "МАТП-9", "РУС-11", "ИНФ_КЕГЭ-11",
     * "МАТ_ГВЭ-9", "СОЧИН-11", "ИЗЛОЖ-11" (см. ленту новостей default.aspx).
     * Итоговое собеседование на сайте обычно имеет код с "СОБ" (по аналогии
     * с "СОЧИН"/"ИЗЛОЖ" для 11 класса) — если реальный код будет другим,
     * это единственное место, которое нужно поправить.
     */
    fun resolveExamType(code: String): ExamType {
        val upper = code.uppercase()
        return when {
            upper.contains("СОБ") -> ExamType.FINAL_INTERVIEW
            upper.startsWith("СОЧИН") || upper.startsWith("ИЗЛОЖ") -> ExamType.FINAL_ESSAY
            upper.contains("КЕГЭ") -> ExamType.KEGE
            upper.contains("ГВЭ") -> ExamType.GVE
            upper.endsWith("-9") -> ExamType.OGE
            upper.endsWith("-11") -> ExamType.EGE
            else -> ExamType.UNKNOWN
        }
    }

    /**
     * Максимальный первичный балл по предметам ОГЭ/ЕГЭ — нужен только для
     * отображения "X из Y" рядом с баллом, НЕ для определения типа экзамена.
     */
    private fun resolveMaxScoreForSubject(code: String, examType: ExamType): Int? {
        val shortCode = code.substringBefore("-").substringBefore("_").trim().uppercase()
        return when (examType) {
            ExamType.OGE -> ogeMaxPrimaryScores[shortCode]
            ExamType.EGE, ExamType.KEGE -> egeMaxPrimaryScores[shortCode]
            ExamType.GVE -> gveMaxPrimaryScores[shortCode]
            ExamType.FINAL_INTERVIEW -> 20
            else -> null
        }
    }

    private val ogeMaxPrimaryScores = mapOf(
        "МАТП" to 31, "РУС" to 33, "ФИЗ" to 40, "ХИМ" to 34, "БИО" to 44,
        "ИНФ" to 19, "ИСТ" to 37, "ОБЩ" to 37, "ГЕО" to 32, "АНГ" to 70,
        "НЕМ" to 70, "ФРА" to 70, "ИСП" to 70, "ЛИТ" to 33
    )

    private val egeMaxPrimaryScores = mapOf(
        "МАТП" to 21, "МАТБ" to 21, "РУС" to 59, "ФИЗ" to 53, "ХИМ" to 56,
        "БИО" to 58, "ИНФ" to 27, "ИСТ" to 55, "ОБЩ" to 57, "ГЕО" to 47,
        "АНГ" to 100, "НЕМ" to 100, "ФРА" to 100, "ИСП" to 100, "КИТ" to 100,
        "ЛИТ" to 55
    )

    private val gveMaxPrimaryScores = mapOf(
        "МАТ" to 12, "РУС" to 20
    )

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
        "РУС_ГВЭ" to "Русский язык (ГВЭ)",
        "СОБ" to "Устное собеседование"
    )
}
