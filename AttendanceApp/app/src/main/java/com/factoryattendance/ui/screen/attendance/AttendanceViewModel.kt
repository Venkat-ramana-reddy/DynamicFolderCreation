package com.factoryattendance.ui.screen.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.factoryattendance.data.preferences.AppPreferences
import com.factoryattendance.data.repository.AttendanceRepository
import com.factoryattendance.data.repository.ShiftRepository
import com.factoryattendance.data.repository.WorkerRepository
import com.factoryattendance.domain.model.*
import com.factoryattendance.util.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkerAttendanceUi(
    val worker: Worker,
    val record: AttendanceRecord,
    val shift: Shift,
    val hours: HoursSummary
)

data class AttendanceUiState(
    val rows: List<WorkerAttendanceUi> = emptyList(),
    val selectedDate: String = TimeUtils.todayIso(),
    val searchQuery: String = "",
    val filterStatus: AttendanceStatus? = null,
    val isAdmin: Boolean = false,
    val currentWorkerId: String? = null,
    val loading: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val workerRepo: WorkerRepository,
    private val attendanceRepo: AttendanceRepository,
    private val shiftRepo: ShiftRepository,
    private val prefs: AppPreferences
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(TimeUtils.todayIso())
    private val _searchQuery  = MutableStateFlow("")
    private val _filterStatus = MutableStateFlow<AttendanceStatus?>(null)

    val uiState: StateFlow<AttendanceUiState> = combine(
        _selectedDate,
        _searchQuery,
        _filterStatus,
        prefs.isAdmin,
        prefs.currentWorkerId
    ) { date, query, filter, isAdmin, workerId ->
        Quintuple(date, query, filter, isAdmin, workerId)
    }.flatMapLatest { (date, query, filter, isAdmin, workerId) ->
        combine(
            workerRepo.observeWorkers(),
            attendanceRepo.observeForDate(date),
            shiftRepo.observeAll()
        ) { workers, records, shifts ->
            val recordMap = records.associateBy { it.workerId }
            val shiftMap  = shifts.associateBy { it.name }
            val defaultShift = shifts.find { it.alwaysOn } ?: shifts.firstOrNull()
                ?: Shift("", "General", "08:30", "17:30")

            val rows = workers.mapNotNull { worker ->
                val record = recordMap[worker.id] ?: AttendanceRecord(
                    id = "", workerId = worker.id, date = date
                )
                val shift = shiftMap[worker.shift] ?: defaultShift
                val hours = TimeUtils.calculateHours(record.timeIn, record.timeOut, shift)
                WorkerAttendanceUi(worker, record, shift, hours)
            }.filter { row ->
                val matchesSearch = query.isBlank() ||
                    row.worker.name.contains(query, ignoreCase = true) ||
                    row.worker.dept.contains(query, ignoreCase = true)
                val matchesFilter = filter == null || row.record.status == filter ||
                    (filter == AttendanceStatus.PRESENT && row.record.status == AttendanceStatus.LATE)
                matchesSearch && matchesFilter
            }
            AttendanceUiState(rows, date, query, filter, isAdmin, workerId)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AttendanceUiState(loading = true))

    fun setDate(date: String)              { _selectedDate.value = date }
    fun setSearchQuery(q: String)          { _searchQuery.value = q }
    fun setFilter(s: AttendanceStatus?)    { _filterStatus.value = s }
    fun changeDate(delta: Int) {
        val parts = _selectedDate.value.split("-").map { it.toInt() }
        val cal = java.util.Calendar.getInstance().apply {
            set(parts[0], parts[1] - 1, parts[2])
            add(java.util.Calendar.DAY_OF_MONTH, delta)
        }
        val y = cal.get(java.util.Calendar.YEAR)
        val m = (cal.get(java.util.Calendar.MONTH) + 1).toString().padStart(2, '0')
        val d = cal.get(java.util.Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        val newDate = "$y-$m-$d"
        if (newDate <= TimeUtils.todayIso()) _selectedDate.value = newDate
    }

    fun canEdit(workerId: String): Boolean {
        val state = uiState.value
        return state.isAdmin || state.currentWorkerId == workerId
    }

    fun setStatus(workerId: String, status: AttendanceStatus) {
        viewModelScope.launch {
            val date = _selectedDate.value
            val existing = attendanceRepo.getOrCreate(workerId, date, "")
            val timeIn = if (existing.timeIn.isBlank() &&
                (status == AttendanceStatus.PRESENT || status == AttendanceStatus.LATE))
                TimeUtils.nowHHMM() else existing.timeIn
            val (effectiveStatus, effectiveTimeIn) = when {
                status == AttendanceStatus.PRESENT && existing.status == AttendanceStatus.LATE ->
                    Pair(AttendanceStatus.PRESENT, timeIn)
                status == AttendanceStatus.ABSENT || status == AttendanceStatus.OFF ->
                    Pair(status, "")
                else -> Pair(status, timeIn)
            }
            val updated = existing.copy(
                status = effectiveStatus,
                timeIn = effectiveTimeIn,
                timeOut = if (status == AttendanceStatus.ABSENT || status == AttendanceStatus.OFF)
                    "" else existing.timeOut
            )
            attendanceRepo.upsert(updated)
        }
    }

    fun setTimeIn(workerId: String, time: String) {
        viewModelScope.launch {
            val date = _selectedDate.value
            val existing = attendanceRepo.getOrCreate(workerId, date, "")
            val shifts = shiftRepo.getShiftsSnapshot()
            val worker = workerRepo.observeWorkers().first().find { it.id == workerId }
            val shift = shifts.find { it.name == worker?.shift } ?: shifts.firstOrNull()
                ?: return@launch
            val newStatus = when {
                time.isBlank() -> AttendanceStatus.UNMARKED
                TimeUtils.isLate(time, shift) -> AttendanceStatus.LATE
                else -> AttendanceStatus.PRESENT
            }
            attendanceRepo.upsert(existing.copy(timeIn = time, status = newStatus))
        }
    }

    fun setTimeOut(workerId: String, time: String) {
        viewModelScope.launch {
            val date = _selectedDate.value
            val existing = attendanceRepo.getOrCreate(workerId, date, "")
            attendanceRepo.upsert(existing.copy(timeOut = time))
        }
    }
}

private data class Quintuple<A,B,C,D,E>(val a: A, val b: B, val c: C, val d: D, val e: E)
private operator fun <A,B,C,D,E> Quintuple<A,B,C,D,E>.component1() = a
private operator fun <A,B,C,D,E> Quintuple<A,B,C,D,E>.component2() = b
private operator fun <A,B,C,D,E> Quintuple<A,B,C,D,E>.component3() = c
private operator fun <A,B,C,D,E> Quintuple<A,B,C,D,E>.component4() = d
private operator fun <A,B,C,D,E> Quintuple<A,B,C,D,E>.component5() = e
