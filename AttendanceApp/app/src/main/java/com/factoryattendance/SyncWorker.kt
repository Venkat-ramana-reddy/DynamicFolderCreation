package com.factoryattendance

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.factoryattendance.data.repository.AttendanceRepository
import com.factoryattendance.data.repository.WorkerRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val workerRepository: WorkerRepository,
    private val attendanceRepository: AttendanceRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        workerRepository.pushUnsynced()
        attendanceRepository.pushDirty()
        Result.success()
    } catch (e: Exception) {
        if (runAttemptCount < 3) Result.retry() else Result.failure()
    }

    companion object {
        const val WORK_NAME = "attendance_sync"
    }
}
