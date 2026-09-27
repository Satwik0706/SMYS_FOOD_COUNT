package com.satwik.oodapplication.presentation.student

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.satwik.oodapplication.utils.Resource

@Composable
fun StudentNotificationsScreen(
    studentYear: String,
    viewModel: StudentHomeViewModel = hiltViewModel()
) {
    val notificationState by viewModel.notifications.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        SectionHeader("Notifications", "Mess announcements and alerts")
        Spacer(modifier = Modifier.height(8.dp))

        when (val state = notificationState) {
            is Resource.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeWidth = 3.dp)
            }
            is Resource.Error -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
            }
            is Resource.Success -> {
                val list = state.data?.filter { it.targetYear == "All" || it.targetYear == studentYear } ?: emptyList()
                
                if (list.isNotEmpty()) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(list.sortedByDescending { it.timestamp }) { notification ->
                            AnnouncementCard(notification)
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No notifications yet.", color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}
