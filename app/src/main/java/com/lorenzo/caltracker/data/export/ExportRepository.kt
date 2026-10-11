package com.lorenzo.caltracker.data.export

import android.content.Context
import android.net.Uri
import com.lorenzo.caltracker.data.repository.FoodRepository
import com.lorenzo.caltracker.data.repository.LogRepository
import com.lorenzo.caltracker.data.repository.RecipeRepository
import com.lorenzo.caltracker.data.repository.SettingsRepository
import com.lorenzo.caltracker.data.repository.WeightRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class ExportRepository(
    private val context: Context,
    private val logRepository: LogRepository,
    private val foodRepository: FoodRepository,
    private val recipeRepository: RecipeRepository,
    private val settingsRepository: SettingsRepository,
    private val weightRepository: WeightRepository,
) {
    /** Writes the Excel workbook to [uri]. Returns the number of entries exported. */
    suspend fun exportExcel(uri: Uri): Int = withContext(Dispatchers.IO) {
        val entries = logRepository.getAll()
        val output = context.contentResolver.openOutputStream(uri, "wt") ?: error("Cannot open file for writing")
        output.use { ExcelExporter.write(it, entries, foodRepository.getAll(), recipeRepository.getAll(), weightRepository.getAll(), settingsRepository.goals.first()) }
        entries.size
    }
}
