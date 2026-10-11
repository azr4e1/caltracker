package com.lorenzo.caltracker.data.export

import com.lorenzo.caltracker.data.db.entity.EntryUnit
import com.lorenzo.caltracker.data.db.entity.Food
import com.lorenzo.caltracker.data.db.entity.Goals
import com.lorenzo.caltracker.data.db.entity.LogEntry
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.util.zip.ZipFile
import org.junit.Assert.assertTrue
import org.junit.Test

class ExcelExporterTest {
    @Test
    fun writesWorkbookWithThreeSheetsAndTheData() {
        val day = LocalDate.of(2026, 10, 11)
        val entries = listOf(
            LogEntry(date = day, time = LocalTime.of(8, 30), foodId = 1, name = "Oats", amount = 80.0, unit = EntryUnit.G,
                kcal = 300.0, protein = 10.0, carbs = 50.0, fat = 6.0),
            LogEntry(date = day, time = LocalTime.of(21, 15), name = "Snack", amount = 1.0, unit = EntryUnit.QUICK,
                kcal = 200.0, protein = 0.0, carbs = 0.0, fat = 0.0),
        )
        val foods = listOf(Food(id = 1, name = "Oats", kcalPer100 = 375.0, proteinPer100 = 12.5, carbsPer100 = 62.5, fatPer100 = 7.5))
        val goals = Goals(kcal = 2600.0, protein = 186.0, carbs = 302.0, fat = 72.0)

        val file = File.createTempFile("export", ".xlsx").apply { deleteOnExit() }
        file.outputStream().use { ExcelExporter.write(it, entries, foods, goals) }

        // Read via the central directory, like Excel does (the zip is written in streaming mode).
        val files = ZipFile(file).use { zip ->
            zip.entries().asSequence().associate { it.name to zip.getInputStream(it).readBytes().decodeToString() }
        }
        assertTrue(files.keys.toString(), files.keys.containsAll(listOf("xl/worksheets/sheet1.xml", "xl/worksheets/sheet2.xml", "xl/worksheets/sheet3.xml")))
        val workbook = files.getValue("xl/workbook.xml")
        listOf("Entries", "Daily totals", "Foods").forEach { assertTrue(it, workbook.contains("name=\"$it\"")) }
        val strings = files.getValue("xl/sharedStrings.xml")
        listOf("Oats", "Snack", "Quick add").forEach { assertTrue(it, strings.contains(it)) }
    }
}
