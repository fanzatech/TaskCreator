package com.fanzatech.taskcreator

data class Task(
    val id: Int,
    val name: String,
    val description: String,
    val isCompleted: Boolean = false,
    val linkedElapsedMillis: Long = 0L,
    val isTimerLinked: Boolean = false,
    val isDeleted: Boolean = false,
    val startedTimestamp: Long = 0L
)

