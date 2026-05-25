package com.factoryattendance.data.local.dao

import androidx.room.*
import com.factoryattendance.data.local.entity.ShiftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shifts ORDER BY name ASC")
    fun observeAll(): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shifts WHERE enabled = 1 ORDER BY name ASC")
    fun observeEnabled(): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shifts WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): ShiftEntity?

    @Upsert
    suspend fun upsertAll(shifts: List<ShiftEntity>)

    @Upsert
    suspend fun upsert(shift: ShiftEntity)
}
