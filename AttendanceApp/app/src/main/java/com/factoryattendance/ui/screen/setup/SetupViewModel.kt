package com.factoryattendance.ui.screen.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.factoryattendance.data.local.dao.WorkerDao
import com.factoryattendance.data.preferences.AppPreferences
import com.factoryattendance.data.repository.AttendanceRepository
import com.factoryattendance.data.repository.ShiftRepository
import com.factoryattendance.data.repository.WorkerRepository
import com.factoryattendance.domain.model.Shift
import com.factoryattendance.domain.model.Worker
import com.factoryattendance.util.HashUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupUiState(
    val workers: List<Worker> = emptyList(),
    val shifts: List<Shift> = emptyList(),
    val supabaseUrl: String = "",
    val supabaseKey: String = "",
    val connectionStatus: String = "",
    val isAdmin: Boolean = false
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val workerRepo: WorkerRepository,
    private val workerDao: WorkerDao,
    private val shiftRepo: ShiftRepository,
    private val prefs: AppPreferences,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    val uiState: StateFlow<SetupUiState> = combine(
        workerRepo.observeWorkers(),
        shiftRepo.observeAll(),
        prefs.supabaseUrl,
        prefs.supabaseKey,
        prefs.isAdmin
    ) { workers, shifts, url, key, admin ->
        SetupUiState(workers, shifts, url, key, isAdmin = admin)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SetupUiState())

    fun addWorker(name: String, dept: String, shift: String) {
        if (name.isBlank()) return
        viewModelScope.launch { workerRepo.addWorker(name, dept.ifBlank { "General" }, shift) }
    }

    fun deleteWorker(id: String) {
        viewModelScope.launch { workerRepo.deleteWorker(id) }
    }

    fun updateWorkerShift(workerId: String, shiftName: String) {
        viewModelScope.launch { workerRepo.updateWorkerShift(workerId, shiftName) }
    }

    fun saveShift(shift: Shift) {
        viewModelScope.launch { shiftRepo.save(shift) }
    }

    fun saveSupabaseSettings(url: String, key: String) {
        viewModelScope.launch {
            prefs.setSupabaseUrl(url)
            prefs.setSupabaseKey(key)
        }
    }

    fun testConnection(url: String, key: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            onResult("Testing…")
            try {
                val result = supabaseClient.postgrest["workers"].select { limit(1) }
                onResult("✓ Connection successful")
            } catch (e: Exception) {
                onResult("✗ Failed: ${e.message}")
            }
        }
    }

    fun changeAdminPin(newPin: String, onResult: (String) -> Unit) {
        if (!Regex("""^\d{4}$""").matches(newPin)) {
            onResult("PIN must be exactly 4 digits")
            return
        }
        viewModelScope.launch {
            prefs.setAdminPinHash(HashUtils.sha256(newPin))
            onResult("PIN changed ✓")
        }
    }

    fun logout() {
        viewModelScope.launch {
            prefs.setAdmin(false)
            prefs.setCurrentWorker(null)
        }
    }
}
