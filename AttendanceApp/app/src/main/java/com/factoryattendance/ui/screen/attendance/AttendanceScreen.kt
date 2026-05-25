package com.factoryattendance.ui.screen.attendance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.factoryattendance.domain.model.AttendanceStatus
import com.factoryattendance.ui.component.ShiftBadge
import com.factoryattendance.ui.component.StatCard
import com.factoryattendance.ui.component.StatusChip
import com.factoryattendance.ui.theme.*
import com.factoryattendance.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(viewModel: AttendanceViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Admin date picker
        if (state.isAdmin) {
            Card(modifier = Modifier.fillMaxWidth().padding(12.dp, 12.dp, 12.dp, 0.dp)) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = { viewModel.changeDate(-1) }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous day")
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        val isToday = state.selectedDate == TimeUtils.todayIso()
                        Text(
                            if (isToday) "Today" else TimeUtils.isoToDisplay(state.selectedDate),
                            fontWeight = FontWeight.Bold, fontSize = 15.sp
                        )
                        Text(
                            if (isToday) "Today's attendance" else "Past attendance — admin only",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isToday) MaterialTheme.colorScheme.onSurfaceVariant else Amber40
                        )
                    }
                    IconButton(
                        onClick = { viewModel.changeDate(1) },
                        enabled = state.selectedDate < TimeUtils.todayIso()
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next day")
                    }
                }
            }
        }

        // Search bar
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search worker...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().padding(12.dp, 8.dp),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Filter chips
        LazyRow(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                FilterChip(
                    selected = state.filterStatus == null,
                    onClick = { viewModel.setFilter(null) },
                    label = { Text("All") }
                )
            }
            items(listOf(AttendanceStatus.PRESENT, AttendanceStatus.ABSENT,
                AttendanceStatus.LATE, AttendanceStatus.OFF)) { status ->
                FilterChip(
                    selected = state.filterStatus == status,
                    onClick = { viewModel.setFilter(if (state.filterStatus == status) null else status) },
                    label = { Text(status.label) }
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.rows.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No workers found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(state.rows, key = { it.worker.id }) { row ->
                    WorkerAttendanceCard(
                        row = row,
                        canEdit = viewModel.canEdit(row.worker.id),
                        isMe = state.currentWorkerId == row.worker.id,
                        onStatusChange = { viewModel.setStatus(row.worker.id, it) },
                        onTimeInChange  = { viewModel.setTimeIn(row.worker.id, it) },
                        onTimeOutChange = { viewModel.setTimeOut(row.worker.id, it) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun WorkerAttendanceCard(
    row: WorkerAttendanceUi,
    canEdit: Boolean,
    isMe: Boolean,
    onStatusChange: (AttendanceStatus) -> Unit,
    onTimeInChange: (String) -> Unit,
    onTimeOutChange: (String) -> Unit
) {
    var timeInDialog  by remember { mutableStateOf(false) }
    var timeOutDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        // Header row: avatar, name, dept, badges
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Avatar
            Surface(
                shape = CircleShape,
                color = if (isMe) Green10 else Blue10,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(row.worker.initials(), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        color = if (isMe) Green40 else Blue40)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(row.worker.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("${row.worker.dept} · ${row.worker.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ShiftBadge(row.worker.shift)
            if (row.hours.hasOvertime) {
                Surface(shape = RoundedCornerShape(8.dp), color = Amber10) {
                    Text("OT", modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Amber40)
                }
            }
            if (!canEdit) Icon(Icons.Default.Lock, contentDescription = null,
                modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(8.dp))

        // Status chips or read-only pill
        if (canEdit) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(AttendanceStatus.PRESENT, AttendanceStatus.LATE,
                    AttendanceStatus.ABSENT, AttendanceStatus.OFF).forEach { s ->
                    val selected = row.record.status == s ||
                        (s == AttendanceStatus.PRESENT && row.record.status == AttendanceStatus.LATE)
                    StatusChip(s, isSelected = selected, onClick = { onStatusChange(s) })
                }
            }
        } else {
            StatusChip(row.record.status, isSelected = true, onClick = {})
        }

        Spacer(Modifier.height(8.dp))

        // Time in / out
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimeField(
                label = "→ Time in",
                value = row.record.timeIn,
                enabled = canEdit,
                modifier = Modifier.weight(1f),
                onClick = { if (canEdit) timeInDialog = true }
            )
            TimeField(
                label = "← Time out",
                value = row.record.timeOut,
                enabled = canEdit,
                modifier = Modifier.weight(1f),
                onClick = { if (canEdit) timeOutDialog = true }
            )
        }

        // Hours bar
        val normal = row.hours.normalMinutes
        val ot     = row.hours.overtimeMinutes
        if (normal != null && !row.hours.isInvalid) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(
                    progress = { minOf(1f, (normal.toFloat()) / 600f) },
                    modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = if ((ot ?: 0) > 0) Amber40 else Green40
                )
                Text(
                    TimeUtils.minutesToLabel(normal) + if ((ot ?: 0) > 0) " +${TimeUtils.minutesToLabel(ot)}" else "",
                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    color = if ((ot ?: 0) > 0) Amber40 else Green40
                )
            }
        }
    }

    // Time picker dialogs
    if (timeInDialog) {
        TimePickerDialog(
            initial = row.record.timeIn,
            onConfirm = { onTimeInChange(it); timeInDialog = false },
            onDismiss = { timeInDialog = false }
        )
    }
    if (timeOutDialog) {
        TimePickerDialog(
            initial = row.record.timeOut,
            onConfirm = { onTimeOutChange(it); timeOutDialog = false },
            onDismiss = { timeOutDialog = false }
        )
    }
}

@Composable
private fun TimeField(label: String, value: String, enabled: Boolean,
                     modifier: Modifier, onClick: () -> Unit) {
    OutlinedTextField(
        value = value.ifBlank { "--:--" },
        onValueChange = {},
        label = { Text(label, fontSize = 10.sp) },
        readOnly = true,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(9.dp),
        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }.also { source ->
            LaunchedEffect(source) {
                source.interactions.collect { if (it is androidx.compose.foundation.interaction.PressInteraction.Release) onClick() }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val parts = initial.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: 8
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val state = rememberTimePickerState(initialHour = h, initialMinute = m, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select time") },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm("${state.hour.toString().padStart(2,'0')}:${state.minute.toString().padStart(2,'0')}")
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
