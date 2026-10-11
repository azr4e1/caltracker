package com.lorenzo.caltracker.ui.addfood

import android.content.Context
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.lorenzo.caltracker.data.db.entity.BaseUnit
import com.lorenzo.caltracker.data.db.entity.Food
import com.lorenzo.caltracker.data.remote.RemoteFood
import com.lorenzo.caltracker.ui.formatWhole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodScreen(
    onBack: () -> Unit,
    onFoodSelected: (Long) -> Unit,
    onEditDraft: (RemoteFood) -> Unit,
    onQuickAdd: () -> Unit,
    onNewFood: () -> Unit,
    viewModel: AddFoodViewModel = viewModel(factory = AddFoodViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
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
                title = { Text("Add food") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
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
                FilledTonalButton(onClick = onQuickAdd, modifier = Modifier.weight(1f)) { Text("Quick add") }
                FilledTonalButton(onClick = onNewFood, modifier = Modifier.weight(1f)) { Text("New food") }
            }
            LazyColumn(Modifier.fillMaxSize()) {
                if (state.query.isNotBlank()) {
                    savedFoodSection("Your foods", state.results, onFoodSelected, empty = "No saved food matches.")
                    onlineSection(state.query.trim(), viewModel.online, expandedSections, { expandedSections = expandedSections + it }, ::search) { viewModel.choose(it, handler) }
                } else {
                    if (state.recent.isNotEmpty()) savedFoodSection("Recent", state.recent, onFoodSelected)
                    savedFoodSection(
                        "All foods", state.all, onFoodSelected,
                        empty = "No saved foods yet. Search, scan a barcode, or create one with \"New food\".",
                    )
                }
            }
        }
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

private fun LazyListScope.sectionHeader(key: String, title: String) {
    item(key = "header-$key") {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        )
    }
}

private fun LazyListScope.message(key: String, text: String) {
    item(key = key) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
    }
}

private fun unitLabel(unit: BaseUnit) = if (unit == BaseUnit.ML) "ml" else "g"

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
    onChoose: (RemoteFood) -> Unit,
) {
    if (online != null && online.query == query) {
        remoteSection("Products · Open Food Facts", online.products, expanded, onExpand, onChoose)
        remoteSection("Generic foods · USDA", online.generic, expanded, onExpand, onChoose)
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
) {
    sectionHeader(title, title)
    if (result == null) {
        item(key = "loading-$title") {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
        return
    }
    result.onFailure { message("error-$title", searchError(it)) }
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

private fun searchError(error: Throwable): String = when {
    error.message?.contains("429") == true -> "Too many searches right now. Try again in a minute."
    else -> "Couldn't search: check your connection."
}
