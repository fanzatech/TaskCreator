package com.fanzatech.taskcreator

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fanzatech.taskcreator.ui.theme.TaskCreatorTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TaskCreatorTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current.applicationContext
    val repository = remember { TaskRepository.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()

    val currentScreen = remember { mutableStateOf<Screen>(Screen.MainScreen) }
    val tasksList = remember { mutableStateOf(listOf<Task>()) }
    val nextId = remember { mutableStateOf(0) }

    LaunchedEffect(repository) {
        repository.ensureLegacyImport()
        repository.tasksFlow.collect { loadedTasks ->
            tasksList.value = loadedTasks
            nextId.value = (loadedTasks.maxOfOrNull { it.id } ?: -1) + 1
        }
    }

    val onTasksChanged: (List<Task>) -> Unit = { updatedTasks ->
        val previousById = tasksList.value.associateBy { it.id }
        tasksList.value = updatedTasks
        nextId.value = (updatedTasks.maxOfOrNull { it.id } ?: -1) + 1
        coroutineScope.launch {
            updatedTasks.forEach { updatedTask ->
                val previousTask = previousById[updatedTask.id]
                if (previousTask == null || previousTask != updatedTask) {
                    repository.upsertTask(updatedTask)
                }
            }
        }
    }

    when (currentScreen.value) {
        Screen.MainScreen -> {
            TaskInputField(
                tasksList = tasksList,
                nextId = nextId,
                onTasksChanged = onTasksChanged,
                onNavigateToCompleted = { currentScreen.value = Screen.CompletedScreen },
                onNavigateToDeleted = { currentScreen.value = Screen.DeletedScreen }
            )
        }
        Screen.CompletedScreen -> {
            CompletedTasksScreen(
                tasksList = tasksList,
                onTasksChanged = onTasksChanged,
                onNavigateBack = { currentScreen.value = Screen.MainScreen }
            )
        }
        Screen.DeletedScreen -> {
            DeletedTasksScreen(
                tasksList = tasksList,
                onTasksChanged = onTasksChanged,
                onNavigateBack = { currentScreen.value = Screen.MainScreen }
            )
        }
    }
}

sealed class Screen {
    object MainScreen : Screen()
    object CompletedScreen : Screen()
    object DeletedScreen : Screen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskInputField(
    tasksList: androidx.compose.runtime.MutableState<List<Task>>,
    nextId: androidx.compose.runtime.MutableState<Int>,
    onTasksChanged: (List<Task>) -> Unit,
    onNavigateToCompleted: () -> Unit,
    onNavigateToDeleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val taskNameInput = remember { mutableStateOf("") }
    val taskDescriptionInput = remember { mutableStateOf("") }
    val searchQuery = remember { mutableStateOf("") }
    val context = LocalContext.current
    val nowMillisState = remember { mutableStateOf(System.currentTimeMillis()) }
    val taskDetailLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val taskId = data?.getIntExtra(TaskDetailActivity.RESULT_TASK_ID, -1) ?: -1
            val updatedMillis = data?.getLongExtra(TaskDetailActivity.RESULT_LINKED_MILLIS, -1L) ?: -1L
            val startedTimestamp = data?.getLongExtra(TaskDetailActivity.RESULT_STARTED_TIMESTAMP, -1L) ?: -1L
            val isTimerRunning = data?.getBooleanExtra(TaskDetailActivity.RESULT_TIMER_LINKED, false) ?: false
            val markIncomplete = data?.getBooleanExtra(TaskDetailActivity.RESULT_MARK_INCOMPLETE, false) ?: false
            if (taskId >= 0) {
                val updatedTasks = tasksList.value.map { task ->
                    if (task.id != taskId) {
                        task
                    } else {
                        task.copy(
                            linkedElapsedMillis = if (updatedMillis >= 0L) updatedMillis else task.linkedElapsedMillis,
                            startedTimestamp = if (startedTimestamp >= 0L) startedTimestamp else task.startedTimestamp,
                            isTimerLinked = isTimerRunning,
                            isCompleted = if (markIncomplete) false else task.isCompleted
                        )
                    }
                }
                onTasksChanged(updatedTasks)
            }
        }
    }

    val taskEditLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val taskId = data?.getIntExtra(TaskEditActivity.RESULT_TASK_ID, -1) ?: -1
            val updatedName = data?.getStringExtra(TaskEditActivity.RESULT_TASK_NAME).orEmpty()
            val updatedDescription = data?.getStringExtra(TaskEditActivity.RESULT_TASK_DESCRIPTION).orEmpty()
            if (taskId >= 0 && updatedName.isNotBlank()) {
                onTasksChanged(
                    tasksList.value.map { task ->
                        if (task.id == taskId) {
                            task.copy(name = updatedName, description = updatedDescription)
                        } else {
                            task
                        }
                    }
                )
            }
        }
    }

    val launchTaskDetail: (Task) -> Unit = { task ->
        val detailIntent = Intent(context, TaskDetailActivity::class.java).apply {
            putExtra(TaskDetailActivity.EXTRA_TASK_ID, task.id)
            putExtra(TaskDetailActivity.EXTRA_TASK_NAME, task.name)
            putExtra(TaskDetailActivity.EXTRA_TASK_DESCRIPTION, task.description)
            putExtra(TaskDetailActivity.EXTRA_TASK_COMPLETED, task.isCompleted)
            putExtra(TaskDetailActivity.EXTRA_TASK_LINKED_MILLIS, task.linkedElapsedMillis)
            putExtra(TaskDetailActivity.EXTRA_TASK_TIMER_LINKED, task.isTimerLinked)
            putExtra(TaskDetailActivity.EXTRA_TASK_STARTED_TIMESTAMP, task.startedTimestamp)
        }
        taskDetailLauncher.launch(detailIntent)
    }

    val launchTaskEdit: (Task) -> Unit = { task ->
        val editIntent = Intent(context, TaskEditActivity::class.java).apply {
            putExtra(TaskEditActivity.EXTRA_TASK_ID, task.id)
            putExtra(TaskEditActivity.EXTRA_TASK_NAME, task.name)
            putExtra(TaskEditActivity.EXTRA_TASK_DESCRIPTION, task.description)
        }
        taskEditLauncher.launch(editIntent)
    }

    val hasRunningTasks = tasksList.value.any {
        !it.isCompleted && !it.isDeleted && it.isTimerLinked && it.startedTimestamp > 0L
    }

    LaunchedEffect(hasRunningTasks) {
        if (hasRunningTasks) {
            while (true) {
                nowMillisState.value = System.currentTimeMillis()
                delay(1000)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Task Creator") }
            )
        }
    ) { innerPadding ->
        Column(modifier = modifier.padding(innerPadding).padding(16.dp)) {
            Text(
                text = "Create a Task",
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {

            }
            TextField(
                value = taskNameInput.value,
                onValueChange = {
                    taskNameInput.value = it
                    if (it.isEmpty()) searchQuery.value = ""
                },
                label = { Text("Enter task name") },
                trailingIcon = {
                    IconButton(onClick = { searchQuery.value = taskNameInput.value.trim() }) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search"
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            TextField(
                value = taskDescriptionInput.value,
                onValueChange = { taskDescriptionInput.value = it },
                label = { Text("Enter task details") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        // Handle task creation
                        val trimmedName = taskNameInput.value.trim()
                        if (trimmedName.isNotEmpty()) {
                            // Add task to list (description is optional)
                            val newTask = Task(
                                id = nextId.value,
                                name = trimmedName,
                                description = taskDescriptionInput.value.trim(),
                                isCompleted = false
                            )
                            onTasksChanged(listOf(newTask) + tasksList.value)
                            nextId.value++
                            taskNameInput.value = ""
                            taskDescriptionInput.value = ""
                        }
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Text("Add", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
                Button(
                    onClick = onNavigateToDeleted,
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Text("Delete", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
                Button(
                    onClick = onNavigateToCompleted,
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Text("Complete", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
            }

            // Display tasks
            if (tasksList.value.isNotEmpty()) {
                Text(
                    text = "Tasks:",
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                Text(
                    text = "Tap a task row to view details",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = "Press and hold a task row for more than 1 second to edit it",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = "Swipe right on a task row to mark it complete",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = "Swipe left on a task row to delete it",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                val activeTasks = tasksList.value
                    .filter { !it.isCompleted && !it.isDeleted }
                    .filter { task ->
                        searchQuery.value.isEmpty() ||
                        task.name.contains(searchQuery.value, ignoreCase = true)
                    }
                if (activeTasks.isNotEmpty()) {
                    val borderColor = MaterialTheme.colorScheme.outlineVariant
                    val headerColor = MaterialTheme.colorScheme.surfaceVariant
                    val swipeCompleteColor = Color(0xFF2E7D32).copy(alpha = 0.55f)
                    val swipeDeleteColor = Color(0xFFC62828).copy(alpha = 0.55f)
                    val editHoldColor = Color(0xFFEF6C00).copy(alpha = 0.55f)

                    // Table header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .background(headerColor)
                            .border(width = 1.dp, color = borderColor),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Task Name",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 8.dp)
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(borderColor)
                        )
                        Text(
                            text = "Time Spent",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .width(88.dp)
                                .padding(vertical = 8.dp)
                        )
                    }

                    // Table rows
                    LazyColumn(modifier = Modifier
                        .fillMaxWidth()
                        .border(width = 1.dp, color = borderColor)
                    ) {
                        items(activeTasks, key = { it.id }) { task ->
                            val swipeDistancePx = remember(task.id) { mutableStateOf(0f) }
                            val isEditHoldActive = remember(task.id) { mutableStateOf(false) }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min)
                                    .then(
                                        when {
                                            swipeDistancePx.value > 0f -> Modifier.background(swipeCompleteColor)
                                            swipeDistancePx.value < 0f -> Modifier.background(swipeDeleteColor)
                                            isEditHoldActive.value -> Modifier.background(editHoldColor)
                                            else -> Modifier
                                        }
                                    )
                                    .pointerInput(task.id) {
                                        detectHorizontalDragGestures(
                                            onHorizontalDrag = { change, dragAmount ->
                                                swipeDistancePx.value = (swipeDistancePx.value + dragAmount)
                                                    .coerceIn(-260f, 260f)
                                                change.consume()
                                            },
                                            onDragEnd = {
                                                when {
                                                    swipeDistancePx.value > 120f -> {
                                                        onTasksChanged(
                                                            tasksList.value.map { t ->
                                                                if (t.id == task.id) t.copy(isCompleted = true, isTimerLinked = false)
                                                                else t
                                                            }
                                                        )
                                                    }
                                                    swipeDistancePx.value < -120f -> {
                                                        onTasksChanged(
                                                            tasksList.value.map { t ->
                                                                if (t.id == task.id) t.copy(
                                                                    isDeleted = true,
                                                                    isCompleted = false,
                                                                    isTimerLinked = false
                                                                )
                                                                else t
                                                            }
                                                        )
                                                    }
                                                }
                                                swipeDistancePx.value = 0f
                                            },
                                            onDragCancel = {
                                                swipeDistancePx.value = 0f
                                            }
                                        )
                                    }
                                    .pointerInput(task.id) {
                                        detectTapGestures(
                                            onPress = {
                                                val pressStart = System.currentTimeMillis()
                                                isEditHoldActive.value = false

                                                var releasedBeforeThreshold: Boolean? = null
                                                val completedBeforeThreshold = withTimeoutOrNull(1_001L) {
                                                    releasedBeforeThreshold = tryAwaitRelease()
                                                    true
                                                } ?: false

                                                if (completedBeforeThreshold) {
                                                    isEditHoldActive.value = false
                                                    if (releasedBeforeThreshold == true) {
                                                        launchTaskDetail(task)
                                                    }
                                                    return@detectTapGestures
                                                }

                                                isEditHoldActive.value = true
                                                val releasedNormally = tryAwaitRelease()
                                                isEditHoldActive.value = false
                                                if (!releasedNormally) {
                                                    return@detectTapGestures
                                                }

                                                val heldDurationMillis = System.currentTimeMillis() - pressStart
                                                if (heldDurationMillis > 1_000L) {
                                                    launchTaskEdit(task)
                                                } else {
                                                    launchTaskDetail(task)
                                                }
                                            }
                                        )
                                    }
                            ) {
                                if (swipeDistancePx.value > 0f) {
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .padding(start = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Done,
                                            contentDescription = "Complete",
                                            tint = Color.White
                                        )
                                        Text(
                                            text = "Complete",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                if (swipeDistancePx.value < 0f) {
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .padding(end = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Delete",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Delete",
                                            tint = Color.White
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = task.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 8.dp, vertical = 10.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .fillMaxHeight()
                                            .background(borderColor)
                                    )
                                    Text(
                                        text = formatElapsed(getDisplayElapsedMillis(task, nowMillisState.value)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .width(88.dp)
                                            .padding(horizontal = 4.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = borderColor)
                        }
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun TaskInputFieldPreview() {
    TaskCreatorTheme {
        TaskInputField(
            tasksList = remember { mutableStateOf(listOf()) },
            nextId = remember { mutableStateOf(0) },
            onTasksChanged = {},
            onNavigateToCompleted = {},
            onNavigateToDeleted = {}
        )
    }
}

private fun formatElapsed(elapsedMillis: Long): String {
    val totalSeconds = elapsedMillis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
}

private fun getDisplayElapsedMillis(task: Task, nowMillis: Long): Long {
    if (!task.isTimerLinked || task.startedTimestamp <= 0L) {
        return task.linkedElapsedMillis
    }
    val runningDelta = (nowMillis - task.startedTimestamp).coerceAtLeast(0L)
    return task.linkedElapsedMillis + runningDelta
}
