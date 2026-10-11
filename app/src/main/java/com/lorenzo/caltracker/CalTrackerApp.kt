package com.lorenzo.caltracker

import android.app.Application
import com.lorenzo.caltracker.widget.TodayWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import com.lorenzo.caltracker.data.backup.BackupRepository
import com.lorenzo.caltracker.data.db.AppDatabase
import com.lorenzo.caltracker.data.export.ExportRepository
import com.lorenzo.caltracker.data.repository.DiaryDateHolder
import com.lorenzo.caltracker.data.repository.FoodRepository
import com.lorenzo.caltracker.data.repository.LogRepository
import com.lorenzo.caltracker.data.repository.OnlineFoodRepository
import com.lorenzo.caltracker.data.repository.PreferencesRepository
import com.lorenzo.caltracker.data.repository.RecipeRepository
import com.lorenzo.caltracker.data.repository.SettingsRepository
import com.lorenzo.caltracker.data.repository.WeightRepository

/** Holds the app-wide singletons. Screens get their dependencies from here. */
class AppContainer(app: Application) {
    val database: AppDatabase by lazy { AppDatabase.create(app) }
    val logRepository: LogRepository by lazy { LogRepository(database) }
    val foodRepository: FoodRepository by lazy { FoodRepository(database.foodDao()) }
    val recipeRepository: RecipeRepository by lazy { RecipeRepository(database) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(database.settingsDao()) }
    val backupRepository: BackupRepository by lazy { BackupRepository(app, database) }
    val exportRepository: ExportRepository by lazy {
        ExportRepository(app, logRepository, foodRepository, recipeRepository, settingsRepository, weightRepository)
    }
    val weightRepository: WeightRepository by lazy { WeightRepository(database.weightDao(), settingsRepository) }
    val onlineFoodRepository: OnlineFoodRepository by lazy { OnlineFoodRepository(foodRepository, preferencesRepository) }
    val preferencesRepository: PreferencesRepository by lazy { PreferencesRepository(app) }
    val diaryDate = DiaryDateHolder()
}

@OptIn(FlowPreview::class)
class CalTrackerApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Redraw the home-screen widget whenever the diary or the goals change.
        appScope.launch {
            container.database.invalidationTracker.createFlow("log_entries", "goals", emitInitialState = false)
                .debounce(500)
                .collect { TodayWidget.refresh(this@CalTrackerApp) }
        }
    }
}
