package com.factoryattendance.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.factoryattendance.data.local.dao.AttendanceDao
import com.factoryattendance.data.local.dao.ShiftDao
import com.factoryattendance.data.local.dao.WorkerDao
import com.factoryattendance.data.local.entity.AttendanceEntity
import com.factoryattendance.data.local.entity.ShiftEntity
import com.factoryattendance.data.local.entity.WorkerEntity

@Database(
    entities = [WorkerEntity::class, AttendanceEntity::class, ShiftEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workerDao(): WorkerDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun shiftDao(): ShiftDao
}
