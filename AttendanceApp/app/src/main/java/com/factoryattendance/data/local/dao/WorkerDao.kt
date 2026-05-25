package com.factoryattendance.data.local.dao

import androidx.room.*
import com.factoryattendance.data.local.entity.WorkerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerDao {
    @Query("SELECT * FROM workers ORDER BY name ASC")
    fun observeAll(): Flow<List<WorkerEntity>>

    @Query("SELECT * FROM workers WHERE id = :id")
    suspend fun getById(id: String): WorkerEntity?

    @Upsert
    suspend fun upsertAll(workers: List<WorkerEntity>)

    @Upsert
    suspend fun upsert(worker: WorkerEntity)

    @Query("DELETE FROM workers WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM workers WHERE isSynced = 0")
    suspend fun getUnsynced(): List<WorkerEntity>
}
