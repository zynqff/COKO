package ru.coko.ege.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ru.coko.ege.data.repository.CokoRepository
import ru.coko.ege.presentation.login.LoginScreen
import ru.coko.ege.presentation.main.MainScreen
import ru.coko.ege.presentation.privacy.PrivacyScreen
import ru.coko.ege.presentation.splash.SplashScreen

@Composable
fun CokoNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToMain = { dashboard ->
                    navigateToMain(navController, dashboard)
                }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onNavigateToPrivacy = { navController.navigate(Routes.PRIVACY) },
                onLoginSuccess = { dashboard ->
                    navigateToMain(navController, dashboard)
                }
            )
        }

        composable(Routes.PRIVACY) {
            PrivacyScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.MAIN) {
            val dashboard = pendingDashboard
                ?: CokoRepository.DashboardData(profile = null, exams = emptyList())

            MainScreen(
                initialDashboard = dashboard,
                onLoggedOut = {
                    pendingDashboard = null
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}

/**
 * Простой способ передать "тяжёлые" данные дэшборда между экранами навигации
 * без сериализации через NavArgs (Compose Navigation плохо работает с
 * сложными объектами в аргументах). Так как переход на MAIN происходит
 * только из Splash/Login внутри одного процесса — это безопасно.
 */
private var pendingDashboard: CokoRepository.DashboardData? = null

private fun navigateToMain(
    navController: androidx.navigation.NavController,
    dashboard: CokoRepository.DashboardData
) {
    pendingDashboard = dashboard
    navController.navigate(Routes.MAIN) {
        popUpTo(0) { inclusive = true }
    }
}
