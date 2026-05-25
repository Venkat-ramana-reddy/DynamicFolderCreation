package com.factoryattendance.ui.screen.report

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.factoryattendance.data.preferences.AppPreferences
import com.factoryattendance.data.repository.AttendanceRepository
import com.factoryattendance.data.repository.ShiftRepository
import com.factoryattendance.data.repository.WorkerRepository
import com.factoryattendance.domain.model.AttendanceRecord
import com.factoryattendance.domain.model.AttendanceStatus
import com.factoryattendance.domain.model.Worker
import com.factoryattendance.util.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import javax.inject.Inject

data class CalendarDay(
    val dayOfMonth: Int,
    val dateKey: String,
    val status: AttendanceStatus,
    val isToday: Boolean,
    val isFuture: Boolean,
    val hasOvertime: Boolean
)

data class WorkerMonthStats(
    val worker: Worker,
    val present: Int,
    val absent: Int,
    val late: Int,
    val off: Int,
    val otMinutes: Int
)

data class ReportUiState(
    val year: Int = Calendar.getInstance().get(Calendar.YEAR),
    val month: Int = Calendar.getInstance().get(Calendar.MONTH),
    val workerStats: List<WorkerMonthStats> = emptyList(),
    val workers: List<Worker> = emptyList(),
    val selectedWorkerId: String? = null,
    val calendarDays: List<CalendarDay?> = emptyList(),  // nulls for empty cells
    val firstDayOfWeek: Int = 0,
    val recordedDays: Int = 0,
    val todayHours: List<Pair<Worker, AttendanceRecord>> = emptyList(),
    val totalOtMinutes: Int = 0,
    val avgAttendance: Float = 0f,
    val perfectCount: Int = 0
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val workerRepo: WorkerRepository,
    private val attendanceRepo: AttendanceRepository,
    private val shiftRepo: ShiftRepository,
    private val prefs: AppPreferences
) : ViewModel() {

    private val today = TimeUtils.todayIso()
    private val _year  = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    private val _month = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))
    private val _selectedWorkerId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ReportUiState> = combine(
        _year, _month, _selectedWorkerId,
        workerRepo.observeWorkers(),
        prefs.currentWorkerId
    ) { year, month, selWorker, workers, currentWorker ->
        val monthPrefix = "$year-${(month + 1).toString().padStart(2, '0')}"
        val allDays = getDaysInMonth(year, month)
        val firstDay = Calendar.getInstance().apply { set(year, month, 1) }.get(Calendar.DAY_OF_WEEK) - 1

        val shifts = shiftRepo.getShiftsSnapshot()

        // Collect records for the entire month
        val monthRecords = attendanceRepo.observeForMonth(monthPrefix).first()
        val recordsByDate = monthRecords.groupBy { it.date }

        val recordedDays = allDays.count { day ->
            !day.isFuture && recordsByDate.containsKey(day.dateKey)
        }

        // Worker stats
        val workerStats = workers.map { worker ->
            var p = 0; var a = 0; var l = 0; var off = 0; var otMins = 0
            allDays.filter { !it.isFuture }.forEach { day ->
                val rec = recordsByDate[day.dateKey]?.find { it.workerId == worker.id }
                when (rec?.status) {
                    AttendanceStatus.PRESENT  -> p++
                    AttendanceStatus.ABSENT   -> a++
                    AttendanceStatus.LATE     -> l++
                    AttendanceStatus.OFF      -> off++
                    else -> {}
                }
                if (rec != null) {
                    val shift = shifts.find { it.name == worker.shift } ?: shifts.firstOrNull()
                    if (shift != null) {
                        val h = TimeUtils.calculateHours(rec.timeIn, rec.timeOut, shift)
                        otMins += h.overtimeMinutes ?: 0
                    }
                }
            }
            WorkerMonthStats(worker, p, a, l, off, otMins)
        }

        val totalOT = workerStats.sumOf { it.otMinutes }
        val avgAtt = if (workerStats.isEmpty()) 0f else
            workerStats.map { (it.present + it.late).toFloat() / maxOf(1, recordedDays) }.average().toFloat()
        val perfect = workerStats.count { it.absent == 0 && recordedDays > 0 }

        // Calendar for selected worker
        val selectedId = selWorker ?: (currentWorker ?: workers.firstOrNull()?.id)
        val calDays: MutableList<CalendarDay?> = MutableList(firstDay) { null }
        allDays.forEach { day ->
            val rec = recordsByDate[day.dateKey]?.find { it.workerId == selectedId }
            val shift = shifts.find { it.name == workers.find { w -> w.id == selectedId }?.shift }
            val hours = if (rec != null && shift != null)
                TimeUtils.calculateHours(rec.timeIn, rec.timeOut, shift) else null
            calDays.add(CalendarDay(
                dayOfMonth = day.dayOfMonth,
                dateKey = day.dateKey,
                status = rec?.status ?: AttendanceStatus.UNMARKED,
                isToday = day.dateKey == today,
                isFuture = day.isFuture,
                hasOvertime = hours?.hasOvertime ?: false
            ))
        }

        // Today's hours
        val todayRecords = attendanceRepo.observeForDate(today).first()
        val todayHours = workers.mapNotNull { w ->
            val rec = todayRecords.find { it.workerId == w.id } ?: return@mapNotNull null
            Pair(w, rec)
        }

        ReportUiState(
            year = year, month = month, workerStats = workerStats,
            workers = workers, selectedWorkerId = selectedId,
            calendarDays = calDays, firstDayOfWeek = firstDay,
            recordedDays = recordedDays, todayHours = todayHours,
            totalOtMinutes = totalOT, avgAttendance = avgAtt, perfectCount = perfect
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportUiState())

    fun changeMonth(delta: Int) {
        var m = _month.value + delta
        var y = _year.value
        if (m > 11) { m = 0; y++ }
        if (m < 0)  { m = 11; y-- }
        _month.value = m; _year.value = y
    }

    fun selectWorker(id: String) { _selectedWorkerId.value = id }

    fun exportCsv(context: Context) {
        viewModelScope.launch {
            val state = uiState.value
            val monthNames = listOf("January","February","March","April","May","June",
                "July","August","September","October","November","December")
            val sb = StringBuilder("Worker,Dept,Present,Absent,Late,Off,OT Hours\n")
            state.workerStats.forEach { s ->
                sb.append("\"${s.worker.name}\",\"${s.worker.dept}\"," +
                    "${s.present},${s.absent},${s.late},${s.off}," +
                    "\"${TimeUtils.minutesToLabel(s.otMinutes)}\"\n")
            }
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                ?: return@launch
            dir.mkdirs()
            val file = File(dir, "attendance_${monthNames[state.month]}_${state.year}.csv")
            file.writeText(sb.toString())
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export attendance CSV"))
        }
    }

    private data class DayInfo(val dayOfMonth: Int, val dateKey: String, val isFuture: Boolean)

    private fun getDaysInMonth(year: Int, month: Int): List<DayInfo> {
        val cal = Calendar.getInstance().apply { set(year, month, 1) }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val todayCal = Calendar.getInstance()
        return (1..daysInMonth).map { day ->
            val key = "$year-${(month + 1).toString().padStart(2,'0')}-${day.toString().padStart(2,'0')}"
            val isFuture = Calendar.getInstance().apply { set(year, month, day) }.after(todayCal)
            DayInfo(day, key, isFuture)
        }
    }
}
