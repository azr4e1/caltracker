package com.lorenzo.caltracker.data.export

import com.lorenzo.caltracker.data.db.dao.RecipeWithIngredients
import com.lorenzo.caltracker.data.db.entity.EntryUnit
import com.lorenzo.caltracker.data.db.entity.Food
import com.lorenzo.caltracker.data.db.entity.Goals
import com.lorenzo.caltracker.data.db.entity.LogEntry
import com.lorenzo.caltracker.data.db.entity.WeightEntry
import com.lorenzo.caltracker.domain.nutrientsFor
import java.io.OutputStream
import org.dhatim.fastexcel.Workbook
import org.dhatim.fastexcel.Worksheet

/**
 * Writes the diary as an .xlsx workbook: every entry, one row per day, the saved foods, and the
 * recipes (one row per ingredient), and the weigh-ins. Dates and times are real Excel values, so they sort and filter correctly.
 */
object ExcelExporter {
    private const val DATE_FORMAT = "yyyy-mm-dd"
    private const val TIME_FORMAT = "hh:mm"
    private const val NUMBER_FORMAT = "0.0"

    fun write(
        output: OutputStream,
        entries: List<LogEntry>,
        foods: List<Food>,
        recipes: List<RecipeWithIngredients>,
        weights: List<WeightEntry>,
        goals: Goals?,
    ) {
        val workbook = Workbook(output, "CalTracker", "1.0")
        writeEntries(workbook.newWorksheet("Entries"), entries)
        writeDailyTotals(workbook.newWorksheet("Daily totals"), entries, goals)
        writeFoods(workbook.newWorksheet("Foods"), foods)
        writeRecipes(workbook.newWorksheet("Recipes"), recipes)
        writeWeights(workbook.newWorksheet("Weight"), weights)
        workbook.finish()
    }

    private fun writeEntries(ws: Worksheet, entries: List<LogEntry>) {
        header(ws, "Date", "Time", "Name", "Amount", "Unit", "kcal", "Protein (g)", "Carbs (g)", "Fat (g)", "Type")
        entries.forEachIndexed { i, e ->
            val r = i + 1
            date(ws, r, 0, e.date)
            ws.value(r, 1, e.time.toSecondOfDay() / 86_400.0)
            ws.style(r, 1).format(TIME_FORMAT).set()
            ws.value(r, 2, e.name)
            if (!e.isQuickAdd) {
                ws.value(r, 3, e.amount)
                ws.value(r, 4, e.unit.excelLabel)
            }
            numbers(ws, r, 5, e.kcal, e.protein, e.carbs, e.fat)
            ws.value(
                r, 9,
                when {
                    e.isQuickAdd -> "Quick add"
                    e.recipeId != null -> "Recipe"
                    else -> "Food"
                },
            )
        }
        widths(ws, 12, 8, 30, 9, 9, 9, 12, 11, 9, 11)
    }

    private fun writeDailyTotals(ws: Worksheet, entries: List<LogEntry>, goals: Goals?) {
        header(ws, "Date", "kcal", "Protein (g)", "Carbs (g)", "Fat (g)", "Entries", "Goal kcal", "Difference")
        entries.groupBy { it.date }.toSortedMap().entries.forEachIndexed { i, (date, day) ->
            val r = i + 1
            val kcal = day.sumOf { it.kcal }
            date(ws, r, 0, date)
            numbers(ws, r, 1, kcal, day.sumOf { it.protein }, day.sumOf { it.carbs }, day.sumOf { it.fat })
            ws.value(r, 5, day.size)
            if (goals != null) numbers(ws, r, 6, goals.kcal, kcal - goals.kcal)
        }
        widths(ws, 12, 9, 12, 11, 9, 9, 10, 11)
    }

    private fun writeFoods(ws: Worksheet, foods: List<Food>) {
        header(
            ws, "Name", "Brand", "Barcode", "Per 100", "kcal", "Protein (g)", "Carbs (g)", "Fat (g)",
            "Fiber (g)", "Sugar (g)", "Sat. fat (g)", "Salt (g)", "Serving", "Serving size", "Source",
        )
        foods.forEachIndexed { i, f ->
            val r = i + 1
            ws.value(r, 0, f.name)
            ws.value(r, 1, f.brand)
            ws.value(r, 2, f.barcode)
            ws.value(r, 3, f.baseUnit.name.lowercase())
            numbers(ws, r, 4, f.kcalPer100, f.proteinPer100, f.carbsPer100, f.fatPer100)
            numbers(ws, r, 8, f.fiberPer100, f.sugarPer100, f.saturatedFatPer100, f.saltPer100)
            ws.value(r, 12, f.servingName)
            f.servingSize?.let { ws.value(r, 13, it) }
            ws.value(r, 14, f.source.name.lowercase())
        }
        widths(ws, 30, 16, 15, 8, 8, 12, 11, 9, 10, 10, 12, 9, 12, 13, 10)
    }

    private fun writeRecipes(ws: Worksheet, recipes: List<RecipeWithIngredients>) {
        header(ws, "Recipe", "Portions", "Cooked weight (g)", "Ingredient", "Amount", "Unit", "kcal", "Protein (g)", "Carbs (g)", "Fat (g)")
        var r = 1
        recipes.forEach { recipe ->
            recipe.ingredients.sortedBy { it.ingredient.position }.forEach { (ingredient, food) ->
                val n = food.nutrientsFor(ingredient.amount)
                ws.value(r, 0, recipe.recipe.name)
                ws.value(r, 1, recipe.recipe.servings)
                recipe.recipe.totalWeight?.let { ws.value(r, 2, it) }
                ws.value(r, 3, food.name)
                ws.value(r, 4, ingredient.amount)
                ws.value(r, 5, food.baseUnit.name.lowercase())
                numbers(ws, r, 6, n.kcal, n.protein, n.carbs, n.fat)
                r++
            }
        }
        widths(ws, 24, 9, 16, 28, 9, 6, 8, 12, 11, 9)
    }

    private fun writeWeights(ws: Worksheet, weights: List<WeightEntry>) {
        header(ws, "Date", "Weight (kg)")
        weights.forEachIndexed { i, w ->
            date(ws, i + 1, 0, w.date)
            numbers(ws, i + 1, 1, w.weightKg)
        }
        widths(ws, 12, 12)
    }

    private fun header(ws: Worksheet, vararg titles: String) {
        titles.forEachIndexed { c, title -> ws.value(0, c, title) }
        ws.range(0, 0, 0, titles.lastIndex).style().bold().set()
        ws.freezePane(0, 1)
    }

    private fun date(ws: Worksheet, r: Int, c: Int, date: java.time.LocalDate) {
        ws.value(r, c, date)
        ws.style(r, c).format(DATE_FORMAT).set()
    }

    /** Writes consecutive numeric cells starting at column [start]; nulls are left blank. */
    private fun numbers(ws: Worksheet, r: Int, start: Int, vararg values: Double?) {
        values.forEachIndexed { i, v ->
            if (v != null) {
                ws.value(r, start + i, v)
                ws.style(r, start + i).format(NUMBER_FORMAT).set()
            }
        }
    }

    private fun widths(ws: Worksheet, vararg widths: Int) {
        widths.forEachIndexed { c, w -> ws.width(c, w.toDouble()) }
    }

    private val EntryUnit.excelLabel: String
        get() = when (this) {
            EntryUnit.G -> "g"
            EntryUnit.ML -> "ml"
            EntryUnit.SERVING -> "serving"
            EntryUnit.QUICK -> ""
        }
}
