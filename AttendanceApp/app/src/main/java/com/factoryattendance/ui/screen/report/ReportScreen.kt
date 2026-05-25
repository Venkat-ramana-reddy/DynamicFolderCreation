package com.factoryattendance.ui.screen.report

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.factoryattendance.domain.model.AttendanceStatus
import com.factoryattendance.ui.theme.*
import com.factoryattendance.util.TimeUtils

private val MONTH_NAMES = listOf("January","February","March","April","May","June",
    "July","August","September","October","November","December")
private val DAY_HEADERS = listOf("Su","Mo","Tu","We","Th","Fr","Sa")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(viewModel: ReportViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Today's hours
        Card {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("TODAY'S HOURS", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (state.todayHours.isEmpty()) {
                    Text("No hours recorded today", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp))
                } else {
                    state.todayHours.forEach { (worker, rec) ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(worker.name, Modifier.weight(1f), fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${rec.timeIn.ifBlank{"--"}} → ${rec.timeOut.ifBlank{"--"}}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        HorizontalDivider()
                    }
                }
            }
        }

        HorizontalDivider()
        Text("MONTHLY REPORT", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

        // Month selector
        Card {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.changeMonth(-1) }) {
                    Icon(Icons.Default.ChevronLeft, null)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${MONTH_NAMES[state.month]} ${state.year}",
                        fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("${state.recordedDays} day${if (state.recordedDays != 1) "s" else ""} recorded",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { viewModel.changeMonth(1) }) {
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
        }

        // Stats grid
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            com.factoryattendance.ui.component.StatCard(
                "Working days", state.recordedDays.toString(), modifier = Modifier.weight(1f))
            com.factoryattendance.ui.component.StatCard(
                "Avg attendance", "${(state.avgAttendance * 100).toInt()}%",
                valueColor = Green40, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            com.factoryattendance.ui.component.StatCard(
                "Total OT hrs", TimeUtils.minutesToLabel(state.totalOtMinutes),
                valueColor = Amber40, modifier = Modifier.weight(1f))
            com.factoryattendance.ui.component.StatCard(
                "Perfect attend.", state.perfectCount.toString(), modifier = Modifier.weight(1f))
        }

        // Worker summary table
        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp, 12.dp, 12.dp, 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("WORKER SUMMARY", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.exportCsv(context) }, contentPadding = PaddingValues(4.dp)) {
                        Icon(Icons.Default.Download, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Export CSV", fontSize = 11.sp)
                    }
                }
                // Header
                Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text("WORKER", Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    listOf("P" to Green40, "A" to Red40, "L" to Amber40,
                        "OFF" to MaterialTheme.colorScheme.onSurfaceVariant,
                        "OT HRS" to Amber40).forEach { (lbl, color) ->
                        Text(lbl, Modifier.width(40.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            color = color, textAlign = TextAlign.Center)
                    }
                }
                state.workerStats.forEach { s ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(s.worker.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(s.worker.dept, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${s.present}", Modifier.width(40.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = Green40, textAlign = TextAlign.Center)
                        Text("${s.absent}",  Modifier.width(40.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = Red40,  textAlign = TextAlign.Center)
                        Text("${s.late}",    Modifier.width(40.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = Amber40, textAlign = TextAlign.Center)
                        Text("${s.off}",     Modifier.width(40.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        Text(
                            if (s.otMinutes > 0) TimeUtils.minutesToLabel(s.otMinutes) else "--",
                            Modifier.width(40.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            color = Amber40, textAlign = TextAlign.Right
                        )
                    }
                    HorizontalDivider()
                }
            }
        }

        // Calendar heatmap
        Card {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("DAILY VIEW", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    if (state.workers.isNotEmpty()) {
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                            OutlinedTextField(
                                value = state.workers.find { it.id == state.selectedWorkerId }?.name?.split(" ")?.firstOrNull() ?: "",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                modifier = Modifier.menuAnchor().width(120.dp),
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                state.workers.forEach { w ->
                                    DropdownMenuItem(
                                        text = { Text(w.name.split(" ").firstOrNull() ?: w.name) },
                                        onClick = { viewModel.selectWorker(w.id); expanded = false }
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                CalendarHeatmap(state.calendarDays)
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun CalendarHeatmap(days: List<CalendarDay?>) {
    // Day headers
    Row(modifier = Modifier.fillMaxWidth()) {
        DAY_HEADERS.forEach { hdr ->
            Text(hdr, Modifier.weight(1f), textAlign = TextAlign.Center,
                fontSize = 9.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Spacer(Modifier.height(4.dp))
    // Calendar grid rows
    days.chunked(7).forEach { week ->
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            val cells = if (week.size < 7) week + List(7 - week.size) { null } else week
            cells.forEach { day ->
                Box(modifier = Modifier.weight(1f).padding(2.dp)) {
                    if (day == null) {
                        Spacer(Modifier.size(34.dp))
                    } else {
                        val (bgColor, textColor) = when {
                            day.isFuture -> Pair(Color.Transparent, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                            day.status == AttendanceStatus.PRESENT -> Pair(Green10, Green40)
                            day.status == AttendanceStatus.ABSENT  -> Pair(Red10, Red40)
                            day.status == AttendanceStatus.LATE    -> Pair(Amber10, Amber40)
                            day.status == AttendanceStatus.OFF     -> Pair(Surface2, Text3)
                            else -> Pair(Surface2, Text3)
                        }
                        Box(
                            modifier = Modifier
                                .height(34.dp)
                                .fillMaxWidth()
                                .background(bgColor, RoundedCornerShape(7.dp))
                                .then(if (day.isToday) Modifier.border(1.5.dp, Blue40, RoundedCornerShape(7.dp)) else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${day.dayOfMonth}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
                                val lbl = when (day.status) {
                                    AttendanceStatus.PRESENT  -> "P"
                                    AttendanceStatus.ABSENT   -> "A"
                                    AttendanceStatus.LATE     -> "L"
                                    AttendanceStatus.OFF      -> "Off"
                                    else -> ""
                                }
                                if (!day.isFuture && lbl.isNotEmpty()) {
                                    Text(lbl, fontSize = 8.sp, color = textColor)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    // Legend
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf("P" to Green40, "A" to Red40, "L" to Amber40).forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(10.dp).background(color.copy(alpha = 0.2f), RoundedCornerShape(3.dp))
                    .border(1.dp, color, RoundedCornerShape(3.dp)))
                Text("$label = ${when(label){"P"->"Present";"A"->"Absent";else->"Late"}}",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
