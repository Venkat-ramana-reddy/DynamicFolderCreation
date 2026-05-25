package com.factoryattendance.data.local.dao

import androidx.room.*
import com.factoryattendance.data.local.entity.AttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records WHERE date = :date ORDER BY workerId")
    fun observeByDate(date: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance_records WHERE workerId = :workerId AND date BETWEEN :start AND :end")
    fun observeByWorkerRange(workerId: String, start: String, end: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance_records WHERE date LIKE :monthPrefix || '%'")
    fun observeByMonth(monthPrefix: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance_records WHERE workerId = :workerId AND date = :date LIMIT 1")
    suspend fun getForWorkerDate(workerId: String, date: String): AttendanceEntity?

    @Query("SELECT * FROM attendance_records WHERE isDirty = 1")
    suspend fun getDirty(): List<AttendanceEntity>

    @Upsert
    suspend fun upsert(record: AttendanceEntity)

    @Upsert
    suspend fun upsertAll(records: List<AttendanceEntity>)

    @Query("UPDATE attendance_records SET isDirty = 0, isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}
