package com.lorenzo.caltracker.data.repository

import com.lorenzo.caltracker.data.db.dao.FoodDao
import com.lorenzo.caltracker.data.db.entity.Food
import kotlinx.coroutines.flow.Flow

class FoodRepository(private val dao: FoodDao) {
    fun observeAll(): Flow<List<Food>> = dao.observeAll()

    fun observeRecent(limit: Int = 20): Flow<List<Food>> = dao.observeRecent(limit)

    fun search(query: String): Flow<List<Food>> = dao.search(query.trim())

    suspend fun get(id: Long): Food? = dao.getById(id)

    suspend fun add(food: Food): Long = dao.insert(food)
}
