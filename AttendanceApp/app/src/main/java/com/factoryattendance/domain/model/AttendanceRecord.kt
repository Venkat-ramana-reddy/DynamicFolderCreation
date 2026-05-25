package com.factoryattendance.domain.model

data class AttendanceRecord(
    val id: String,
    val workerId: String,
    val date: String,          // ISO-8601 "YYYY-MM-DD"
    val status: AttendanceStatus = AttendanceStatus.UNMARKED,
    val timeIn: String = "",   // "HH:MM"
    val timeOut: String = "",
    val checkInMethod: CheckInMethod = CheckInMethod.MANUAL,
    val isDirty: Boolean = false,
    val isSynced: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
