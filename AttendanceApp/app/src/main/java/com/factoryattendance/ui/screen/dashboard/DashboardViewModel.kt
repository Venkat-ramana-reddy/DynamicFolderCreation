package com.factoryattendance.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.factoryattendance.data.preferences.AppPreferences
import com.factoryattendance.data.repository.AttendanceRepository
import com.factoryattendance.data.repository.ShiftRepository
import com.factoryattendance.domain.model.DashboardStats
import com.factoryattendance.domain.model.Shift
import com.factoryattendance.util.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val stats: DashboardStats = DashboardStats(),
    val shifts: List<Shift> = emptyList(),
    val loading: Boolean = false,
    val syncStatus: String = "Not synced"
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val attendanceRepo: AttendanceRepository,
    private val shiftRepo: ShiftRepository,
    private val prefs: AppPreferences
) : ViewModel() {

    private val today = TimeUtils.todayIso()

    val uiState: StateFlow<DashboardUiState> = combine(
        shiftRepo.observeEnabled(),
        attendanceRepo.observeForDate(today)
    ) { shifts, _ ->
        val stats = attendanceRepo.getDashboardStats(today)
        DashboardUiState(stats = stats, shifts = shifts)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState(loading = true))

    val isAdmin: StateFlow<Boolean> = prefs.isAdmin
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isDarkMode: StateFlow<Boolean> = prefs.isDarkMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun toggleDarkMode() {
        viewModelScope.launch { prefs.setDarkMode(!isDarkMode.value) }
    }

    fun sync() {
        viewModelScope.launch {
            attendanceRepo.pushDirty()
        }
    }
}
