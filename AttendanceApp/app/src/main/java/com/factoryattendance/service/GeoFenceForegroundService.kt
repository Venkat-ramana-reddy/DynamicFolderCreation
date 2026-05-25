package com.factoryattendance.service

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.*
import androidx.core.app.NotificationCompat
import com.factoryattendance.R
import com.factoryattendance.data.preferences.AppPreferences
import com.factoryattendance.data.repository.AttendanceRepository
import com.factoryattendance.data.repository.ShiftRepository
import com.factoryattendance.data.repository.WorkerRepository
import com.factoryattendance.domain.model.AttendanceStatus
import com.factoryattendance.domain.model.CheckInMethod
import com.factoryattendance.util.GeoUtils
import com.factoryattendance.util.TimeUtils
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class LocationState(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val distanceMeters: Double = -1.0,
    val isInsideZone: Boolean = false,
    val isTracking: Boolean = false
)

@AndroidEntryPoint
class GeoFenceForegroundService : Service() {

    @Inject lateinit var fusedLocation: FusedLocationProviderClient
    @Inject lateinit var prefs: AppPreferences
    @Inject lateinit var workerRepo: WorkerRepository
    @Inject lateinit var attendanceRepo: AttendanceRepository
    @Inject lateinit var shiftRepo: ShiftRepository

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val binder = LocalBinder()

    private val _locationState = MutableStateFlow(LocationState())
    val locationState: StateFlow<LocationState> = _locationState

    private val _checkInLog = MutableStateFlow<List<String>>(emptyList())
    val checkInLog: StateFlow<List<String>> = _checkInLog

    private var wasInside = mutableMapOf<String, Boolean>()

    inner class LocalBinder : Binder() {
        fun getService(): GeoFenceForegroundService = this@GeoFenceForegroundService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, buildNotification("Geo-fence active"),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIF_ID, buildNotification("Geo-fence active"))
        }
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 15_000L)
            .setMinUpdateDistanceMeters(10f)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc -> handleLocation(loc.latitude, loc.longitude) }
            }
        }
        try {
            fusedLocation.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (_: SecurityException) { /* permission not granted */ }
    }

    private fun handleLocation(lat: Double, lng: Double) {
        scope.launch {
            val factoryLat = prefs.factoryLat.first()
            val factoryLng = prefs.factoryLng.first()
            val radius     = prefs.fenceRadius.first()
            val isSet      = factoryLat != 0.0 || factoryLng != 0.0

            if (!isSet) {
                _locationState.value = LocationState(lat, lng, -1.0, false, true)
                return@launch
            }

            val dist   = GeoUtils.haversineDistance(lat, lng, factoryLat, factoryLng)
            val inside = dist <= radius
            _locationState.value = LocationState(lat, lng, dist, inside, true)

            val workers = workerRepo.observeWorkers().first()
            val today   = TimeUtils.todayIso()
            val shifts  = shiftRepo.getShiftsSnapshot()

            workers.forEach { worker ->
                val prevInside = wasInside[worker.id] ?: false
                if (inside && !prevInside) {
                    // Auto check-in
                    val record = attendanceRepo.getOrCreate(worker.id, today, worker.shift)
                    if (record.timeIn.isBlank()) {
                        val shift = shifts.find { it.name == worker.shift } ?: shifts.firstOrNull()
                        val now = TimeUtils.nowHHMM()
                        val status = if (shift != null && TimeUtils.isLate(now, shift))
                            AttendanceStatus.LATE else AttendanceStatus.PRESENT
                        attendanceRepo.upsert(record.copy(
                            timeIn = now, status = status, checkInMethod = CheckInMethod.GEO_FENCE
                        ))
                        addLog("→ ${worker.name} auto check-in at $now")
                    }
                    wasInside[worker.id] = true
                } else if (!inside && prevInside) {
                    // Auto check-out
                    val record = attendanceRepo.getOrCreate(worker.id, today, worker.shift)
                    if (record.timeIn.isNotBlank() && record.timeOut.isBlank()) {
                        val now = TimeUtils.nowHHMM()
                        attendanceRepo.upsert(record.copy(timeOut = now, checkInMethod = CheckInMethod.GEO_FENCE))
                        addLog("← ${worker.name} auto check-out at $now")
                    }
                    wasInside[worker.id] = false
                }
            }

            val notifText = if (inside) "Inside factory zone (≈${dist.toInt()}m)" else "Outside zone (${dist.toInt()}m away)"
            updateNotification(notifText)
        }
    }

    private fun addLog(entry: String) {
        _checkInLog.value = (listOf(entry) + _checkInLog.value).take(50)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        fusedLocation.removeLocationUpdates { /* callback */ }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.geo_fence_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.geo_fence_notification_title))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification(text))
    }

    companion object {
        const val CHANNEL_ID = "geo_fence_channel"
        const val NOTIF_ID   = 1001
    }
}
