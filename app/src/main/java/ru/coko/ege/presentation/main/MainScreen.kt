package ru.coko.ege.presentation.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ru.coko.ege.data.repository.CokoRepository
import ru.coko.ege.presentation.help.HelpScreen
import ru.coko.ege.presentation.navigation.Routes
import ru.coko.ege.presentation.profile.ProfileScreen
import ru.coko.ege.presentation.results.ResultDetailScreen

private data class BottomTab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomTabs = listOf(
    BottomTab(Routes.MAIN_HOME, "Главная", Icons.Filled.Home),
    BottomTab(Routes.RESULTS, "Результаты", Icons.Filled.Assessment),
    BottomTab(Routes.HELP, "Помощь", Icons.Filled.Help),
    BottomTab(Routes.PROFILE, "Профиль", Icons.Filled.Person)
)

@Composable
fun MainScreen(
    initialDashboard: CokoRepository.DashboardData,
    onLoggedOut: () -> Unit
) {
    val mainViewModel: MainViewModel = hiltViewModel()
    val navController = rememberNavController()

    // Заполняем общий ViewModel данными, полученными при логине/сплеш-автологине,
    // чтобы все вкладки сразу видели актуальный профиль и список экзаменов.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        mainViewModel.setInitialData(initialDashboard)
    }

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            NavigationBar {
                bottomTabs.forEach { tab ->
                    val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.MAIN_HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.MAIN_HOME) {
                HomeScreen(viewModel = mainViewModel)
            }
            composable(Routes.RESULTS) {
                ResultsScreen(
                    viewModel = mainViewModel,
                    onOpenDetail = { examId ->
                        navController.navigate(Routes.resultDetail(examId))
                    }
                )
            }
            composable(Routes.RESULT_DETAIL) {
                ResultDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.HELP) {
                HelpScreen()
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    viewModel = mainViewModel,
                    onLoggedOut = onLoggedOut
                )
            }
        }
    }
}
