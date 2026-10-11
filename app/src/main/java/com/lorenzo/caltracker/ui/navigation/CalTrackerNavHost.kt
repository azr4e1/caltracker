package com.lorenzo.caltracker.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.lorenzo.caltracker.ui.addfood.AddFoodScreen
import com.lorenzo.caltracker.ui.diary.DiaryScreen
import com.lorenzo.caltracker.ui.foodeditor.FoodEditorScreen
import com.lorenzo.caltracker.ui.foodeditor.FoodEditorViewModel
import com.lorenzo.caltracker.ui.history.HistoryScreen
import com.lorenzo.caltracker.ui.logfood.LogFoodScreen
import com.lorenzo.caltracker.ui.logfood.LogFoodViewModel
import com.lorenzo.caltracker.ui.profile.ProfileEditorScreen
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
fun CalTrackerNavHost(needsSetup: Boolean) {
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
                            onClick = { navController.navigateToTab(tab.route) },
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
            startDestination = if (needsSetup) ProfileEditorRoute(isSetup = true) else DiaryRoute,
            modifier = Modifier.padding(padding),
        ) {
            composable<DiaryRoute> {
                DiaryScreen(onAddFood = { date -> navController.navigate(AddFoodRoute(date.toEpochDay())) })
            }
            composable<HistoryRoute> { HistoryScreen(onOpenDay = { navController.navigateToTab(DiaryRoute) }) }
            composable<SettingsRoute> {
                SettingsScreen(onEditProfile = { navController.navigate(ProfileEditorRoute()) })
            }
            composable<EditFoodRoute> { entry ->
                val route = entry.toRoute<EditFoodRoute>()
                FoodEditorScreen(
                    viewModel = viewModel(factory = FoodEditorViewModel.factory(route.foodId)),
                    onBack = navController::popBackStack,
                    onSaved = { navController.popBackStack() },
                )
            }
            composable<ProfileEditorRoute> { entry ->
                val route = entry.toRoute<ProfileEditorRoute>()
                ProfileEditorScreen(
                    isSetup = route.isSetup,
                    onClose = {
                        if (route.isSetup) {
                            navController.navigate(DiaryRoute) { popUpTo<ProfileEditorRoute> { inclusive = true } }
                        } else {
                            navController.popBackStack()
                        }
                    },
                )
            }

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
                    onEdit = { foodId -> navController.navigate(EditFoodRoute(foodId)) },
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
                    viewModel = viewModel(factory = FoodEditorViewModel.factory(foodId = null)),
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

/** Switches bottom-bar tab, keeping each tab's state. */
private fun NavHostController.navigateToTab(route: Any) {
    navigate(route) {
        popUpTo<DiaryRoute> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.backToDiary() {
    popBackStack<DiaryRoute>(inclusive = false)
}
