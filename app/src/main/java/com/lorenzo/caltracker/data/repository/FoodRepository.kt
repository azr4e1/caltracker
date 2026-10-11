package com.lorenzo.caltracker.data.repository

import com.lorenzo.caltracker.data.db.dao.FoodDao
import com.lorenzo.caltracker.data.db.entity.Food
import com.lorenzo.caltracker.data.remote.RemoteFood
import kotlinx.coroutines.flow.Flow

class FoodRepository(private val dao: FoodDao) {
    fun observeAll(): Flow<List<Food>> = dao.observeAll()

    fun observeRecent(limit: Int = 20): Flow<List<Food>> = dao.observeRecent(limit)

    fun observe(id: Long): Flow<Food?> = dao.observeById(id)

    fun search(query: String): Flow<List<Food>> = dao.search(query.trim())

    suspend fun get(id: Long): Food? = dao.getById(id)

    suspend fun getByBarcode(barcode: String): Food? = dao.getByBarcode(barcode)

    /**
     * Saves a food from an online database so it works offline and shows up in recents.
     * If it was saved before (same barcode or same source id), returns the existing one instead.
     */
    suspend fun saveRemote(remote: RemoteFood): Long {
        remote.barcode?.let { dao.getByBarcode(it) }?.let { return it.id }
        dao.getByExternalId(remote.source, remote.externalId)?.let { return it.id }
        return dao.insert(remote.toFood())
    }

    suspend fun getAll(): List<Food> = dao.getAll()

    suspend fun add(food: Food): Long = dao.insert(food)

    suspend fun update(food: Food) = dao.update(food.copy(updatedAt = System.currentTimeMillis()))

    /** Deletes the food. Logged entries keep their values; fails if a recipe uses it. */
    suspend fun delete(food: Food) = dao.delete(food)
}
