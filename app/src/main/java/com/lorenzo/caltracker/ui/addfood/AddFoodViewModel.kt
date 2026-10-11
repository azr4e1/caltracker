package com.lorenzo.caltracker.ui.addfood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lorenzo.caltracker.data.db.entity.Food
import com.lorenzo.caltracker.data.repository.FoodRepository
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

data class AddFoodUiState(
    val query: String = "",
    val recent: List<Food> = emptyList(),
    val all: List<Food> = emptyList(),
    val results: List<Food> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class AddFoodViewModel(foodRepository: FoodRepository) : ViewModel() {
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

    fun setQuery(value: String) {
        query.value = value
    }

    companion object {
        val Factory = appViewModelFactory { AddFoodViewModel(it.foodRepository) }
    }
}
