package com.fanzatech.taskcreator

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskRepository private constructor(
    private val context: Context,
    private val database: TaskDatabase
) {
    private val dao = database.taskDao()

    val tasksFlow: Flow<List<Task>> = dao.observeAll().map { entities ->
        entities.map { it.toTask() }
    }

    suspend fun upsertTask(task: Task) {
        dao.upsert(task.toEntity())
    }

    suspend fun replaceAllTasks(tasks: List<Task>) {
        database.withTransaction {
            dao.deleteAll()
            dao.insertAll(tasks.map { it.toEntity() })
        }
    }

    suspend fun ensureLegacyImport() {
        val prefs = context.getSharedPreferences(IMPORT_PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_IMPORT_DONE, false)) return

        if (dao.count() == 0) {
            val legacyTasks = TaskStorage.loadTasks(context)
            if (legacyTasks.isNotEmpty()) {
                dao.insertAll(legacyTasks.map { it.toEntity() })
            }
        }

        prefs.edit().putBoolean(KEY_IMPORT_DONE, true).apply()
    }

    companion object {
        private const val IMPORT_PREFS = "task_creator_room"
        private const val KEY_IMPORT_DONE = "import_done"

        @Volatile
        private var INSTANCE: TaskRepository? = null

        fun getInstance(context: Context): TaskRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TaskRepository(
                    context.applicationContext,
                    TaskDatabase.getInstance(context.applicationContext)
                ).also { INSTANCE = it }
            }
        }
    }
}
