package com.factoryattendance.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.factoryattendance.ui.theme.*

@Composable
fun ShiftBadge(shiftName: String, modifier: Modifier = Modifier) {
    val (bg, fg) = when (shiftName) {
        "General" -> Pair(Surface2, Text2)
        "A"       -> Pair(Blue10,   Blue40)
        "B"       -> Pair(Green10,  Green40)
        "C"       -> Pair(Amber10,  Amber40)
        "D"       -> Pair(Color(0xFFF3E8FA), Color(0xFF7A2090))
        else      -> Pair(Surface2, Text2)
    }
    val label = if (shiftName == "General") "Gen" else shiftName
    Box(
        modifier = modifier
            .size(26.dp)
            .background(bg, RoundedCornerShape(6.dp))
            .border(1.dp, Border, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = if (shiftName == "General") 8.sp else 11.sp,
            fontWeight = FontWeight.Bold, color = fg)
    }
}
