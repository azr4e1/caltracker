package com.lorenzo.caltracker.data.backup

import android.content.Context
import android.net.Uri
import com.lorenzo.caltracker.data.db.AppDatabase
import java.time.OffsetDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class BackupRepository(private val context: Context, private val database: AppDatabase) {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    /** Writes a full backup to [uri]. Returns the number of log entries exported. */
    suspend fun export(uri: Uri): Int = withContext(Dispatchers.IO) {
        val dao = database.backupDao()
        val backup = Backup(
            schemaVersion = AppDatabase.VERSION,
            exportedAt = OffsetDateTime.now().toString(),
            foods = dao.foods(),
            recipes = dao.recipes(),
            recipeIngredients = dao.recipeIngredients(),
            logEntries = dao.logEntries(),
            profile = dao.profile(),
            goals = dao.goals(),
            weightEntries = dao.weightEntries(),
        )
        val output = context.contentResolver.openOutputStream(uri, "wt") ?: error("Cannot open file for writing")
        output.bufferedWriter().use { it.write(json.encodeToString(Backup.serializer(), backup)) }
        backup.logEntries.size
    }

    /** Permanently deletes all data: diary, foods, recipes, weigh-ins, profile and goals. */
    suspend fun deleteAll() = withContext(Dispatchers.IO) { database.backupDao().deleteAll() }

    /** Replaces all data with the backup at [uri]. Returns the number of log entries restored. */
    suspend fun import(uri: Uri): Int = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri) ?: error("Cannot open file")
        val backup = input.bufferedReader().use { json.decodeFromString(Backup.serializer(), it.readText()) }
        require(backup.schemaVersion <= AppDatabase.VERSION) {
            "Backup is from a newer version of the app (schema ${backup.schemaVersion})"
        }
        database.backupDao().replaceAll(backup)
        backup.logEntries.size
    }
}
