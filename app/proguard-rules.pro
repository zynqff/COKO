# Jsoup
-keep class org.jsoup.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep data/domain models intact (parsed via reflection-free Jsoup but kept for safety)
-keep class ru.coko.ege.domain.model.** { *; }
-keep class ru.coko.ege.data.repository.CokoRepository$DashboardData { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
