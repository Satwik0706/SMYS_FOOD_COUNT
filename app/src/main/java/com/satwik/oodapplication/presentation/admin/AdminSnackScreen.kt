package com.satwik.oodapplication.presentation.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.satwik.oodapplication.utils.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSnackScreen(
    viewModel: AdminSnackViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val snackStatusResource by viewModel.snackStatus.collectAsState()
    val reportResource by viewModel.snackReport.collectAsState()

    // Locally remember the lock state to prevent "flickering" during network updates
    var localLockState by remember { mutableStateOf<Boolean?>(null) }
    
    // Sync local state with server data when it arrives
    LaunchedEffect(snackStatusResource) {
        if (snackStatusResource is Resource.Success) {
            localLockState = snackStatusResource.data?.locked
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Snack Management") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            // Lock Control Card
            val isLocked = localLockState ?: false

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isLocked) MaterialTheme.colorScheme.errorContainer 
                                    else MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Snack Submission Lock", 
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isLocked) "Students CANNOT submit snack counts" 
                                   else "Students CAN submit snack counts",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Switch(
                        checked = isLocked,
                        enabled = snackStatusResource !is Resource.Loading,
                        onCheckedChange = { 
                            val newState = !isLocked
                            localLockState = newState // Instant UI feedback
                            viewModel.toggleSnackLock() 
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("Batch-wise Snack Requirements", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            when (val resource = reportResource) {
                is Resource.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is Resource.Error -> {
                    Text("Error: ${resource.message}", color = MaterialTheme.colorScheme.error)
                }
                is Resource.Success -> {
                    val report = resource.data ?: emptyList()
                    var total = 0
                    report.forEach { total += it.count }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(report) { batch ->
                            SnackBatchRow(batch)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurface,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total Today", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.surface, style = MaterialTheme.typography.titleMedium)
                            Text(total.toString(), fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primaryContainer)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SnackBatchRow(batch: BatchSnackCount) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Fastfood, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.width(16.dp))
                Text("Batch ${batch.batchName}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    batch.count.toString(), 
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    fontWeight = FontWeight.Black, 
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
