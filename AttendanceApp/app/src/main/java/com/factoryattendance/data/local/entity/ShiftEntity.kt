package com.factoryattendance.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shifts")
data class ShiftEntity(
    @PrimaryKey val id: String,
    val name: String,
    val startTime: String,
    val endTime: String,
    val lateThresholdMinutes: Int,
    val enabled: Boolean,
    val alwaysOn: Boolean
)
