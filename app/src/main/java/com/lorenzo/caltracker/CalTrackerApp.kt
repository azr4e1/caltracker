package com.lorenzo.caltracker

import android.app.Application
import com.lorenzo.caltracker.data.db.AppDatabase
import com.lorenzo.caltracker.data.repository.LogRepository

/** Holds the app-wide singletons. Screens get their dependencies from here. */
class AppContainer(app: Application) {
    val database: AppDatabase by lazy { AppDatabase.create(app) }
    val logRepository: LogRepository by lazy { LogRepository(database.logEntryDao()) }
}

class CalTrackerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
