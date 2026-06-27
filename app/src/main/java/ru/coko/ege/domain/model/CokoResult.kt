package ru.coko.ege.domain.model

/**
 * Обёртка результата сетевого запроса/парсинга.
 * Нужна, чтобы UI могло явно показать ошибку входа, отсутствие сети
 * или то, что структура сайта изменилась и парсер "сломался".
 */
sealed class CokoResult<out T> {
    data class Success<T>(val data: T) : CokoResult<T>()
    data class Error(val type: ErrorType, val message: String) : CokoResult<Nothing>()
}

enum class ErrorType {
    NO_INTERNET,        // нет подключения к сети
    INVALID_CREDENTIALS,// сайт явно сказал "неверные данные"
    SITE_STRUCTURE_CHANGED, // не нашли ожидаемые поля/токены в HTML — сайт обновился
    SERVER_ERROR,       // 5xx / таймаут / сайт лежит
    UNKNOWN
}
