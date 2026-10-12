package com.lorenzo.caltracker.ui.addfood

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.lorenzo.caltracker.data.db.dao.RecipeWithIngredients
import com.lorenzo.caltracker.data.db.entity.BaseUnit
import com.lorenzo.caltracker.data.db.entity.Food
import com.lorenzo.caltracker.data.remote.RemoteFood
import com.lorenzo.caltracker.domain.nutrition
import com.lorenzo.caltracker.ui.formatAmount
import com.lorenzo.caltracker.ui.formatWhole
import com.lorenzo.caltracker.ui.plural

/**
 * Finds a food to log: saved foods, recipes, online search, barcode scan.
 * With [pickingIngredient] it picks a recipe ingredient instead: no quick add and no recipes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodScreen(
    pickingIngredient: Boolean,
    onBack: () -> Unit,
    onFoodSelected: (Long) -> Unit,
    onEditDraft: (RemoteFood) -> Unit,
    onQuickAdd: () -> Unit,
    onNewFood: () -> Unit,
    onRecipeSelected: (Long) -> Unit,
    onNewRecipe: () -> Unit,
    onNext: () -> Unit,
    viewModel: AddFoodViewModel = viewModel(factory = AddFoodViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pending by viewModel.pending.collectAsStateWithLifecycle()
    var confirmDiscard by remember { mutableStateOf(false) }
    // Leaving with foods picked but not logged asks first.
    val leave = { if (!pickingIngredient && pending.isNotEmpty()) confirmDiscard = true else onBack() }
    BackHandler(enabled = !pickingIngredient && pending.isNotEmpty()) { confirmDiscard = true }
    var expandedSections by remember(viewModel.online?.query) { mutableStateOf(emptySet<String>()) }
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    fun search() {
        keyboard?.hide()
        viewModel.searchOnline()
    }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel.message) {
        viewModel.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.message = null
        }
    }
    val handler = remember(onFoodSelected, onEditDraft) {
        object : FoodChoiceHandler {
            override fun onFoodReady(foodId: Long) = onFoodSelected(foodId)
            override fun onNeedsEditing(draft: RemoteFood) = onEditDraft(draft)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (pickingIngredient) "Add ingredient" else "Add food") },
                navigationIcon = {
                    IconButton(onClick = leave) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (!pickingIngredient && pending.isNotEmpty()) {
                        Button(onClick = onNext, modifier = Modifier.padding(end = 8.dp)) { Text("Next (${pending.size})") }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
            if (viewModel.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                placeholder = { Text("Search foods") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { search() }),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    onClick = { scanBarcode(context, onScanned = { viewModel.lookupBarcode(it, handler) }, onError = { viewModel.message = it }) },
                    enabled = !viewModel.busy,
                    modifier = Modifier.weight(1f),
                ) { Text("Scan") }
                if (!pickingIngredient) {
                    FilledTonalButton(onClick = onQuickAdd, modifier = Modifier.weight(1f)) { Text("Quick add") }
                }
                FilledTonalButton(onClick = onNewFood, modifier = Modifier.weight(1f)) { Text("New food") }
            }
            // Drawn only once loaded: sections arriving later would be inserted above the
            // visible items and end up scrolled out of view.
            if (state.loaded) LazyColumn(Modifier.fillMaxSize()) {
                if (state.query.isNotBlank()) {
                    if (!pickingIngredient && state.recipes.isNotEmpty()) recipeSection(state.recipes, onRecipeSelected, onNewRecipe)
                    savedFoodSection("Your foods", state.results, onFoodSelected, empty = "No saved food matches.")
                    onlineSection(
                        state.query.trim(), viewModel.online, expandedSections, { expandedSections = expandedSections + it }, ::search,
                        viewModel.usesSharedUsdaKey,
                    ) { viewModel.choose(it, handler) }
                } else {
                    if (state.recent.isNotEmpty()) savedFoodSection("Recent", state.recent, onFoodSelected)
                    if (!pickingIngredient) recipeSection(state.recipes, onRecipeSelected, onNewRecipe)
                    savedFoodSection(
                        "All foods", state.all, onFoodSelected,
                        empty = "No saved foods yet. Search, scan a barcode, or create one with \"New food\".",
                    )
                }
            }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard selection?") },
            text = { Text("${plural(pending.size, "food", "foods")} you picked haven't been logged yet.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    viewModel.discardPending()
                    onBack()
                }) { Text("Discard") }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep") } },
        )
    }
}

/** Opens Google's barcode scanner (provided by Play services, no camera permission needed). */
private fun scanBarcode(context: Context, onScanned: (String) -> Unit, onError: (String) -> Unit) {
    val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E)
        .enableAutoZoom()
        .build()
    GmsBarcodeScanning.getClient(context, options).startScan()
        .addOnSuccessListener { barcode -> barcode.rawValue?.let(onScanned) }
        .addOnFailureListener { onError("Scanner unavailable: ${it.message}") }
}

/** A section title with a divider above it, so sections are easy to tell apart. */
private fun LazyListScope.sectionHeader(key: String, title: String) {
    item(key = "header-$key") {
        Column(Modifier.padding(top = 12.dp)) {
            HorizontalDivider()
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
            )
        }
    }
}

private fun LazyListScope.message(key: String, text: String) {
    item(key = key) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
    }
}

private fun unitLabel(unit: BaseUnit) = if (unit == BaseUnit.ML) "ml" else "g"

private fun LazyListScope.recipeSection(recipes: List<RecipeWithIngredients>, onClick: (Long) -> Unit, onNewRecipe: () -> Unit) {
    item(key = "header-recipes") {
        Column(Modifier.padding(top = 12.dp)) {
            HorizontalDivider()
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Recipes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                TextButton(onClick = onNewRecipe) { Text("New recipe") }
            }
        }
    }
    if (recipes.isEmpty()) message("empty-recipes", "Group foods you often eat together, e.g. your usual breakfast.")
    items(recipes, key = { "recipe-${it.recipe.id}" }) { r ->
        val n = r.nutrition
        ListItem(
            headlineContent = { Text(r.recipe.name) },
            supportingContent = {
                val portions = if (n.servings == 1.0) "portion" else "portions"
                val ingredients = if (r.ingredients.size == 1) "ingredient" else "ingredients"
                Text("${n.servings.formatAmount()} $portions · ${r.ingredients.size} $ingredients")
            },
            trailingContent = { Text("${n.perServing.kcal.formatWhole()} kcal / portion") },
            modifier = Modifier.clickable { onClick(r.recipe.id) },
        )
    }
}

private fun LazyListScope.savedFoodSection(title: String, foods: List<Food>, onClick: (Long) -> Unit, empty: String? = null) {
    sectionHeader(title, title)
    if (foods.isEmpty() && empty != null) message("empty-$title", empty)
    items(foods, key = { "$title-${it.id}" }) { food ->
        ListItem(
            headlineContent = { Text(food.name) },
            supportingContent = food.brand?.let { { Text(it) } },
            trailingContent = { Text("${food.kcalPer100.formatWhole()} kcal / 100 ${unitLabel(food.baseUnit)}") },
            modifier = Modifier.clickable { onClick(food.id) },
        )
    }
}

/** Results shown per source before "Show all", so both sources fit on screen. */
private const val COLLAPSED_RESULTS = 5

private fun LazyListScope.onlineSection(
    query: String,
    online: OnlineSearch?,
    expanded: Set<String>,
    onExpand: (String) -> Unit,
    onSearch: () -> Unit,
    sharedUsdaKey: Boolean,
    onChoose: (RemoteFood) -> Unit,
) {
    if (online != null && online.query == query) {
        remoteSection("Products · Open Food Facts", online.products, expanded, onExpand, onChoose)
        remoteSection("Generic foods · USDA", online.generic, expanded, onExpand, onChoose, sharedUsdaKey)
    } else if (query.length >= 2) {
        item(key = "online-button") {
            OutlinedButton(onClick = onSearch, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Icon(Icons.Filled.Search, contentDescription = null)
                Text("  Search online for \u201c$query\u201d")
            }
        }
    }
}

/** [result] is null while loading. */
private fun LazyListScope.remoteSection(
    title: String,
    result: Result<List<RemoteFood>>?,
    expanded: Set<String>,
    onExpand: (String) -> Unit,
    onChoose: (RemoteFood) -> Unit,
    sharedKey: Boolean = false,
) {
    sectionHeader(title, title)
    if (result == null) {
        item(key = "loading-$title") {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
        return
    }
    result.onFailure { message("error-$title", searchError(it, sharedKey)) }
    result.onSuccess { foods ->
        if (foods.isEmpty()) message("empty-$title", "No results.")
        val shown = if (title in expanded) foods else foods.take(COLLAPSED_RESULTS)
        items(shown, key = { "$title-${it.source}-${it.externalId}" }) { food ->
            ListItem(
                headlineContent = { Text(food.name) },
                supportingContent = {
                    val details = listOfNotNull(food.brand, "incomplete, needs editing".takeUnless { food.isComplete })
                    if (details.isNotEmpty()) Text(details.joinToString(" · "))
                },
                trailingContent = { food.kcal?.let { Text("${it.formatWhole()} kcal / 100 ${unitLabel(food.baseUnit)}") } },
                modifier = Modifier.clickable { onChoose(food) },
            )
        }
        if (shown.size < foods.size) {
            item(key = "more-$title") {
                TextButton(onClick = { onExpand(title) }, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text("Show all ${foods.size}")
                }
            }
        }
    }
}

private fun searchError(error: Throwable, sharedKey: Boolean = false): String = when {
    error.message?.contains("429") == true && sharedKey ->
        "The shared USDA key is busy. Try again later, or add your own free key in Settings."
    error.message?.contains("429") == true -> "Too many searches right now. Try again in a minute."
    error.message?.contains("403") == true -> "USDA rejected the API key. Check it in Settings."
    else -> "Couldn't search: check your connection."
}
