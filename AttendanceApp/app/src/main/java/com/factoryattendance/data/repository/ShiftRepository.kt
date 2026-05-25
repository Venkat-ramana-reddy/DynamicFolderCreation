package com.factoryattendance.data.repository

import com.factoryattendance.data.local.dao.ShiftDao
import com.factoryattendance.data.local.entity.ShiftEntity
import com.factoryattendance.data.mapper.toDomain
import com.factoryattendance.data.mapper.toEntity
import com.factoryattendance.data.remote.ShiftDto
import com.factoryattendance.data.remote.SupabaseDataSource
import com.factoryattendance.domain.model.Shift
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShiftRepository @Inject constructor(
    private val dao: ShiftDao,
    private val remote: SupabaseDataSource
) {
    fun observeAll(): Flow<List<Shift>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    fun observeEnabled(): Flow<List<Shift>> = dao.observeEnabled().map { list -> list.map { it.toDomain() } }

    suspend fun getShiftsSnapshot(): List<Shift> = dao.observeAll().first().map { it.toDomain() }

    suspend fun getByName(name: String): Shift? = dao.getByName(name)?.toDomain()

    suspend fun save(shift: Shift): Result<Unit> = runCatching { dao.upsert(shift.toEntity()) }

    suspend fun seedDefaults() {
        val existing = dao.observeAll().first()
        if (existing.isNotEmpty()) return
        val defaults = listOf(
            ShiftEntity(UUID.randomUUID().toString(), "General", "08:30", "17:30", 15, true,  true),
            ShiftEntity(UUID.randomUUID().toString(), "A",       "08:30", "17:30", 15, true,  false),
            ShiftEntity(UUID.randomUUID().toString(), "B",       "14:00", "23:00", 15, false, false),
            ShiftEntity(UUID.randomUUID().toString(), "C",       "22:00", "07:00", 15, false, false),
            ShiftEntity(UUID.randomUUID().toString(), "D",       "05:00", "14:00", 15, false, false)
        )
        dao.upsertAll(defaults)
    }

    suspend fun seedDefaultWorkers(workerDao: com.factoryattendance.data.local.dao.WorkerDao) {
        val existing = workerDao.observeAll().first()
        if (existing.isNotEmpty()) return
        val defaults = listOf(
            com.factoryattendance.data.local.entity.WorkerEntity(UUID.randomUUID().toString(), "Arjun Kumar",  "Line A",      "A",       System.currentTimeMillis()),
            com.factoryattendance.data.local.entity.WorkerEntity(UUID.randomUUID().toString(), "Priya Nair",   "Line A",      "A",       System.currentTimeMillis()),
            com.factoryattendance.data.local.entity.WorkerEntity(UUID.randomUUID().toString(), "Ravi Selvam",  "Line B",      "B",       System.currentTimeMillis()),
            com.factoryattendance.data.local.entity.WorkerEntity(UUID.randomUUID().toString(), "Meena Devi",   "Line B",      "General", System.currentTimeMillis()),
            com.factoryattendance.data.local.entity.WorkerEntity(UUID.randomUUID().toString(), "Suresh Babu",  "Quality",     "General", System.currentTimeMillis()),
            com.factoryattendance.data.local.entity.WorkerEntity(UUID.randomUUID().toString(), "Kavitha R",    "Quality",     "General", System.currentTimeMillis()),
            com.factoryattendance.data.local.entity.WorkerEntity(UUID.randomUUID().toString(), "Dinesh Kumar", "Maintenance", "General", System.currentTimeMillis()),
            com.factoryattendance.data.local.entity.WorkerEntity(UUID.randomUUID().toString(), "Lakshmi S",    "Packaging",   "General", System.currentTimeMillis())
        )
        workerDao.upsertAll(defaults)
    }
}
