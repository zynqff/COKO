package ru.coko.ege.presentation.navigation

import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val PRIVACY = "privacy"
    const val MAIN = "main"               // host with bottom bar
    const val MAIN_HOME = "main_home"
    const val RESULTS = "results"
    const val RESULT_DETAIL = "result_detail/{examId}"
    const val HELP = "help"
    const val PROFILE = "profile"

    /**
     * examId здесь — это реальное имя ASP.NET-постбэка вида
     * "ctl00$ContentPlaceHolder1$lb_<guid>", поэтому его нужно URL-кодировать
     * перед подстановкой в путь навигации (символ $ безопасен, но лучше
     * не рисковать с будущими форматами id).
     */
    fun resultDetail(examId: String) = "result_detail/${URLEncoder.encode(examId, "UTF-8")}"

    fun decodeExamId(encoded: String): String = URLDecoder.decode(encoded, "UTF-8")
}
