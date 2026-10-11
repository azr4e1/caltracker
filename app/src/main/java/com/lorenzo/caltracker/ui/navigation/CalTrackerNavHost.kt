package com.lorenzo.caltracker.ui.navigation

import android.widget.Toast
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.lorenzo.caltracker.data.remote.RemoteFood
import com.lorenzo.caltracker.ui.addfood.AddFoodScreen
import com.lorenzo.caltracker.ui.plural
import com.lorenzo.caltracker.ui.diary.DiaryScreen
import com.lorenzo.caltracker.ui.foodeditor.FoodEditorScreen
import com.lorenzo.caltracker.ui.foodeditor.FoodEditorViewModel
import com.lorenzo.caltracker.ui.history.HistoryScreen
import com.lorenzo.caltracker.ui.logfood.LogFoodScreen
import com.lorenzo.caltracker.ui.logfood.LogFoodViewModel
import com.lorenzo.caltracker.ui.profile.ProfileEditorScreen
import com.lorenzo.caltracker.ui.quickadd.QuickAddScreen
import com.lorenzo.caltracker.ui.recipe.LogRecipeScreen
import com.lorenzo.caltracker.ui.recipe.LogRecipeViewModel
import com.lorenzo.caltracker.ui.recipe.RecipeEditorScreen
import com.lorenzo.caltracker.ui.recipe.RecipeEditorViewModel
import com.lorenzo.caltracker.ui.quickadd.QuickAddViewModel
import com.lorenzo.caltracker.ui.settings.SettingsScreen
import java.time.LocalDate
import kotlin.reflect.KClass
import kotlinx.serialization.json.Json

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
            startDestination = if (needsSetup) ProfileEditorRoute(EditorSection.BOTH, isSetup = true) else DiaryRoute,
            modifier = Modifier.padding(padding),
        ) {
            composable<DiaryRoute> {
                DiaryScreen(
                    onAddFood = { date -> navController.navigate(AddFoodRoute(date.toEpochDay())) },
                    onCreateRecipe = { ids ->
                        navController.navigate(
                            RecipeEditorRoute(epochDay = LocalDate.now().toEpochDay(), fromEntryIds = ids.joinToString(","), logAfterSave = false),
                        )
                    },
                )
            }
            composable<HistoryRoute> { HistoryScreen(onOpenDay = { navController.navigateToTab(DiaryRoute) }) }
            composable<SettingsRoute> {
                SettingsScreen(
                    onEditProfile = { navController.navigate(ProfileEditorRoute(EditorSection.PROFILE)) },
                    onEditGoals = { navController.navigate(ProfileEditorRoute(EditorSection.GOALS)) },
                )
            }
            composable<RecipeEditorRoute> { entry ->
                val route = entry.toRoute<RecipeEditorRoute>()
                val fromEntries = route.fromEntryIds?.split(',')?.mapNotNull { it.toLongOrNull() }.orEmpty()
                val viewModel: RecipeEditorViewModel = viewModel(factory = RecipeEditorViewModel.factory(route.recipeId, fromEntries))
                val context = LocalContext.current
                // A food chosen in the picker comes back through this entry's saved state.
                val picked by entry.savedStateHandle.getStateFlow<Long?>(PICKED_FOOD_ID, null).collectAsStateWithLifecycle()
                LaunchedEffect(picked) {
                    picked?.let {
                        viewModel.onFoodPicked(it)
                        entry.savedStateHandle[PICKED_FOOD_ID] = null
                    }
                }
                RecipeEditorScreen(
                    viewModel = viewModel,
                    logAfterSave = route.logAfterSave,
                    onAddIngredient = { navController.navigate(AddFoodRoute(route.epochDay, forRecipe = true)) },
                    onBack = navController::popBackStack,
                    onSaved = { recipeId ->
                        if (!route.logAfterSave) {
                            val replaced = viewModel.replacedCount
                            val text = if (replaced > 0) "Recipe saved and logged in place of ${plural(replaced, "entry", "entries")}" else "Recipe saved"
                            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        } else if (route.recipeId == 0L) {
                            navController.navigate(LogRecipeRoute(recipeId, route.epochDay)) {
                                popUpTo<RecipeEditorRoute> { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    },
                )
            }
            composable<LogRecipeRoute> { entry ->
                val route = entry.toRoute<LogRecipeRoute>()
                LogRecipeScreen(
                    viewModel = viewModel(factory = LogRecipeViewModel.factory(route)),
                    onBack = navController::popBackStack,
                    onSaved = navController::backToDiary,
                    onEdit = { recipeId -> navController.navigate(RecipeEditorRoute(recipeId, route.epochDay)) },
                )
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
                    section = route.section,
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
                    pickingIngredient = route.forRecipe,
                    onBack = navController::popBackStack,
                    onFoodSelected = { foodId ->
                        if (route.forRecipe) navController.deliverPickedFood(foodId) else navController.navigate(LogFoodRoute(foodId, route.epochDay))
                    },
                    onEditDraft = { draft ->
                        val json = Json.encodeToString(RemoteFood.serializer(), draft)
                        navController.navigate(NewFoodRoute(route.epochDay, json, forRecipe = route.forRecipe))
                    },
                    onQuickAdd = { navController.navigate(QuickAddRoute(route.epochDay)) },
                    onNewFood = { navController.navigate(NewFoodRoute(route.epochDay, forRecipe = route.forRecipe)) },
                    onRecipeSelected = { recipeId -> navController.navigate(LogRecipeRoute(recipeId, route.epochDay)) },
                    onNewRecipe = { navController.navigate(RecipeEditorRoute(epochDay = route.epochDay)) },
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
                val draft = route.draftJson?.let { Json.decodeFromString(RemoteFood.serializer(), it) }
                FoodEditorScreen(
                    viewModel = viewModel(factory = FoodEditorViewModel.factory(foodId = null, draft = draft)),
                    onBack = navController::popBackStack,
                    onSaved = { foodId ->
                        if (route.forRecipe) {
                            navController.deliverPickedFood(foodId)
                        } else {
                            // Replace the editor with the log screen, so Back returns to the food list.
                            navController.navigate(LogFoodRoute(foodId, route.epochDay)) {
                                popUpTo<NewFoodRoute> { inclusive = true }
                            }
                        }
                    },
                )
            }
        }
    }
}

private const val PICKED_FOOD_ID = "pickedFoodId"

/** Returns a food chosen as a recipe ingredient to the recipe editor, closing the picker screens. */
private fun NavHostController.deliverPickedFood(foodId: Long) {
    getBackStackEntry<RecipeEditorRoute>().savedStateHandle[PICKED_FOOD_ID] = foodId
    popBackStack<RecipeEditorRoute>(inclusive = false)
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
