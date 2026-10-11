package com.lorenzo.caltracker

import android.app.Application
import com.lorenzo.caltracker.data.backup.BackupRepository
import com.lorenzo.caltracker.data.db.AppDatabase
import com.lorenzo.caltracker.data.export.ExportRepository
import com.lorenzo.caltracker.data.repository.DiaryDateHolder
import com.lorenzo.caltracker.data.repository.FoodRepository
import com.lorenzo.caltracker.data.repository.LogRepository
import com.lorenzo.caltracker.data.repository.SettingsRepository

/** Holds the app-wide singletons. Screens get their dependencies from here. */
class AppContainer(app: Application) {
    val database: AppDatabase by lazy { AppDatabase.create(app) }
    val logRepository: LogRepository by lazy { LogRepository(database.logEntryDao()) }
    val foodRepository: FoodRepository by lazy { FoodRepository(database.foodDao()) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(database.settingsDao()) }
    val backupRepository: BackupRepository by lazy { BackupRepository(app, database) }
    val exportRepository: ExportRepository by lazy {
        ExportRepository(app, logRepository, foodRepository, settingsRepository)
    }
    val diaryDate = DiaryDateHolder()
}

class CalTrackerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
