package com.satwik.oodapplication.presentation.admin

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.satwik.oodapplication.data.model.User
import com.satwik.oodapplication.utils.Constants
import com.satwik.oodapplication.utils.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAttendanceScreen(
    viewModel: AdminAttendanceViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val students by viewModel.students.collectAsState()
    val attendanceMap by viewModel.attendanceMap.collectAsState()
    val foodCounts by viewModel.foodCounts.collectAsState()
    val conflicts by viewModel.conflicts.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var selectedBatch by remember { mutableStateOf("All") }
    var filterNotOnLeave by remember { mutableStateOf(false) }
    var filterDinnerOrdered by remember { mutableStateOf(false) }

    val batches = listOf("All") + students.mapNotNull { it.year }.distinct().sorted()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Daily Attendance", fontWeight = FontWeight.ExtraBold) },
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
                .padding(horizontal = 16.dp)
        ) {
            // Batch Filter Tabs
            ScrollableTabRow(
                selectedTabIndex = batches.indexOf(selectedBatch).coerceAtLeast(0),
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                batches.forEach { batch ->
                    Tab(
                        selected = selectedBatch == batch,
                        onClick = { selectedBatch = batch },
                        text = { Text(batch, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterNotOnLeave,
                    onClick = { filterNotOnLeave = !filterNotOnLeave },
                    label = { Text("Not on Leave", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = if (filterNotOnLeave) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
                FilterChip(
                    selected = filterDinnerOrdered,
                    onClick = { filterDinnerOrdered = !filterDinnerOrdered },
                    label = { Text("Dinner Ordered", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = if (filterDinnerOrdered) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Group students by year and sort alphabetically by name (A -> Z) inside each year group
            val groupedStudents = remember(students, selectedBatch, filterNotOnLeave, filterDinnerOrdered, foodCounts) {
                var filtered = if (selectedBatch == "All") students else students.filter { it.year == selectedBatch }

                if (filterNotOnLeave) {
                    filtered = filtered.filter { student ->
                        val count = foodCounts[student.uid]
                        val onLeave = count?.isOnLeave ?: student.isLeave
                        !onLeave
                    }
                }

                if (filterDinnerOrdered) {
                    filtered = filtered.filter { student ->
                        val count = foodCounts[student.uid]
                        if (count != null) {
                            !count.isOnLeave && count.isDinner
                        } else {
                            !student.isLeave && student.dinnerPref
                        }
                    }
                }

                filtered.groupBy { it.year ?: "Other" }
                    .toSortedMap(compareBy { yearName -> yearName })
                    .mapValues { (_, studentList) ->
                        studentList.sortedBy { it.name.lowercase() }
                    }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                groupedStudents.forEach { (year, studentList) ->
                    item(key = "header_$year") {
                        YearGroupHeader(yearName = year, count = studentList.size)
                    }
                    items(
                        items = studentList,
                        key = { it.uid }
                    ) { student ->
                        val status = attendanceMap[student.uid] ?: Constants.ATTENDANCE_PRESENT
                        val count = foodCounts[student.uid]
                        val isDinnerOrdered = if (count != null) (!count.isOnLeave && count.isDinner) else (!student.isLeave && student.dinnerPref)
                        val isOnLeave = count?.isOnLeave ?: student.isLeave

                        AttendanceCard(
                            student = student,
                            selectedStatus = status,
                            isDinnerOrdered = isDinnerOrdered,
                            isOnLeave = isOnLeave,
                            onStatusSelected = { newStatus ->
                                viewModel.setAttendanceStatus(student.uid, newStatus)
                            }
                        )
                    }
                }
            }

            Button(
                onClick = { viewModel.submitAttendance() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (uiState is Resource.Loading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text("Submit Attendance", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Food Conflict Report Dialog
    if (conflicts.isNotEmpty()) {
        val absentConflicts = conflicts.filter { it.status == Constants.ATTENDANCE_ABSENT }
        val permissionConflicts = conflicts.filter { it.status == Constants.ATTENDANCE_PERMISSION }
        val leaveConflicts = conflicts.filter { it.status == Constants.ATTENDANCE_LEAVE }

        AlertDialog(
            onDismissRequest = { viewModel.dismissConflicts() },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "Food Mismanagement Report",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                ) {
                    Text(
                        "The following students have dinner ordered but are marked with non-present status:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (absentConflicts.isNotEmpty()) {
                            item {
                                ConflictCategoryHeader(
                                    title = "Absent with Dinner Ordered",
                                    color = Color(0xFFC62828)
                                )
                            }
                            items(absentConflicts) { conflict ->
                                ConflictStudentRow(
                                    student = conflict.student,
                                    statusLabel = "Absent",
                                    color = Color(0xFFC62828)
                                )
                            }
                        }

                        if (permissionConflicts.isNotEmpty()) {
                            item {
                                ConflictCategoryHeader(
                                    title = "Permission with Dinner Ordered",
                                    color = Color(0xFF1565C0)
                                )
                            }
                            items(permissionConflicts) { conflict ->
                                ConflictStudentRow(
                                    student = conflict.student,
                                    statusLabel = "Permission",
                                    color = Color(0xFF1565C0)
                                )
                            }
                        }

                        if (leaveConflicts.isNotEmpty()) {
                            item {
                                ConflictCategoryHeader(
                                    title = "Leave with Dinner Ordered",
                                    color = Color(0xFFE65100)
                                )
                            }
                            items(leaveConflicts) { conflict ->
                                ConflictStudentRow(
                                    student = conflict.student,
                                    statusLabel = "Leave",
                                    color = Color(0xFFE65100)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.revertConflictsToPresent() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Revert Conflicts to Present", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = { viewModel.confirmAndSaveWithConflicts() }) {
                        Text("Confirm & Save As-Is", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { viewModel.dismissConflicts() }) {
                        Text("Review / Cancel")
                    }
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun YearGroupHeader(yearName: String, count: Int) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = yearName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Surface(
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                shape = CircleShape
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun AttendanceCard(
    student: User,
    selectedStatus: String,
    isDinnerOrdered: Boolean,
    isOnLeave: Boolean,
    onStatusSelected: (String) -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Side: Student Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Year tag / initials avatar
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = getYearAbbreviation(student.year),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Text(
                        text = student.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!student.rollNumber.isNullOrBlank()) {
                    Text(
                        text = "Roll: ${student.rollNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                // Indicators: Dinner ordered / Leave status
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    if (isDinnerOrdered) {
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFF2E7D32))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    Icons.Default.DinnerDining,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    "Dinner",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    if (isOnLeave) {
                        Surface(
                            color = Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFFE65100))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    Icons.Default.EventBusy,
                                    contentDescription = null,
                                    tint = Color(0xFFE65100),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    "On Leave",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right Side: Compact horizontal options (Present, Absent, Permission, Leave)
            AttendanceStatusSelector(
                selectedStatus = selectedStatus,
                onStatusSelected = onStatusSelected
            )
        }
    }
}

@Composable
fun AttendanceStatusSelector(
    selectedStatus: String,
    onStatusSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val statuses = listOf(
        Constants.ATTENDANCE_PRESENT to ("P" to Color(0xFF2E7D32)),
        Constants.ATTENDANCE_ABSENT to ("A" to Color(0xFFC62828)),
        Constants.ATTENDANCE_PERMISSION to ("Perm" to Color(0xFF1565C0)),
        Constants.ATTENDANCE_LEAVE to ("Leave" to Color(0xFFE65100))
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        statuses.forEach { (status, labelAndColor) ->
            val (label, color) = labelAndColor
            val isSelected = selectedStatus == status

            Surface(
                onClick = { onStatusSelected(status) },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                ),
                modifier = Modifier.height(34.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 7.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (isSelected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ConflictCategoryHeader(title: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ConflictStudentRow(student: User, statusLabel: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = student.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${student.year ?: "Unknown Year"} • Roll: ${student.rollNumber ?: "N/A"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Surface(
            color = color.copy(alpha = 0.15f),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

private fun getYearAbbreviation(year: String?): String {
    if (year.isNullOrBlank()) return "Y"
    return when {
        year.contains("1", ignoreCase = true) -> "Y1"
        year.contains("2", ignoreCase = true) -> "Y2"
        year.contains("3", ignoreCase = true) -> "Y3"
        year.contains("4", ignoreCase = true) -> "Y4"
        else -> year.take(3).uppercase()
    }
}
