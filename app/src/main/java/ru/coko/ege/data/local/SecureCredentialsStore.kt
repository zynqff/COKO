package ru.coko.ege.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.coko.ege.domain.model.UserCredentials
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Хранит фамилию + серию/номер паспорта в зашифрованном виде на устройстве.
 *
 * Используется androidx.security.crypto.EncryptedSharedPreferences:
 * мастер-ключ генерируется и хранится в Android Keystore (аппаратный
 * защищённый модуль на большинстве устройств), а сами значения шифруются
 * AES256-GCM перед записью на диск. Это и есть тот самый "кейстор",
 * про который шла речь — Keystore хранит сам ключ шифрования, а не
 * напрямую пароль, что и обеспечивает безопасность даже при доступе
 * к файлам приложения через root/бэкап.
 *
 * Благодаря этому при следующем запуске приложение само логинится
 * на сайт ЦОКО без повторного ввода данных пользователем.
 */
@Singleton
class SecureCredentialsStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private companion object {
        const val PREFS_FILE_NAME = "coko_secure_prefs"
        const val KEY_LAST_NAME = "last_name"
        const val KEY_PASSPORT_SERIES = "passport_series"
        const val KEY_PASSPORT_NUMBER = "passport_number"
        const val KEY_HAS_CREDENTIALS = "has_credentials"
    }

    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    suspend fun saveCredentials(credentials: UserCredentials) = withContext(Dispatchers.IO) {
        encryptedPrefs.edit()
            .putString(KEY_LAST_NAME, credentials.lastName)
            .putString(KEY_PASSPORT_SERIES, credentials.passportSeries)
            .putString(KEY_PASSPORT_NUMBER, credentials.passportNumber)
            .putBoolean(KEY_HAS_CREDENTIALS, true)
            .apply()
    }

    suspend fun getCredentials(): UserCredentials? = withContext(Dispatchers.IO) {
        if (!encryptedPrefs.getBoolean(KEY_HAS_CREDENTIALS, false)) return@withContext null

        val lastName = encryptedPrefs.getString(KEY_LAST_NAME, null) ?: return@withContext null
        val series = encryptedPrefs.getString(KEY_PASSPORT_SERIES, null) ?: return@withContext null
        val number = encryptedPrefs.getString(KEY_PASSPORT_NUMBER, null) ?: return@withContext null

        UserCredentials(lastName = lastName, passportSeries = series, passportNumber = number)
    }

    suspend fun hasStoredCredentials(): Boolean = withContext(Dispatchers.IO) {
        encryptedPrefs.getBoolean(KEY_HAS_CREDENTIALS, false)
    }

    /** Вызывается при нажатии "Выйти из аккаунта" в профиле. */
    suspend fun clearCredentials() = withContext(Dispatchers.IO) {
        encryptedPrefs.edit().clear().apply()
    }
}
