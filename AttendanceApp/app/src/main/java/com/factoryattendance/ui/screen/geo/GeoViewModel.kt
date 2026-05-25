package com.factoryattendance.ui.screen.geo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.factoryattendance.data.preferences.AppPreferences
import com.factoryattendance.service.LocationState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GeoUiState(
    val isTracking: Boolean = false,
    val locationState: LocationState = LocationState(),
    val factoryLat: Double = 0.0,
    val factoryLng: Double = 0.0,
    val radius: Int = 200,
    val isAdmin: Boolean = false,
    val log: List<String> = emptyList()
)

@HiltViewModel
class GeoViewModel @Inject constructor(
    private val prefs: AppPreferences
) : ViewModel() {

    private val _isTracking = MutableStateFlow(false)
    private val _locationState = MutableStateFlow(LocationState())
    private val _log = MutableStateFlow<List<String>>(emptyList())

    val uiState: StateFlow<GeoUiState> = combine(
        _isTracking,
        _locationState,
        prefs.factoryLat,
        prefs.factoryLng,
        prefs.fenceRadius
    ) { tracking, loc, lat, lng, radius ->
        GeoUiState(tracking, loc, lat, lng, radius)
    }.combine(prefs.isAdmin) { state, admin ->
        state.copy(isAdmin = admin)
    }.combine(_log) { state, log ->
        state.copy(log = log)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GeoUiState())

    fun onTrackingChanged(tracking: Boolean) { _isTracking.value = tracking }
    fun onLocationUpdate(state: LocationState) { _locationState.value = state }
    fun onLogUpdate(log: List<String>) { _log.value = log }

    fun setFactoryLocation(lat: Double, lng: Double) {
        viewModelScope.launch {
            prefs.setFactoryLocation(lat, lng)
        }
    }

    fun setRadius(r: Int) {
        viewModelScope.launch { prefs.setFenceRadius(r) }
    }
}
