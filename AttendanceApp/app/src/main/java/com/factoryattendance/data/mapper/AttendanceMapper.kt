package com.factoryattendance.data.mapper

import com.factoryattendance.data.local.entity.AttendanceEntity
import com.factoryattendance.domain.model.AttendanceRecord
import com.factoryattendance.domain.model.AttendanceStatus
import com.factoryattendance.domain.model.CheckInMethod

fun AttendanceEntity.toDomain() = AttendanceRecord(
    id = id,
    workerId = workerId,
    date = date,
    status = runCatching { AttendanceStatus.valueOf(status) }.getOrDefault(AttendanceStatus.UNMARKED),
    timeIn = timeIn,
    timeOut = timeOut,
    checkInMethod = runCatching { CheckInMethod.valueOf(checkInMethod) }.getOrDefault(CheckInMethod.MANUAL),
    isDirty = isDirty,
    isSynced = isSynced,
    updatedAt = updatedAt
)

fun AttendanceRecord.toEntity() = AttendanceEntity(
    id = id,
    workerId = workerId,
    date = date,
    status = status.name,
    timeIn = timeIn,
    timeOut = timeOut,
    checkInMethod = checkInMethod.name,
    updatedAt = updatedAt,
    isDirty = isDirty,
    isSynced = isSynced
)
