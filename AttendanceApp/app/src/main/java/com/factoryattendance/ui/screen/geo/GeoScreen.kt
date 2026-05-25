package com.factoryattendance.ui.screen.geo

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.factoryattendance.service.GeoFenceForegroundService
import com.factoryattendance.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeoScreen(viewModel: GeoViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    fun hasLocationPermission() = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) viewModel.onTrackingChanged(true)
    }

    var service by remember { mutableStateOf<GeoFenceForegroundService?>(null) }
    val connection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                service = (binder as? GeoFenceForegroundService.LocalBinder)?.getService()
            }
            override fun onServiceDisconnected(name: ComponentName?) { service = null }
        }
    }

    LaunchedEffect(state.isTracking) {
        if (state.isTracking) {
            val intent = Intent(context, GeoFenceForegroundService::class.java)
            context.startForegroundService(intent)
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        } else {
            runCatching { context.unbindService(connection) }
            context.stopService(Intent(context, GeoFenceForegroundService::class.java))
        }
    }

    LaunchedEffect(service) {
        service?.locationState?.collect { viewModel.onLocationUpdate(it) }
    }
    LaunchedEffect(service) {
        service?.checkInLog?.collect { viewModel.onLogUpdate(it) }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Tracking toggle card
        Card {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Geo-fence tracking", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (state.isTracking) "On · tracking active" else "Off — battery saving",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.isTracking,
                        onCheckedChange = { enabled ->
                            if (enabled && !hasLocationPermission()) {
                                permissionLauncher.launch(arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ))
                            } else {
                                viewModel.onTrackingChanged(enabled)
                            }
                        }
                    )
                }

                if (state.isTracking) {
                    val loc = state.locationState
                    val hasFactory = state.factoryLat != 0.0 || state.factoryLng != 0.0
                    val inside = loc.isInsideZone
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            !hasFactory -> MaterialTheme.colorScheme.surfaceVariant
                            inside -> Green10
                            else -> Red10
                        }
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(when { !hasFactory -> "📍"; inside -> "✅"; else -> "❌" }, fontSize = 36.sp)
                            Text(
                                when { !hasFactory -> "Factory location not set"; inside -> "Inside factory zone"; else -> "Outside factory zone" },
                                fontWeight = FontWeight.Bold,
                                color = when { !hasFactory -> MaterialTheme.colorScheme.onSurfaceVariant; inside -> Green40; else -> Red40 }
                            )
                            if (hasFactory && loc.distanceMeters >= 0) {
                                Text("${loc.distanceMeters.toInt()}m from center · fence ${state.radius}m",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (loc.lat != 0.0) {
                                Text("%.5f, %.5f".format(loc.lat, loc.lng),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        if (state.isAdmin) {
            // Set from GPS
            Card {
                Box(Modifier.padding(14.dp)) {
                    OutlinedButton(
                        onClick = {
                            val loc = state.locationState
                            if (loc.lat != 0.0) viewModel.setFactoryLocation(loc.lat, loc.lng)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Flag, null); Spacer(Modifier.width(6.dp))
                        Text("Set factory location from here")
                    }
                }
            }

            // Radius
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("GEO-FENCE RADIUS", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Allowed radius", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Text("${state.radius}m", fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary, fontSize = 17.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("50m", style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = state.radius.toFloat(),
                            onValueChange = { viewModel.setRadius(it.toInt()) },
                            valueRange = 50f..1000f, steps = 18, modifier = Modifier.weight(1f)
                        )
                        Text("1km", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Factory location info
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("FACTORY LOCATION", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    listOf(
                        "Latitude" to if (state.factoryLat == 0.0) "Not set" else "%.6f".format(state.factoryLat),
                        "Longitude" to if (state.factoryLng == 0.0) "Not set" else "%.6f".format(state.factoryLng),
                        "Radius" to "${state.radius}m"
                    ).forEachIndexed { i, (label, value) ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, style = MaterialTheme.typography.bodySmall)
                            Text(value, fontWeight = FontWeight.SemiBold)
                        }
                        if (i < 2) HorizontalDivider()
                    }
                }
            }

            // Log
            Card {
                Column(Modifier.padding(14.dp)) {
                    Text("AUTO CHECK-IN LOG", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    if (state.log.isEmpty()) {
                        Text("No events yet", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        state.log.take(20).forEach { entry ->
                            Text(entry, style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            }
        } else {
            Card {
                Text("🔒 Geo-fence settings are managed by admin. You can turn tracking on/off above.",
                    modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
