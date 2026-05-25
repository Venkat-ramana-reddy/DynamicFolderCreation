package com.factoryattendance.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun PinOverlay(
    title: String = "Admin Login",
    subtitle: String = "Enter your 4-digit PIN",
    error: Boolean = false,
    onPinEntered: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    val shakeError = remember { mutableStateOf(false) }

    LaunchedEffect(error) {
        if (error) {
            shakeError.value = true
            kotlinx.coroutines.delay(700)
            pin = ""
            shakeError.value = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                // Dot indicators
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(4) { i ->
                        val filled = i < pin.length
                        val color = when {
                            shakeError.value -> MaterialTheme.colorScheme.error
                            filled -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outline
                        }
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(if (filled || shakeError.value) color else Color.Transparent)
                                .then(
                                    if (!filled && !shakeError.value)
                                        Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                                    else Modifier
                                )
                        )
                    }
                }

                // Numpad
                val keys = listOf("1","2","3","4","5","6","7","8","9","","0","⌫")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    keys.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { key ->
                                if (key.isEmpty()) {
                                    Spacer(Modifier.size(80.dp, 56.dp))
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            when (key) {
                                                "⌫" -> if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                                else -> if (pin.length < 4) {
                                                    pin += key
                                                    if (pin.length == 4) onPinEntered(pin)
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(80.dp, 56.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(key, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel")
                }
            }
        }
    }
}
