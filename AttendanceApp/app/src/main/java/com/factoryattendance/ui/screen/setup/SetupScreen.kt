package com.factoryattendance.ui.screen.setup

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.factoryattendance.domain.model.Shift
import com.factoryattendance.ui.component.ShiftBadge
import com.factoryattendance.ui.theme.Red10
import com.factoryattendance.ui.theme.Red40

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(viewModel: SetupViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    if (!state.isAdmin) {
        // Locked view for non-admins
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🔒", fontSize = 52.sp)
            Spacer(Modifier.height(14.dp))
            Text("Admin only", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Setup is restricted to admins.\nTap your name badge and log in as Admin to access settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Worker management
        WorkerManagementCard(state = state, viewModel = viewModel)

        // Shift configuration
        ShiftConfigSection(shifts = state.shifts, viewModel = viewModel)

        // Supabase settings
        SupabaseSettingsCard(state = state, viewModel = viewModel)

        // Admin PIN
        AdminPinCard(viewModel = viewModel)

        // Logout
        OutlinedButton(
            onClick = { viewModel.logout() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Red40)
        ) {
            Icon(Icons.Default.Logout, null)
            Spacer(Modifier.width(8.dp))
            Text("Logout / Lock admin")
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun WorkerManagementCard(state: SetupUiState, viewModel: SetupViewModel) {
    var newName  by remember { mutableStateOf("") }
    var newDept  by remember { mutableStateOf("") }
    var newShift by remember { mutableStateOf("General") }
    var showConfirmDelete by remember { mutableStateOf<String?>(null) }

    Card {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("MANAGE WORKERS", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            state.workers.forEach { worker ->
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ShiftBadge(worker.shift, Modifier.size(26.dp))
                    Column(Modifier.weight(1f)) {
                        Text(worker.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("${worker.dept} · ${worker.id}", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    // Shift selector
                    var expanded by remember { mutableStateOf(false) }
                    val enabledShifts = state.shifts.filter { it.enabled }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value = worker.shift,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.menuAnchor().width(90.dp),
                            textStyle = MaterialTheme.typography.labelSmall
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            enabledShifts.forEach { sh ->
                                DropdownMenuItem(
                                    text = { Text(if (sh.alwaysOn) "General" else "Shift ${sh.name}") },
                                    onClick = {
                                        viewModel.updateWorkerShift(worker.id, sh.name)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    // Delete
                    IconButton(onClick = { showConfirmDelete = worker.id }) {
                        Icon(Icons.Default.DeleteOutline, null, tint = Red40)
                    }
                }
                HorizontalDivider()
            }

            // Add worker form
            Text("Add worker", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(newName, { newName = it }, Modifier.weight(2f),
                    label = { Text("Name") }, singleLine = true)
                OutlinedTextField(newDept, { newDept = it }, Modifier.weight(1f),
                    label = { Text("Dept") }, singleLine = true)
            }
            Button(
                onClick = {
                    viewModel.addWorker(newName, newDept, newShift)
                    newName = ""; newDept = ""
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = newName.isNotBlank()
            ) { Text("+ Add worker") }
        }
    }

    showConfirmDelete?.let { wId ->
        val wName = state.workers.find { it.id == wId }?.name ?: ""
        AlertDialog(
            onDismissRequest = { showConfirmDelete = null },
            title = { Text("Remove $wName?") },
            text = { Text("This will delete all attendance records for this worker.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteWorker(wId); showConfirmDelete = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = Red40)) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { showConfirmDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ShiftConfigSection(shifts: List<Shift>, viewModel: SetupViewModel) {
    Card {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("SHIFT SETTINGS", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            shifts.forEach { shift ->
                ShiftConfigCard(shift = shift, onSave = { viewModel.saveShift(it) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShiftConfigCard(shift: Shift, onSave: (Shift) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var startTime by remember(shift.id) { mutableStateOf(shift.startTime) }
    var endTime   by remember(shift.id) { mutableStateOf(shift.endTime) }
    var lateMin   by remember(shift.id) { mutableStateOf(shift.lateThresholdMinutes.toString()) }
    var enabled   by remember(shift.id) { mutableStateOf(shift.enabled) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = if (enabled) 2.dp else 0.dp,
        border = ButtonDefaults.outlinedButtonBorder
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(11.dp, 11.dp, 11.dp, 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ShiftBadge(shift.name)
                Column(Modifier.weight(1f)) {
                    Text(
                        if (shift.alwaysOn) "General" else "Shift ${shift.name}",
                        fontWeight = FontWeight.SemiBold, fontSize = 13.sp
                    )
                    Text(
                        if (enabled) "$startTime → $endTime · Late: ${lateMin}m" else "Disabled",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!shift.alwaysOn) {
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }
            }

            // Expanded body
            if (expanded) {
                HorizontalDivider()
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(startTime, { startTime = it }, Modifier.weight(1f),
                            label = { Text("Start time") }, singleLine = true,
                            placeholder = { Text("HH:MM") })
                        OutlinedTextField(endTime, { endTime = it }, Modifier.weight(1f),
                            label = { Text("End time") }, singleLine = true,
                            placeholder = { Text("HH:MM") })
                    }
                    OutlinedTextField(lateMin, { lateMin = it }, Modifier.fillMaxWidth(),
                        label = { Text("Late threshold (minutes)") }, singleLine = true)
                    Button(
                        onClick = {
                            onSave(shift.copy(
                                startTime = startTime,
                                endTime = endTime,
                                lateThresholdMinutes = lateMin.toIntOrNull() ?: 15,
                                enabled = enabled || shift.alwaysOn
                            ))
                            expanded = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save shift") }
                }
            }
        }
    }
}

@Composable
private fun SupabaseSettingsCard(state: SetupUiState, viewModel: SetupViewModel) {
    var url by remember(state.supabaseUrl) { mutableStateOf(state.supabaseUrl) }
    var key by remember(state.supabaseKey) { mutableStateOf(state.supabaseKey) }
    var connectionStatus by remember { mutableStateOf("") }

    Card {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("SUPABASE SETTINGS", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(url, { url = it }, Modifier.fillMaxWidth(),
                label = { Text("Project URL") }, singleLine = true,
                placeholder = { Text("https://xyz.supabase.co") })
            OutlinedTextField(key, { key = it }, Modifier.fillMaxWidth(),
                label = { Text("Anon key") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.saveSupabaseSettings(url, key) },
                    modifier = Modifier.weight(1f)
                ) { Text("Save") }
                OutlinedButton(
                    onClick = { viewModel.testConnection(url, key) { connectionStatus = it } },
                    modifier = Modifier.weight(1f)
                ) { Text("Test") }
            }
            if (connectionStatus.isNotBlank()) {
                Text(connectionStatus, style = MaterialTheme.typography.bodySmall,
                    color = if (connectionStatus.startsWith("✓")) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun AdminPinCard(viewModel: SetupViewModel) {
    var newPin by remember { mutableStateOf("") }
    var pinStatus by remember { mutableStateOf("") }

    Card {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("ADMIN PIN", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("PIN is stored as SHA-256 hash and cannot be read from storage.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = newPin,
                onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPin = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("New 4-digit PIN") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            Button(
                onClick = { viewModel.changeAdminPin(newPin) { pinStatus = it; if (it.contains("✓")) newPin = "" } },
                modifier = Modifier.fillMaxWidth(),
                enabled = newPin.length == 4
            ) { Text("Save new PIN") }
            if (pinStatus.isNotBlank()) {
                Text(pinStatus, style = MaterialTheme.typography.bodySmall,
                    color = if (pinStatus.contains("✓")) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.error)
            }
        }
    }
}
