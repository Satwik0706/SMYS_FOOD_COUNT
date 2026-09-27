package com.satwik.oodapplication.presentation.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.satwik.oodapplication.utils.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminNotificationsScreen(
    viewModel: NotificationViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val notificationState by viewModel.notifications.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingNotification by remember { mutableStateOf<com.satwik.oodapplication.data.model.AppNotification?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                editingNotification = null
                showDialog = true 
            }) {
                Text("Send")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).navigationBarsPadding().padding(16.dp)) {
            Text("Notifications History", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            when (val state = notificationState) {
                is Resource.Loading -> CircularProgressIndicator()
                is Resource.Error -> Text("Error: ${state.message}")
                is Resource.Success -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.data ?: emptyList()) { notification ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(notification.title, style = MaterialTheme.typography.titleMedium)
                                        Text(notification.body, style = MaterialTheme.typography.bodyMedium)
                                        Text("Target: ${notification.targetYear}", style = MaterialTheme.typography.labelSmall)
                                    }
                                    IconButton(onClick = { 
                                        editingNotification = notification
                                        showDialog = true
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                                    }
                                    IconButton(onClick = { viewModel.deleteNotification(notification) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showDialog) {
            NotificationDialog(
                initialNotification = editingNotification,
                onDismiss = { 
                    showDialog = false
                    editingNotification = null
                },
                onConfirm = { title, body, target, isPush ->
                    viewModel.sendNotification(title, body, target, isPush, editingNotification?.id)
                    showDialog = false
                    editingNotification = null
                }
            )
        }
    }
}

@Composable
fun NotificationDialog(
    initialNotification: com.satwik.oodapplication.data.model.AppNotification? = null,
    onDismiss: () -> Unit, 
    onConfirm: (String, String, String, Boolean) -> Unit
) {
    var title by remember { mutableStateOf(initialNotification?.title ?: "") }
    var body by remember { mutableStateOf(initialNotification?.body ?: "") }
    var target by remember { mutableStateOf(initialNotification?.targetYear ?: "All") }
    var isPush by remember { mutableStateOf(initialNotification?.isPush ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialNotification == null) "Send Notification" else "Edit Notification") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                TextField(value = body, onValueChange = { body = it }, label = { Text("Body") }, modifier = Modifier.fillMaxWidth())
                TextField(value = target, onValueChange = { target = it }, label = { Text("Target Year") }, modifier = Modifier.fillMaxWidth())
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Checkbox(checked = isPush, onCheckedChange = { isPush = it })
                    Text("Send as System Pop-up (Push)")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(title, body, target, isPush) }) { 
                Text(if (initialNotification == null) "Send" else "Update") 
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
