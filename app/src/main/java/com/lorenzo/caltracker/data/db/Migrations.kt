package com.lorenzo.caltracker.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database upgrades. Each one turns version N into N+1 without losing data; Room runs the ones
 * needed when the app starts. Never edit a migration once it has shipped; add a new one.
 */

/** 1 -> 2: quick adds get their own unit instead of being "1 serving" with no food. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(QUICK_ADD_UNIT_SQL)
    }
}

/** 2 -> 3: recipes can store the weight of the finished dish. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE recipes ADD COLUMN totalWeight REAL")
    }
}

/** Shared with backup restore, so old backups get the same fix. */
const val QUICK_ADD_UNIT_SQL =
    "UPDATE log_entries SET unit = 'QUICK' WHERE foodId IS NULL AND recipeId IS NULL AND unit = 'SERVING'"

val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
