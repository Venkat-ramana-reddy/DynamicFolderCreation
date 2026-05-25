package com.factoryattendance.domain.model

enum class AttendanceStatus(val label: String) {
    PRESENT("Present"),
    ABSENT("Absent"),
    LATE("Late"),
    OFF("Off"),
    UNMARKED("Unmarked")
}
