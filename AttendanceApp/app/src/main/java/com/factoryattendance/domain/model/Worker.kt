package com.factoryattendance.domain.model

data class Worker(
    val id: String,
    val name: String,
    val dept: String,
    val shift: String = "General",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun initials(): String = name.split(" ")
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .take(2)
        .joinToString("")
}
