package com.factoryattendance.ui.screen.who

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.factoryattendance.domain.model.Worker
import com.factoryattendance.ui.component.PinOverlay
import com.factoryattendance.ui.theme.Amber40
import com.factoryattendance.ui.theme.Amber10
import com.factoryattendance.ui.theme.Blue10
import com.factoryattendance.ui.theme.Blue40

@Composable
fun WhoScreen(viewModel: WhoViewModel) {
    val workers by viewModel.workers.collectAsState()
    val showPin by viewModel.showPinDialog.collectAsState()
    val pinError by viewModel.pinError.collectAsState()
    val currentWorkerId by viewModel.currentWorkerId.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))
            Text("👤", fontSize = 40.sp)
            Spacer(Modifier.height(8.dp))
            Text("Who are you?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Select your name to check in / out. Others are view-only.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(workers) { worker ->
                    WorkerCard(
                        worker = worker,
                        isSelected = worker.id == currentWorkerId,
                        onClick = { viewModel.selectWorker(worker) }
                    )
                }
                item {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    // Admin card
                    Surface(
                        onClick = { viewModel.showAdminPinDialog() },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = ButtonDefaults.outlinedButtonBorder,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Amber10),
                                contentAlignment = Alignment.Center
                            ) { Text("🔒", fontSize = 18.sp) }
                            Column {
                                Text("Admin", fontWeight = FontWeight.SemiBold, color = Amber40)
                                Text("Full access — PIN required",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    if (showPin) {
        PinOverlay(
            title = "🔒 Admin Login",
            subtitle = "Enter your 4-digit PIN",
            error = pinError,
            onPinEntered = { viewModel.verifyAdminPin(it) },
            onDismiss = { viewModel.dismissPinDialog() }
        )
    }
}

@Composable
private fun WorkerCard(worker: Worker, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) Blue10 else MaterialTheme.colorScheme.surface,
        border = if (isSelected) ButtonDefaults.outlinedButtonBorder else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Blue10),
                contentAlignment = Alignment.Center
            ) { Text(worker.initials(), fontWeight = FontWeight.Bold, color = Blue40, fontSize = 14.sp) }
            Column {
                Text(worker.name, fontWeight = FontWeight.SemiBold)
                Text(worker.dept, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
