package com.fanzatech.taskcreator

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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeletedTasksScreen(
    tasksList: androidx.compose.runtime.MutableState<List<Task>>,
    onTasksChanged: (List<Task>) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val deletedTasks = tasksList.value.filter { it.isDeleted }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Deleted Tasks") },
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
            if (deletedTasks.isEmpty()) {
                Text(
                    text = "No deleted tasks.",
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                Text(
                    text = "Deleted Tasks (${deletedTasks.size})",
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Tasks marked with Delete Task appear here",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(deletedTasks) { task ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                Button(
                                    onClick = {
                                        onTasksChanged(
                                            tasksList.value.map { current ->
                                                if (current.id == task.id) current.copy(isDeleted = false) else current
                                            }
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Undo Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
