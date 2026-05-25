package com.factoryattendance.data.mapper

import com.factoryattendance.data.local.entity.WorkerEntity
import com.factoryattendance.domain.model.Worker

fun WorkerEntity.toDomain() = Worker(id, name, dept, shift, createdAt)
fun Worker.toEntity(isSynced: Boolean = false) = WorkerEntity(id, name, dept, shift, createdAt, isSynced)
