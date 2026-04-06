package com.fanzatech.taskcreator

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String,
    val isCompleted: Boolean,
    val linkedElapsedMillis: Long,
    val isTimerLinked: Boolean,
    val isDeleted: Boolean,
    val startedTimestamp: Long
)

fun TaskEntity.toTask(): Task = Task(
    id = id,
    name = name,
    description = description,
    isCompleted = isCompleted,
    linkedElapsedMillis = linkedElapsedMillis,
    isTimerLinked = isTimerLinked,
    isDeleted = isDeleted,
    startedTimestamp = startedTimestamp
)

fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    name = name,
    description = description,
    isCompleted = isCompleted,
    linkedElapsedMillis = linkedElapsedMillis,
    isTimerLinked = isTimerLinked,
    isDeleted = isDeleted,
    startedTimestamp = startedTimestamp
)

