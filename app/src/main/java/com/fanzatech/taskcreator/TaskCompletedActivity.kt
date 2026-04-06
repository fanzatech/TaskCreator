package com.fanzatech.taskcreator

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletedTasksScreen(
    tasksList: androidx.compose.runtime.MutableState<List<Task>>,
    onTasksChanged: (List<Task>) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val completedTasks = tasksList.value.filter { it.isCompleted && !it.isDeleted }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val taskDetailLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val taskId = data?.getIntExtra(TaskDetailActivity.RESULT_TASK_ID, -1) ?: -1
            val updatedMillis = data?.getLongExtra(TaskDetailActivity.RESULT_LINKED_MILLIS, -1L) ?: -1L
            val markIncomplete = data?.getBooleanExtra(TaskDetailActivity.RESULT_MARK_INCOMPLETE, false) ?: false
            if (taskId >= 0) {
                val updatedTasks = tasksList.value.map { task ->
                    if (task.id != taskId) {
                        task
                    } else {
                        task.copy(
                            linkedElapsedMillis = if (updatedMillis >= 0L) updatedMillis else task.linkedElapsedMillis,
                            isCompleted = if (markIncomplete) false else task.isCompleted
                        )
                    }
                }
                onTasksChanged(updatedTasks)
            }
            if (markIncomplete) onNavigateBack()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Completed Tasks") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = modifier.padding(innerPadding).padding(16.dp)) {
            if (completedTasks.isEmpty()) {
                Text(
                    text = "No completed tasks yet!",
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                Text(
                    text = "Completed Tasks (${completedTasks.size})",
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Tap a task to view details",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Button(
                    onClick = {
                        val message = exportTasksToCsv(context, completedTasks)
                        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text("Export to CSV")
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(completedTasks) { task ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val detailIntent = Intent(context, TaskDetailActivity::class.java).apply {
                                        putExtra(TaskDetailActivity.EXTRA_TASK_ID, task.id)
                                        putExtra(TaskDetailActivity.EXTRA_TASK_NAME, task.name)
                                        putExtra(TaskDetailActivity.EXTRA_TASK_DESCRIPTION, task.description)
                                        putExtra(TaskDetailActivity.EXTRA_TASK_COMPLETED, task.isCompleted)
                                        putExtra(TaskDetailActivity.EXTRA_TASK_LINKED_MILLIS, task.linkedElapsedMillis)
                                        putExtra(TaskDetailActivity.EXTRA_TASK_TIMER_LINKED, task.isTimerLinked)
                                    }
                                    taskDetailLauncher.launch(detailIntent)
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = task.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                HorizontalDivider()
                                Text(
                                    text = task.description,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Time spent: ${formatElapsedCompleted(task.linkedElapsedMillis)}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun exportTasksToCsv(context: android.content.Context, tasks: List<Task>): String {
    return try {
        val fileName = "completed_tasks_${System.currentTimeMillis()}.csv"
        val csvContent = buildString {
            appendLine("ID,Name,Description,Completed,Time Spent (hh:mm:ss)")
            tasks.forEach { task ->
                val name = task.name.replace("\"", "\"\"")
                val description = task.description.replace("\"", "\"\"")
                appendLine("${task.id},\"$name\",\"$description\",${task.isCompleted},${formatElapsedCompleted(task.linkedElapsedMillis)}")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues
            )
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(csvContent.toByteArray())
                }
                "Exported to Downloads/$fileName"
            } ?: "Export failed: could not create file"
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = java.io.File(downloadsDir, fileName)
            file.writeText(csvContent)
            "Exported to Downloads/$fileName"
        }
    } catch (e: Exception) {
        "Export failed: ${e.message}"
    }
}

private fun formatElapsedCompleted(elapsedMillis: Long): String {
    val totalSeconds = elapsedMillis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
}
