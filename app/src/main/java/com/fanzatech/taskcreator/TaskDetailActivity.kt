package com.fanzatech.taskcreator

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fanzatech.taskcreator.ui.theme.TaskCreatorTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class TaskDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
        val taskName = intent.getStringExtra(EXTRA_TASK_NAME).orEmpty()
        val taskDescription = intent.getStringExtra(EXTRA_TASK_DESCRIPTION).orEmpty()
        val isCompleted = intent.getBooleanExtra(EXTRA_TASK_COMPLETED, false)
        val linkedMillis = intent.getLongExtra(EXTRA_TASK_LINKED_MILLIS, 0L)
        val initialStartedTimestamp = intent.getLongExtra(EXTRA_TASK_STARTED_TIMESTAMP, 0L)
        val initialIsTimerRunning = intent.getBooleanExtra(EXTRA_TASK_TIMER_LINKED, false)

        setContent {
            TaskCreatorTheme {
                TaskDetailScreen(
                    taskName = taskName,
                    taskDescription = taskDescription,
                    isCompleted = isCompleted,
                    initialLinkedMillis = linkedMillis,
                    initialStartedTimestamp = initialStartedTimestamp,
                    initialIsTimerRunning = initialIsTimerRunning,
                    onBack = { latestElapsedMillis, startedTs, isRunning ->
                        setResult(
                            RESULT_OK,
                            Intent().apply {
                                putExtra(RESULT_TASK_ID, taskId)
                                putExtra(RESULT_LINKED_MILLIS, latestElapsedMillis)
                                putExtra(RESULT_STARTED_TIMESTAMP, startedTs)
                                putExtra(RESULT_TIMER_LINKED, isRunning)
                            }
                        )
                        finish()
                    },
                    onTimerChanged = { updatedMillis, startedTs, isRunning ->
                        setResult(
                            RESULT_OK,
                            Intent().apply {
                                putExtra(RESULT_TASK_ID, taskId)
                                putExtra(RESULT_LINKED_MILLIS, updatedMillis)
                                putExtra(RESULT_STARTED_TIMESTAMP, startedTs)
                                putExtra(RESULT_TIMER_LINKED, isRunning)
                            }
                        )
                    },
                    onMarkIncomplete = { latestElapsedMillis, startedTs, isRunning ->
                        setResult(
                            RESULT_OK,
                            Intent().apply {
                                putExtra(RESULT_TASK_ID, taskId)
                                putExtra(RESULT_LINKED_MILLIS, latestElapsedMillis)
                                putExtra(RESULT_STARTED_TIMESTAMP, startedTs)
                                putExtra(RESULT_TIMER_LINKED, isRunning)
                                putExtra(RESULT_MARK_INCOMPLETE, true)
                            }
                        )
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_NAME = "extra_task_name"
        const val EXTRA_TASK_DESCRIPTION = "extra_task_description"
        const val EXTRA_TASK_COMPLETED = "extra_task_completed"
        const val EXTRA_TASK_LINKED_MILLIS = "extra_task_linked_millis"
        const val EXTRA_TASK_TIMER_LINKED = "extra_task_timer_linked"
        const val EXTRA_TASK_STARTED_TIMESTAMP = "extra_task_started_timestamp"

        const val RESULT_TASK_ID = "result_task_id"
        const val RESULT_LINKED_MILLIS = "result_linked_millis"
        const val RESULT_MARK_INCOMPLETE = "result_mark_incomplete"
        const val RESULT_STARTED_TIMESTAMP = "result_started_timestamp"
        const val RESULT_TIMER_LINKED = "result_timer_linked"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskDetailScreen(
    taskName: String,
    taskDescription: String,
    isCompleted: Boolean,
    initialLinkedMillis: Long,
    initialStartedTimestamp: Long,
    initialIsTimerRunning: Boolean,
    onBack: (Long, Long, Boolean) -> Unit,
    onTimerChanged: (Long, Long, Boolean) -> Unit,
    onMarkIncomplete: (Long, Long, Boolean) -> Unit
) {
    var accumulatedMillis by rememberSaveable { mutableStateOf(initialLinkedMillis) }
    var runningStartTimestamp by rememberSaveable {
        mutableStateOf(if (initialIsTimerRunning) initialStartedTimestamp else 0L)
    }
    var startedTimestamp by rememberSaveable { mutableStateOf(initialStartedTimestamp) }
    var isTimerRunning by rememberSaveable { mutableStateOf(initialIsTimerRunning) }
    var elapsedMillis by rememberSaveable { mutableStateOf(initialLinkedMillis) }

    val latestElapsed = {
        if (isTimerRunning && runningStartTimestamp > 0L) {
            accumulatedMillis + (System.currentTimeMillis() - runningStartTimestamp)
        } else {
            accumulatedMillis
        }
    }

    LaunchedEffect(isTimerRunning, runningStartTimestamp, accumulatedMillis) {
        if (isTimerRunning && runningStartTimestamp > 0L) {
            while (isTimerRunning) {
                elapsedMillis = latestElapsed()
                delay(200)
            }
        } else {
            elapsedMillis = accumulatedMillis
        }
    }

    val checkpoint = {
        if (isTimerRunning && runningStartTimestamp > 0L) {
            val now = System.currentTimeMillis()
            val latest = accumulatedMillis + (now - runningStartTimestamp)
            Triple(latest, now, true)
        } else {
            Triple(accumulatedMillis, startedTimestamp, false)
        }
    }

    val handleBack = {
        val (latest, startedTs, isRunning) = checkpoint()
        onBack(latest, startedTs, isRunning)
    }

    BackHandler(onBack = handleBack)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Task Details") },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "Task Name", style = MaterialTheme.typography.labelMedium)
                Text(text = taskName, style = MaterialTheme.typography.titleLarge)
                HorizontalDivider()
                Text(text = "Description", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = if (taskDescription.isBlank()) "No description" else taskDescription,
                    style = MaterialTheme.typography.bodyLarge
                )
                HorizontalDivider()
                Text(text = "Completed: ${if (isCompleted) "Yes" else "No"}")
                Text(text = "Date & Time Started: ${formatDateTimeEastern(startedTimestamp)}")
                Text(text = "Total Time: ${formatElapsed(elapsedMillis)}")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val now = System.currentTimeMillis()
                            startedTimestamp = now
                            runningStartTimestamp = now
                            isTimerRunning = true
                            onTimerChanged(accumulatedMillis, runningStartTimestamp, true)
                        },
                        enabled = !isTimerRunning,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Start Task")
                    }
                    Button(
                        onClick = {
                            val latest = latestElapsed()
                            accumulatedMillis = latest
                            elapsedMillis = latest
                            runningStartTimestamp = 0L
                            isTimerRunning = false
                            onTimerChanged(latest, startedTimestamp, false)
                        },
                        enabled = isTimerRunning,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("End Task")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            accumulatedMillis = 0L
                            elapsedMillis = 0L
                            startedTimestamp = 0L
                            runningStartTimestamp = 0L
                            isTimerRunning = false
                            onTimerChanged(0L, 0L, false)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset Timer")
                    }
                    Button(
                        onClick = {
                            val (latest, startedTs, isRunning) = checkpoint()
                            onMarkIncomplete(latest, startedTs, isRunning)
                        },
                        enabled = isCompleted,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Mark Incomplete")
                    }
                }
            }
        }
    }
}

private fun formatElapsed(elapsedMillis: Long): String {
    val totalSeconds = elapsedMillis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
}

private fun formatDateTimeEastern(timestampMillis: Long): String {
    if (timestampMillis == 0L) return "Not started"
    val formatter = SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault())
    formatter.timeZone = TimeZone.getTimeZone("US/Eastern")
    return formatter.format(java.util.Date(timestampMillis))
}
