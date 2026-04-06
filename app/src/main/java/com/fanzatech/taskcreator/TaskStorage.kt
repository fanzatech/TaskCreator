package com.fanzatech.taskcreator

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object TaskStorage {
    private const val PREFS_NAME = "task_creator_prefs"
    private const val KEY_TASKS_JSON = "tasks_json"

    fun loadTasks(context: Context): List<Task> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val tasksJson = prefs.getString(KEY_TASKS_JSON, null) ?: return emptyList()

        return runCatching {
            val array = JSONArray(tasksJson)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    add(
                        Task(
                            id = item.optInt("id", index),
                            name = item.optString("name", ""),
                            description = item.optString("description", ""),
                            isCompleted = item.optBoolean("isCompleted", false),
                            linkedElapsedMillis = item.optLong("linkedElapsedMillis", 0L),
                            isTimerLinked = item.optBoolean("isTimerLinked", false),
                            isDeleted = item.optBoolean("isDeleted", false),
                            startedTimestamp = item.optLong("startedTimestamp", 0L)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveTasks(context: Context, tasks: List<Task>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val tasksArray = JSONArray()
        tasks.forEach { task ->
            tasksArray.put(
                JSONObject().apply {
                    put("id", task.id)
                    put("name", task.name)
                    put("description", task.description)
                    put("isCompleted", task.isCompleted)
                    put("linkedElapsedMillis", task.linkedElapsedMillis)
                    put("isTimerLinked", task.isTimerLinked)
                    put("isDeleted", task.isDeleted)
                    put("startedTimestamp", task.startedTimestamp)
                }
            )
        }

        // commit() helps ensure data is flushed before process death/crash.
        prefs.edit().putString(KEY_TASKS_JSON, tasksArray.toString()).commit()
    }
}

