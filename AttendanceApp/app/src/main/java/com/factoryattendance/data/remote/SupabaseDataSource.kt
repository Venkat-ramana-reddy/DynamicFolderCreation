package com.factoryattendance.data.remote

import com.factoryattendance.data.local.entity.AttendanceEntity
import com.factoryattendance.data.local.entity.ShiftEntity
import com.factoryattendance.data.local.entity.WorkerEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class WorkerDto(
    val id: String,
    val name: String,
    val dept: String,
    val shift: String,
    val created_at: String? = null
)

@Serializable
data class AttendanceDto(
    val id: String,
    val worker_id: String,
    val date: String,
    val status: String,
    val time_in: String? = null,
    val time_out: String? = null,
    val check_in_method: String,
    val updated_at: String? = null
)

@Serializable
data class ShiftDto(
    val id: String,
    val name: String,
    val start_time: String,
    val end_time: String,
    val late_threshold_minutes: Int,
    val enabled: Boolean,
    val always_on: Boolean = false
)

@Singleton
class SupabaseDataSource @Inject constructor(
    private val client: SupabaseClient
) {
    suspend fun fetchWorkers(): List<WorkerDto> =
        client.postgrest["workers"].select().decodeList()

    suspend fun upsertWorker(dto: WorkerDto) {
        client.postgrest["workers"].upsert(dto)
    }

    suspend fun deleteWorker(id: String) {
        client.postgrest["workers"].delete { filter { eq("id", id) } }
    }

    suspend fun fetchAttendanceForDate(date: String): List<AttendanceDto> =
        client.postgrest["attendance_records"].select {
            filter { eq("date", date) }
        }.decodeList()

    suspend fun upsertAttendance(dto: AttendanceDto) {
        client.postgrest["attendance_records"].upsert(dto) {
            onConflict = "worker_id,date"
        }
    }

    suspend fun upsertAttendanceBatch(dtos: List<AttendanceDto>) {
        if (dtos.isEmpty()) return
        client.postgrest["attendance_records"].upsert(dtos) {
            onConflict = "worker_id,date"
        }
    }

    suspend fun fetchShifts(): List<ShiftDto> =
        client.postgrest["shifts"].select().decodeList()

    suspend fun upsertShift(dto: ShiftDto) {
        client.postgrest["shifts"].upsert(dto)
    }
}
