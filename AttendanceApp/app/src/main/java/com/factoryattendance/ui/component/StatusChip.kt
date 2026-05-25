package com.factoryattendance.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.factoryattendance.domain.model.AttendanceStatus
import com.factoryattendance.ui.theme.*

@Composable
fun StatusChip(
    status: AttendanceStatus,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (bg, fg, border) = when (status) {
        AttendanceStatus.PRESENT  -> Triple(Green10,  Green40,  Green40)
        AttendanceStatus.ABSENT   -> Triple(Red10,    Red40,    Red40)
        AttendanceStatus.LATE     -> Triple(Amber10,  Amber40,  Amber40)
        AttendanceStatus.OFF      -> Triple(Surface2, Text3,    Border)
        AttendanceStatus.UNMARKED -> Triple(Surface2, Text3,    Border)
    }
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) bg else Color.Transparent,
        border = BorderStroke(1.5.dp, border),
        tonalElevation = 0.dp
    ) {
        Text(
            text = status.label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) fg else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
