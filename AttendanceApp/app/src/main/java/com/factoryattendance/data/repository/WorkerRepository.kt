package com.factoryattendance.data.repository

import com.factoryattendance.data.local.dao.WorkerDao
import com.factoryattendance.data.mapper.toDomain
import com.factoryattendance.data.mapper.toEntity
import com.factoryattendance.data.remote.SupabaseDataSource
import com.factoryattendance.data.remote.WorkerDto
import com.factoryattendance.domain.model.Worker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkerRepository @Inject constructor(
    private val dao: WorkerDao,
    private val remote: SupabaseDataSource
) {
    fun observeWorkers(): Flow<List<Worker>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun addWorker(name: String, dept: String, shift: String): Result<Unit> = runCatching {
        val worker = Worker(id = UUID.randomUUID().toString(), name = name, dept = dept, shift = shift)
        dao.upsert(worker.toEntity(isSynced = false))
    }

    suspend fun deleteWorker(id: String): Result<Unit> = runCatching {
        dao.deleteById(id)
    }

    suspend fun updateWorkerShift(workerId: String, shiftName: String): Result<Unit> = runCatching {
        val existing = dao.getById(workerId) ?: return@runCatching
        dao.upsert(existing.copy(shift = shiftName, isSynced = false))
    }

    suspend fun syncFromRemote(): Result<Unit> = runCatching {
        val remoteWorkers = remote.fetchWorkers()
        val entities = remoteWorkers.map { dto ->
            com.factoryattendance.data.local.entity.WorkerEntity(
                id = dto.id, name = dto.name, dept = dto.dept, shift = dto.shift,
                createdAt = System.currentTimeMillis(), isSynced = true
            )
        }
        dao.upsertAll(entities)
    }

    suspend fun pushUnsynced(): Result<Unit> = runCatching {
        val unsynced = dao.getUnsynced()
        unsynced.forEach { entity ->
            remote.upsertWorker(WorkerDto(entity.id, entity.name, entity.dept, entity.shift))
            dao.upsert(entity.copy(isSynced = true))
        }
    }
}
