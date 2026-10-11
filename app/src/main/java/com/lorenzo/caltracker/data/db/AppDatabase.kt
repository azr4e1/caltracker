package com.lorenzo.caltracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.lorenzo.caltracker.data.db.dao.BackupDao
import com.lorenzo.caltracker.data.db.dao.FoodDao
import com.lorenzo.caltracker.data.db.dao.LogEntryDao
import com.lorenzo.caltracker.data.db.dao.RecipeDao
import com.lorenzo.caltracker.data.db.dao.SettingsDao
import com.lorenzo.caltracker.data.db.entity.Food
import com.lorenzo.caltracker.data.db.entity.Goals
import com.lorenzo.caltracker.data.db.entity.LogEntry
import com.lorenzo.caltracker.data.db.entity.Profile
import com.lorenzo.caltracker.data.db.entity.Recipe
import com.lorenzo.caltracker.data.db.entity.RecipeIngredient

@Database(
    entities = [Food::class, Recipe::class, RecipeIngredient::class, LogEntry::class, Profile::class, Goals::class],
    version = AppDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun recipeDao(): RecipeDao
    abstract fun logEntryDao(): LogEntryDao
    abstract fun settingsDao(): SettingsDao
    abstract fun backupDao(): BackupDao

    companion object {
        const val VERSION = 2


        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "caltracker.db")
                .addMigrations(*ALL_MIGRATIONS)
                .build()
    }
}
