package com.lorenzo.caltracker.ui.addfood

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lorenzo.caltracker.data.db.entity.Food
import com.lorenzo.caltracker.data.remote.RemoteFood
import com.lorenzo.caltracker.data.repository.BarcodeResult
import com.lorenzo.caltracker.data.repository.FoodRepository
import com.lorenzo.caltracker.data.repository.OnlineFoodRepository
import com.lorenzo.caltracker.ui.appViewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AddFoodUiState(
    val query: String = "",
    val recent: List<Food> = emptyList(),
    val all: List<Food> = emptyList(),
    val results: List<Food> = emptyList(),
)

/** An online search for [query]. Each source's result is null while it is still loading. */
data class OnlineSearch(
    val query: String,
    val products: Result<List<RemoteFood>>? = null,
    val generic: Result<List<RemoteFood>>? = null,
)

/** What to do after a food was chosen or scanned. */
interface FoodChoiceHandler {
    fun onFoodReady(foodId: Long)

    fun onNeedsEditing(draft: RemoteFood)
}

@OptIn(ExperimentalCoroutinesApi::class)
class AddFoodViewModel(
    foodRepository: FoodRepository,
    private val onlineFoods: OnlineFoodRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")

    private val results = query.flatMapLatest { q ->
        if (q.isBlank()) flowOf(emptyList()) else foodRepository.search(q)
    }

    val uiState: StateFlow<AddFoodUiState> = combine(
        query.asStateFlow(),
        foodRepository.observeRecent(),
        foodRepository.observeAll(),
        results,
    ) { q, recent, all, found ->
        AddFoodUiState(query = q, recent = recent, all = all, results = found)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddFoodUiState())

    var online by mutableStateOf<OnlineSearch?>(null)
        private set

    /** True while a scanned barcode is looked up or an online food is saved. */
    var busy by mutableStateOf(false)
        private set

    /** One-off message to show in a snackbar. */
    var message by mutableStateOf<String?>(null)

    fun setQuery(value: String) {
        query.value = value
    }

    /**
     * Searches both online sources in parallel. Triggered explicitly (not on every keystroke)
     * because Open Food Facts allows only about 10 searches a minute.
     */
    fun searchOnline() {
        val q = query.value.trim()
        if (q.length < 2 || online?.query == q) return
        online = OnlineSearch(q)
        // Sources answer at different speeds (USDA is slower), so each result is shown as it arrives.
        viewModelScope.launch {
            val products = runCatching { onlineFoods.searchProducts(q) }
            online?.takeIf { it.query == q }?.let { online = it.copy(products = products) }
        }
        viewModelScope.launch {
            val generic = runCatching { onlineFoods.searchGeneric(q) }
            online?.takeIf { it.query == q }?.let { online = it.copy(generic = generic) }
        }
    }

    /** Saves an online result locally (complete ones) or sends it to the editor (incomplete ones). */
    fun choose(remote: RemoteFood, handler: FoodChoiceHandler) {
        if (!remote.isComplete) {
            handler.onNeedsEditing(remote)
            return
        }
        runBusy { handler.onFoodReady(onlineFoods.save(remote)) }
    }

    fun lookupBarcode(barcode: String, handler: FoodChoiceHandler) = runBusy {
        when (val result = onlineFoods.lookupBarcode(barcode)) {
            is BarcodeResult.Found -> handler.onFoodReady(result.foodId)
            is BarcodeResult.Incomplete -> {
                message = "${result.draft.name} is missing nutrition values. Please complete them from the label."
                handler.onNeedsEditing(result.draft)
            }
            is BarcodeResult.NotFound -> {
                message = "Barcode $barcode isn't in Open Food Facts yet. Enter it from the label."
                handler.onNeedsEditing(RemoteFood.blank(barcode))
            }
        }
    }

    private fun runBusy(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                message = "Couldn't reach Open Food Facts. Check your connection. (${e.message})"
            } finally {
                busy = false
            }
        }
    }

    companion object {
        val Factory = appViewModelFactory { AddFoodViewModel(it.foodRepository, it.onlineFoodRepository) }
    }
}
