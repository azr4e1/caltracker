package com.lorenzo.caltracker.data.repository

import com.lorenzo.caltracker.data.db.entity.LogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** An entry waiting to be logged. [key] identifies it in the list (entries aren't in the database yet). */
data class PendingItem(val key: Int, val entry: LogEntry)

/**
 * Foods picked in the Add food flow that haven't been logged yet. They are reviewed together
 * and then logged in one go. Kept in memory only.
 */
class PendingLog {
    private val _items = MutableStateFlow(emptyList<PendingItem>())
    val items: StateFlow<List<PendingItem>> = _items.asStateFlow()
    private var nextKey = 0

    fun add(entry: LogEntry) = _items.update { it + PendingItem(nextKey++, entry) }

    fun update(key: Int, entry: LogEntry) = _items.update { list -> list.map { if (it.key == key) it.copy(entry = entry) else it } }

    fun remove(key: Int) = _items.update { list -> list.filterNot { it.key == key } }

    fun restore(item: PendingItem) = _items.update { (it + item).sortedBy { p -> p.key } }

    fun clear() {
        _items.value = emptyList()
    }
}
