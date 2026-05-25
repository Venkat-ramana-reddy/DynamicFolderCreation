package com.factoryattendance.ui.screen.who

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.factoryattendance.data.local.dao.WorkerDao
import com.factoryattendance.data.preferences.AppPreferences
import com.factoryattendance.data.repository.ShiftRepository
import com.factoryattendance.data.repository.WorkerRepository
import com.factoryattendance.domain.model.Worker
import com.factoryattendance.util.HashUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WhoViewModel @Inject constructor(
    private val workerRepo: WorkerRepository,
    private val workerDao: WorkerDao,
    private val shiftRepo: ShiftRepository,
    private val prefs: AppPreferences
) : ViewModel() {

    val workers: StateFlow<List<Worker>> = workerRepo.observeWorkers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showWhoScreen = MutableStateFlow(true)
    val showWhoScreen: StateFlow<Boolean> = _showWhoScreen

    private val _showPinDialog = MutableStateFlow(false)
    val showPinDialog: StateFlow<Boolean> = _showPinDialog

    private val _pinError = MutableStateFlow(false)
    val pinError: StateFlow<Boolean> = _pinError

    val isAdmin: StateFlow<Boolean> = prefs.isAdmin
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val currentWorkerId: StateFlow<String?> = prefs.currentWorkerId
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            shiftRepo.seedDefaults()
            shiftRepo.seedDefaultWorkers(workerDao)
        }
    }

    fun selectWorker(worker: Worker) {
        viewModelScope.launch {
            prefs.setAdmin(false)
            prefs.setCurrentWorker(worker.id)
            _showWhoScreen.value = false
        }
    }

    fun showAdminPinDialog() { _showPinDialog.value = true }
    fun dismissPinDialog()   { _showPinDialog.value = false; _pinError.value = false }

    fun verifyAdminPin(pin: String) {
        viewModelScope.launch {
            val stored = prefs.adminPinHash.first()
            if (HashUtils.sha256(pin) == stored) {
                prefs.setAdmin(true)
                prefs.setCurrentWorker(null)
                _showPinDialog.value = false
                _pinError.value = false
                _showWhoScreen.value = false
            } else {
                _pinError.value = true
            }
        }
    }

    fun openWhoScreen() { _showWhoScreen.value = true }
}
