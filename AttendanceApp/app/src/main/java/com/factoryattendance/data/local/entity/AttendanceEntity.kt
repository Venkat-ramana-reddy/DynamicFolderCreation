package com.factoryattendance.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attendance_records",
    foreignKeys = [ForeignKey(
        entity = WorkerEntity::class,
        parentColumns = ["id"],
        childColumns = ["workerId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("workerId"), Index("date")]
)
data class AttendanceEntity(
    @PrimaryKey val id: String,
    val workerId: String,
    val date: String,
    val status: String,
    val timeIn: String,
    val timeOut: String,
    val checkInMethod: String,
    val updatedAt: Long,
    val isDirty: Boolean = false,
    val isSynced: Boolean = false
)
