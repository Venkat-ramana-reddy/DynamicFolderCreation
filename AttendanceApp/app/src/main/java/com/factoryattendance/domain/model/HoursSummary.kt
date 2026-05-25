package com.factoryattendance.domain.model

data class HoursSummary(
    val totalMinutes: Int?,
    val normalMinutes: Int?,
    val overtimeMinutes: Int?,
    val isInvalid: Boolean = false
) {
    val hasOvertime: Boolean get() = (overtimeMinutes ?: 0) > 0

    companion object {
        val EMPTY = HoursSummary(null, null, null)
    }
}
