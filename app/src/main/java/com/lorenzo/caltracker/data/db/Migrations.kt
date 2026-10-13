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

/** 3 -> 4: weight tracking. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `weight_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`date` TEXT NOT NULL, `weightKg` REAL NOT NULL, `createdAt` INTEGER NOT NULL)",
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_weight_entries_date` ON `weight_entries` (`date`)")
    }
}

/**
 * 4 -> 5: goals are kept per day. The current goals become the first row, in force from
 * [GoalPeriod.BEGINNING], so every past day keeps them when the goals next change.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `goal_history` (`date` TEXT NOT NULL, `kcal` REAL NOT NULL, `protein` REAL NOT NULL, " +
                "`carbs` REAL NOT NULL, `fat` REAL NOT NULL, PRIMARY KEY(`date`))",
        )
        db.execSQL(
            "INSERT INTO goal_history (date, kcal, protein, carbs, fat) " +
                "SELECT '1970-01-01', kcal, protein, carbs, fat FROM goals WHERE id = 1",
        )
    }
}

/** Shared with backup restore, so old backups get the same fix. */
const val QUICK_ADD_UNIT_SQL =
    "UPDATE log_entries SET unit = 'QUICK' WHERE foodId IS NULL AND recipeId IS NULL AND unit = 'SERVING'"

val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
