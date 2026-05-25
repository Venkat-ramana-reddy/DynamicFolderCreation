package com.factoryattendance.domain.model

data class Shift(
    val id: String,
    val name: String,
    val startTime: String,      // "HH:MM"
    val endTime: String,
    val lateThresholdMinutes: Int = 15,
    val enabled: Boolean = true,
    val alwaysOn: Boolean = false   // General shift
)
