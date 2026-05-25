package com.factoryattendance.data.mapper

import com.factoryattendance.data.local.entity.ShiftEntity
import com.factoryattendance.domain.model.Shift

fun ShiftEntity.toDomain() = Shift(id, name, startTime, endTime, lateThresholdMinutes, enabled, alwaysOn)
fun Shift.toEntity() = ShiftEntity(id, name, startTime, endTime, lateThresholdMinutes, enabled, alwaysOn)
