package com.factoryattendance.util

import com.factoryattendance.domain.model.HoursSummary
import com.factoryattendance.domain.model.Shift

object TimeUtils {

    fun parseToMinutes(time: String): Int? {
        if (time.isBlank()) return null
        val parts = time.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        return h * 60 + m
    }

    fun minutesToLabel(mins: Int?): String {
        if (mins == null || mins < 0) return "--"
        return "${mins / 60}h ${(mins % 60).toString().padStart(2, '0')}m"
    }

    fun calculateHours(timeIn: String, timeOut: String, shift: Shift): HoursSummary {
        val inM  = parseToMinutes(timeIn)  ?: return HoursSummary.EMPTY
        val outM = parseToMinutes(timeOut) ?: return HoursSummary.EMPTY
        if (outM <= inM) return HoursSummary(null, null, null, isInvalid = true)

        val shiftStartM = parseToMinutes(shift.startTime) ?: return HoursSummary.EMPTY
        val shiftEndM   = parseToMinutes(shift.endTime)   ?: return HoursSummary.EMPTY
        val overnight   = shiftEndM < shiftStartM

        val normal: Int
        val ot: Int
        if (overnight) {
            val normMins = (1440 - shiftStartM) + shiftEndM
            normal = minOf(outM - inM, normMins)
            ot = maxOf(0, (outM - inM) - normMins)
        } else {
            normal = maxOf(0, minOf(outM, shiftEndM) - maxOf(inM, shiftStartM))
            ot = maxOf(0, outM - shiftEndM)
        }
        return HoursSummary(totalMinutes = outM - inM, normalMinutes = normal, overtimeMinutes = ot)
    }

    fun isLate(timeIn: String, shift: Shift): Boolean {
        val inM = parseToMinutes(timeIn) ?: return false
        val startM = parseToMinutes(shift.startTime) ?: return false
        return inM > startM + shift.lateThresholdMinutes
    }

    fun nowHHMM(): String {
        val c = java.util.Calendar.getInstance()
        return "${c.get(java.util.Calendar.HOUR_OF_DAY).toString().padStart(2,'0')}" +
               ":${c.get(java.util.Calendar.MINUTE).toString().padStart(2,'0')}"
    }

    fun todayIso(): String {
        val c = java.util.Calendar.getInstance()
        return "${c.get(java.util.Calendar.YEAR)}-" +
               "${(c.get(java.util.Calendar.MONTH)+1).toString().padStart(2,'0')}-" +
               "${c.get(java.util.Calendar.DAY_OF_MONTH).toString().padStart(2,'0')}"
    }

    fun isoToDisplay(iso: String): String {
        val parts = iso.split("-")
        return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else iso
    }
}
