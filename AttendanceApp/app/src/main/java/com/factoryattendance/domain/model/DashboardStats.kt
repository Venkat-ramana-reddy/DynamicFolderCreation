package com.factoryattendance.domain.model

data class DashboardStats(
    val presentCount: Int = 0,
    val absentCount: Int = 0,
    val lateCount: Int = 0,
    val overtimeCount: Int = 0,
    val totalOvertimeMinutes: Int = 0,
    val totalWorkers: Int = 0,
    val attendanceRate: Float = 0f
)
