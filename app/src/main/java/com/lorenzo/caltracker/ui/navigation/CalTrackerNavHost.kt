package com.lorenzo.caltracker.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.lorenzo.caltracker.ui.addfood.AddFoodScreen
import com.lorenzo.caltracker.ui.diary.DiaryScreen
import com.lorenzo.caltracker.ui.foodeditor.FoodEditorScreen
import com.lorenzo.caltracker.ui.logfood.LogFoodScreen
import com.lorenzo.caltracker.ui.logfood.LogFoodViewModel
import com.lorenzo.caltracker.ui.quickadd.QuickAddScreen
import com.lorenzo.caltracker.ui.quickadd.QuickAddViewModel
import com.lorenzo.caltracker.ui.settings.SettingsScreen
import kotlin.reflect.KClass

private enum class Tab(val route: Any, val routeClass: KClass<*>, val label: String, val icon: ImageVector) {
    DIARY(DiaryRoute, DiaryRoute::class, "Diary", Icons.Filled.Home),
    HISTORY(HistoryRoute, HistoryRoute::class, "History", Icons.Filled.DateRange),
    SETTINGS(SettingsRoute, SettingsRoute::class, "Settings", Icons.Filled.Settings),
}

@Composable
fun CalTrackerNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val currentTab = Tab.entries.firstOrNull { tab -> destination?.hasRoute(tab.routeClass) == true }

    Scaffold(
        // Each screen handles the status bar itself; this outer scaffold only adds the bottom bar.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (currentTab != null) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == currentTab,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = DiaryRoute,
            modifier = Modifier.padding(padding),
        ) {
            composable<DiaryRoute> {
                DiaryScreen(onAddFood = { date -> navController.navigate(AddFoodRoute(date.toEpochDay())) })
            }
            composable<HistoryRoute> { Placeholder("History: coming in Phase 2") }
            composable<SettingsRoute> { SettingsScreen() }

            composable<AddFoodRoute> { entry ->
                val route = entry.toRoute<AddFoodRoute>()
                AddFoodScreen(
                    onBack = navController::popBackStack,
                    onFoodSelected = { foodId -> navController.navigate(LogFoodRoute(foodId, route.epochDay)) },
                    onQuickAdd = { navController.navigate(QuickAddRoute(route.epochDay)) },
                    onNewFood = { navController.navigate(NewFoodRoute(route.epochDay)) },
                )
            }
            composable<LogFoodRoute> { entry ->
                val route = entry.toRoute<LogFoodRoute>()
                LogFoodScreen(
                    viewModel = viewModel(factory = LogFoodViewModel.factory(route)),
                    onBack = navController::popBackStack,
                    onSaved = navController::backToDiary,
                )
            }
            composable<QuickAddRoute> { entry ->
                val route = entry.toRoute<QuickAddRoute>()
                QuickAddScreen(
                    viewModel = viewModel(factory = QuickAddViewModel.factory(route)),
                    onBack = navController::popBackStack,
                    onSaved = navController::backToDiary,
                )
            }
            composable<NewFoodRoute> { entry ->
                val route = entry.toRoute<NewFoodRoute>()
                FoodEditorScreen(
                    onBack = navController::popBackStack,
                    onSaved = { foodId ->
                        // Replace the editor with the log screen, so Back returns to the food list.
                        navController.navigate(LogFoodRoute(foodId, route.epochDay)) {
                            popUpTo<NewFoodRoute> { inclusive = true }
                        }
                    },
                )
            }
        }
    }
}

private fun NavHostController.backToDiary() {
    popBackStack<DiaryRoute>(inclusive = false)
}

@Composable
private fun Placeholder(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text) }
}
