package com.factoryattendance.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workers")
data class WorkerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val dept: String,
    val shift: String,
    val createdAt: Long,
    val isSynced: Boolean = false
)
