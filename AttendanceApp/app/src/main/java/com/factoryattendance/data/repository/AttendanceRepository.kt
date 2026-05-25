package com.factoryattendance.data.repository

import com.factoryattendance.data.local.dao.AttendanceDao
import com.factoryattendance.data.local.dao.WorkerDao
import com.factoryattendance.data.local.entity.AttendanceEntity
import com.factoryattendance.data.mapper.toDomain
import com.factoryattendance.data.mapper.toEntity
import com.factoryattendance.data.remote.AttendanceDto
import com.factoryattendance.data.remote.SupabaseDataSource
import com.factoryattendance.domain.model.AttendanceRecord
import com.factoryattendance.domain.model.AttendanceStatus
import com.factoryattendance.domain.model.CheckInMethod
import com.factoryattendance.domain.model.DashboardStats
import com.factoryattendance.util.TimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceRepository @Inject constructor(
    private val dao: AttendanceDao,
    private val workerDao: WorkerDao,
    private val shiftRepo: ShiftRepository,
    private val remote: SupabaseDataSource
) {
    fun observeForDate(date: String): Flow<List<AttendanceRecord>> =
        dao.observeByDate(date).map { list -> list.map { it.toDomain() } }

    fun observeForMonth(monthPrefix: String): Flow<List<AttendanceRecord>> =
        dao.observeByMonth(monthPrefix).map { list -> list.map { it.toDomain() } }

    fun observeForWorkerRange(workerId: String, start: String, end: String): Flow<List<AttendanceRecord>> =
        dao.observeByWorkerRange(workerId, start, end).map { list -> list.map { it.toDomain() } }

    suspend fun upsert(record: AttendanceRecord): Result<Unit> = runCatching {
        dao.upsert(record.copy(isDirty = true, isSynced = false, updatedAt = System.currentTimeMillis()).toEntity())
        // best-effort immediate push
        pushDirty()
    }

    suspend fun getDashboardStats(date: String): DashboardStats {
        val records = dao.observeByDate(date).first()
        val workers = workerDao.observeAll().first()
        val shifts  = shiftRepo.getShiftsSnapshot()
        var present = 0; var absent = 0; var late = 0; var otCount = 0; var totalOT = 0
        records.forEach { entity ->
            val rec = entity.toDomain()
            val worker = workers.find { it.id == entity.workerId }
            val shift  = shifts.find { it.name == (worker?.shift ?: "General") }
            when (rec.status) {
                AttendanceStatus.PRESENT -> present++
                AttendanceStatus.LATE    -> { present++; late++ }
                AttendanceStatus.ABSENT  -> absent++
                else -> {}
            }
            if (shift != null) {
                val h = TimeUtils.calculateHours(rec.timeIn, rec.timeOut, shift)
                if ((h.overtimeMinutes ?: 0) > 0) { otCount++; totalOT += h.overtimeMinutes!! }
            }
        }
        val rate = if (workers.isNotEmpty()) (present.toFloat() / workers.size) else 0f
        return DashboardStats(present, absent, late, otCount, totalOT, workers.size, rate)
    }

    suspend fun syncFromRemote(date: String): Result<Unit> = runCatching {
        val dtos = remote.fetchAttendanceForDate(date)
        val entities = dtos.map { dto ->
            AttendanceEntity(
                id = dto.id, workerId = dto.worker_id, date = dto.date,
                status = dto.status, timeIn = dto.time_in ?: "",
                timeOut = dto.time_out ?: "", checkInMethod = dto.check_in_method,
                updatedAt = System.currentTimeMillis(), isDirty = false, isSynced = true
            )
        }
        dao.upsertAll(entities)
    }

    suspend fun pushDirty(): Result<Unit> = runCatching {
        val dirty = dao.getDirty()
        if (dirty.isEmpty()) return@runCatching
        val dtos = dirty.map { e ->
            AttendanceDto(
                id = e.id, worker_id = e.workerId, date = e.date, status = e.status,
                time_in = e.timeIn.ifBlank { null }, time_out = e.timeOut.ifBlank { null },
                check_in_method = e.checkInMethod
            )
        }
        remote.upsertAttendanceBatch(dtos)
        dirty.forEach { dao.markSynced(it.id) }
    }

    suspend fun getOrCreate(workerId: String, date: String, shiftName: String): AttendanceRecord {
        val existing = dao.getForWorkerDate(workerId, date)
        return existing?.toDomain() ?: AttendanceRecord(
            id = UUID.randomUUID().toString(),
            workerId = workerId,
            date = date,
            status = AttendanceStatus.UNMARKED
        )
    }
}
