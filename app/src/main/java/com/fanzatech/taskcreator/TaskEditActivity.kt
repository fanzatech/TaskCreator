package com.fanzatech.taskcreator

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fanzatech.taskcreator.ui.theme.TaskCreatorTheme

class TaskEditActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
        val initialName = intent.getStringExtra(EXTRA_TASK_NAME).orEmpty()
        val initialDescription = intent.getStringExtra(EXTRA_TASK_DESCRIPTION).orEmpty()

        setContent {
            TaskCreatorTheme {
                TaskEditScreen(
                    initialName = initialName,
                    initialDescription = initialDescription,
                    onBack = { finish() },
                    onSave = { updatedName, updatedDescription ->
                        setResult(
                            Activity.RESULT_OK,
                            Intent().apply {
                                putExtra(RESULT_TASK_ID, taskId)
                                putExtra(RESULT_TASK_NAME, updatedName)
                                putExtra(RESULT_TASK_DESCRIPTION, updatedDescription)
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

        const val RESULT_TASK_ID = "result_task_id"
        const val RESULT_TASK_NAME = "result_task_name"
        const val RESULT_TASK_DESCRIPTION = "result_task_description"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskEditScreen(
    initialName: String,
    initialDescription: String,
    onBack: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var taskName by rememberSaveable { mutableStateOf(initialName) }
    var taskDescription by rememberSaveable { mutableStateOf(initialDescription) }

    val canSave = taskName.trim().isNotEmpty()

    BackHandler(onBack = onBack)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Edit Task") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextField(
                value = taskName,
                onValueChange = { taskName = it },
                label = { Text("Task name") },
                modifier = Modifier.fillMaxWidth()
            )
            TextField(
                value = taskDescription,
                onValueChange = { taskDescription = it },
                label = { Text("Task description") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onSave(taskName.trim(), taskDescription.trim()) },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}

