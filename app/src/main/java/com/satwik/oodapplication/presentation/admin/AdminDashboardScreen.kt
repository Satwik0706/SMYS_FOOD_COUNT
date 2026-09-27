package com.satwik.oodapplication.presentation.admin

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.satwik.oodapplication.presentation.auth.AuthViewModel
import com.satwik.oodapplication.data.model.User
import com.satwik.oodapplication.ui.components.GlassCard
import com.satwik.oodapplication.ui.components.GlassmorphicBackground
import com.satwik.oodapplication.ui.components.SectionHeader
import com.satwik.oodapplication.ui.theme.*
import com.satwik.oodapplication.utils.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: AdminViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onNavigateToMenu: () -> Unit,
    onNavigateToStudents: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToFoodCount: () -> Unit,
    onNavigateToAttendance: () -> Unit,
    onNavigateToSnack: () -> Unit,
    onNavigateToRequests: () -> Unit,
    onLogout: () -> Unit
) {
    val summaryResource by viewModel.summary.collectAsState()
    val lockStatus by viewModel.lockStatus.collectAsState()
    val allStudents by viewModel.allStudents.collectAsState()
    val isDark = isSystemInDarkTheme()
    
    var visible by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var resetType by remember { mutableStateOf("All") }
    var isChecked by remember { mutableStateOf(false) }
    var showVersions by remember { mutableStateOf(false) }
    var showWhatsAppSettings by remember { mutableStateOf(false) }
    var whatsAppNumber by remember { mutableStateOf("") }
    
    val adminWhatsAppResource by viewModel.adminWhatsApp.collectAsState()
    
    LaunchedEffect(adminWhatsAppResource) {
        if (adminWhatsAppResource is Resource.Success) {
            whatsAppNumber = (adminWhatsAppResource as Resource.Success).data ?: ""
        }
    }

    LaunchedEffect(Unit) { visible = true }

    GlassmorphicBackground(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Surface(
                    color = if (isDark) Color(0xB30A0E1A) else Color(0xCCFFFFFF),
                    border = BorderStroke(
                        0.5.dp, 
                        if (isDark) Color(0x20FFFFFF) else Color(0x15000000)
                    )
                ) {
                    TopAppBar(
                        title = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(38.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    color = PrimaryMain,
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.AdminPanelSettings,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "Admin Command", 
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-0.5).sp,
                                        color = if (isDark) TextWhitePrimary else TextDarkPrimary
                                    )
                                    Text(
                                        "System Overview & Mess Controls",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isDark) TextWhiteSecondary else TextDarkSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    authViewModel.logout()
                                    onLogout()
                                },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Surface(
                                    color = ErrorRed.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f)),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ExitToApp, 
                                            contentDescription = "Logout", 
                                            tint = ErrorRed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            }
        ) { padding ->
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(800)) + slideInVertically(initialOffsetY = { 30 })
            ) {
                when (val resource = summaryResource) {
                    is Resource.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(strokeWidth = 3.dp, color = PrimaryMain)
                        }
                    }
                    is Resource.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            GlassCard(modifier = Modifier.padding(24.dp)) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Error, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Connection Error", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                                    Text(resource.message ?: "Unknown error", style = MaterialTheme.typography.bodySmall, color = if (isDark) TextWhiteSecondary else TextDarkSecondary)
                                }
                            }
                        }
                    }
                    is Resource.Success -> {
                        val summary = resource.data ?: return@AnimatedVisibility
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .navigationBarsPadding()
                                .padding(horizontal = 20.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            // Today's Stats Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                StatMiniCard(
                                    label = "Enrolled",
                                    value = summary.totalStudents.toString(),
                                    icon = Icons.Default.Group,
                                    color = PrimaryMain,
                                    modifier = Modifier.weight(1f)
                                )
                                StatMiniCard(
                                    label = "Eating Today",
                                    value = summary.countSubmitted.toString(),
                                    icon = Icons.Default.Restaurant,
                                    color = SuccessGreen,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(26.dp))
                            
                            SectionHeader("System Controls", "Manage student portal access & automation")
                            
                            // Controls Glass Container
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(26.dp),
                                elevation = 6.dp
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    ControlRow(
                                        title = "Student Portal Lock",
                                        subtitle = if (lockStatus.locked) "Portal is currently CLOSED" else "Portal is currently OPEN",
                                        icon = if (lockStatus.locked) Icons.Default.Lock else Icons.Default.LockOpen,
                                        checked = lockStatus.locked,
                                        onCheckedChange = { viewModel.toggleLock() },
                                        activeColor = ErrorRed,
                                        inactiveColor = SuccessGreen
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 14.dp), 
                                        color = if (isDark) Color(0x20FFFFFF) else Color(0x15000000)
                                    )
                                    ControlRow(
                                        title = "Automation Engine",
                                        subtitle = if (lockStatus.automationEnabled) "Automatic scheduling ACTIVE" else "Manual override ACTIVE",
                                        icon = Icons.Default.SettingsSuggest,
                                        checked = lockStatus.automationEnabled,
                                        onCheckedChange = { viewModel.toggleAutomation() },
                                        activeColor = PrimaryMain,
                                        inactiveColor = if (isDark) TextWhiteTertiary else TextDarkTertiary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(26.dp))
                            
                            SectionHeader("Meal Management", "Lock individual meal slots")
                            
                            // Meal Locks Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val mealLocks = listOf(
                                    Triple("Breakfast", lockStatus.breakfastLocked, "breakfast"),
                                    Triple("Lunch", lockStatus.lunchLocked, "lunch"),
                                    Triple("Dinner", lockStatus.dinnerLocked, "dinner")
                                )
                                
                                mealLocks.forEach { (name, isLocked, type) ->
                                    MealLockChip(
                                        name = name,
                                        isLocked = isLocked,
                                        onClick = { viewModel.toggleMealLock(type) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(28.dp))
                            
                            SectionHeader("Management Hub", "Tools and analytics dashboard")
                            
                            // Primary Actions Grid
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    HubCard("Students", "Directory & Enroll", Icons.Default.People, PrimaryMain, Modifier.weight(1f), onClick = onNavigateToStudents)
                                    HubCard("Menu", "Meal Schedule", Icons.AutoMirrored.Filled.List, SecondaryMain, Modifier.weight(1f), onClick = onNavigateToMenu)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    HubCard("Food Count", "Kitchen Analytics", Icons.Default.BarChart, BreakfastColor, Modifier.weight(1f), onClick = onNavigateToFoodCount)
                                    HubCard("Attendance", "Daily Logs", Icons.Default.AssignmentTurnedIn, DinnerColor, Modifier.weight(1f), onClick = onNavigateToAttendance)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    HubCard("Requests", "Student Portal", Icons.Default.Update, WarningAmber, Modifier.weight(1f), onClick = onNavigateToRequests)
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                            
                            // Secondary Actions Glass Card
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                elevation = 4.dp
                            ) {
                                Column {
                                    ListItem(
                                        headlineContent = { Text("Snack Management", fontWeight = FontWeight.Bold) },
                                        supportingContent = { Text("Batch counts & snack locks", color = if (isDark) TextWhiteSecondary else TextDarkSecondary) },
                                        leadingContent = { 
                                            Surface(
                                                color = SnackColor.copy(alpha = 0.18f),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.Fastfood, null, tint = SnackColor, modifier = Modifier.size(20.dp))
                                                }
                                            }
                                        },
                                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = if (isDark) TextWhiteTertiary else TextDarkTertiary) },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                        modifier = Modifier.clickable { onNavigateToSnack() }
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = if (isDark) Color(0x20FFFFFF) else Color(0x15000000))
                                    ListItem(
                                        headlineContent = { Text("App Versions", fontWeight = FontWeight.Bold) },
                                        supportingContent = { Text("Check student install versions", color = if (isDark) TextWhiteSecondary else TextDarkSecondary) },
                                        leadingContent = { 
                                            Surface(
                                                color = SecondaryMain.copy(alpha = 0.18f),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.SystemUpdate, null, tint = SecondaryMain, modifier = Modifier.size(20.dp))
                                                }
                                            }
                                        },
                                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = if (isDark) TextWhiteTertiary else TextDarkTertiary) },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                        modifier = Modifier.clickable { showVersions = true }
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = if (isDark) Color(0x20FFFFFF) else Color(0x15000000))
                                    ListItem(
                                        headlineContent = { Text("WhatsApp Contact", fontWeight = FontWeight.Bold) },
                                        supportingContent = { Text("Update admin WhatsApp for students", color = if (isDark) TextWhiteSecondary else TextDarkSecondary) },
                                        leadingContent = { 
                                            Surface(
                                                color = SuccessGreen.copy(alpha = 0.18f),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.ContactPhone, null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                                                }
                                            }
                                        },
                                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = if (isDark) TextWhiteTertiary else TextDarkTertiary) },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                        modifier = Modifier.clickable { showWhatsAppSettings = true }
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = if (isDark) Color(0x20FFFFFF) else Color(0x15000000))
                                    val allLocksOff = viewModel.allLocksOff
                                    ListItem(
                                        headlineContent = { 
                                            Text(
                                                "System Reset", 
                                                fontWeight = FontWeight.Bold,
                                                color = if (allLocksOff) ErrorRed else (if (isDark) TextWhiteTertiary else TextDarkTertiary)
                                            ) 
                                        },
                                        supportingContent = { 
                                            Text(
                                                if (allLocksOff) "Force reset all daily counts" else "LOCKED: Unlock portal to reset",
                                                color = if (isDark) TextWhiteSecondary else TextDarkSecondary
                                            ) 
                                        },
                                        leadingContent = { 
                                            Surface(
                                                color = (if (allLocksOff) ErrorRed else Color.Gray).copy(alpha = 0.18f),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Default.Refresh, 
                                                        null, 
                                                        tint = if (allLocksOff) ErrorRed else (if (isDark) TextWhiteTertiary else TextDarkTertiary),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                        modifier = Modifier.clickable(enabled = allLocksOff) { showResetDialog = true }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Urgent Broadcast Gradient Button
                            Button(
                                onClick = onNavigateToNotifications,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(58.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues(0.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(PrimaryMain, Color(0xFFFF7A45))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            "Broadcast Urgent Alert", 
                                            style = MaterialTheme.typography.titleMedium, 
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }
            }

            if (showResetDialog) {
                val allLocksOff = viewModel.allLocksOff
                AlertDialog(
                    onDismissRequest = { 
                        showResetDialog = false 
                        isChecked = false
                        resetType = "All"
                    },
                    shape = RoundedCornerShape(28.dp),
                    title = { Text("Force Reset Meal Counts", fontWeight = FontWeight.Black) },
                    text = {
                        Column {
                            if (!allLocksOff) {
                                Text(
                                    "ERROR: All locks must be OFF to perform a reset. Please unlock the portal and all meals first.",
                                    color = ErrorRed,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            
                            Text("Choose which meal counts to reset to ZERO for students NOT on leave:", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(12.dp))

                            val options = listOf("All", "Breakfast", "Lunch", "Snack", "Dinner")
                            options.forEach { option ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { resetType = option },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = resetType == option, 
                                        onClick = { resetType = option },
                                        colors = RadioButtonDefaults.colors(selectedColor = PrimaryMain)
                                    )
                                    Text(option, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text("This action cannot be undone.", color = if (isDark) TextWhiteTertiary else TextDarkTertiary, style = MaterialTheme.typography.labelSmall)
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { isChecked = it },
                                    enabled = allLocksOff,
                                    colors = CheckboxDefaults.colors(checkedColor = PrimaryMain)
                                )
                                Text("Confirm selection reset", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (resetType == "All") {
                                    viewModel.resetAllCounts()
                                } else {
                                    viewModel.resetMeal(resetType)
                                }
                                showResetDialog = false
                                isChecked = false
                            },
                            enabled = isChecked && allLocksOff,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                        ) {
                            Text("Reset Now", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { 
                            showResetDialog = false 
                            isChecked = false
                        }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showVersions) {
                AlertDialog(
                    onDismissRequest = { showVersions = false },
                    shape = RoundedCornerShape(28.dp),
                    title = { Text("Student App Versions", fontWeight = FontWeight.Black) },
                    text = {
                        Box(modifier = Modifier.heightIn(max = 400.dp)) {
                            LazyColumn {
                                items(allStudents.sortedBy { it.name }) { student ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            student.name, 
                                            modifier = Modifier.weight(1f), 
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Surface(
                                            color = if (student.appVersion != null) PrimaryMain.copy(alpha = 0.16f) else (if (isDark) Color(0x30FFFFFF) else Color(0x18000000)),
                                            shape = RoundedCornerShape(8.dp),
                                            border = if (student.appVersion != null) BorderStroke(1.dp, PrimaryMain.copy(alpha = 0.35f)) else null
                                        ) {
                                            Text(
                                                text = student.appVersion ?: "N/A",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Black,
                                                color = if (student.appVersion != null) PrimaryMain else (if (isDark) TextWhiteTertiary else TextDarkTertiary)
                                            )
                                        }
                                    }
                                    HorizontalDivider(thickness = 0.5.dp, color = if (isDark) Color(0x15FFFFFF) else Color(0x10000000))
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showVersions = false }) { Text("Close", fontWeight = FontWeight.Bold) }
                    }
                )
            }

            if (showWhatsAppSettings) {
                AlertDialog(
                    onDismissRequest = { showWhatsAppSettings = false },
                    shape = RoundedCornerShape(28.dp),
                    title = { Text("Update Admin Contact", fontWeight = FontWeight.Black) },
                    text = {
                        Column {
                            Text("Enter the WhatsApp number for student redirection (include country code):", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = whatsAppNumber,
                                onValueChange = { whatsAppNumber = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("e.g. 919876543210") },
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryMain,
                                    focusedLabelColor = PrimaryMain
                                )
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.updateWhatsAppNumber(whatsAppNumber)
                                showWhatsAppSettings = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryMain)
                        ) {
                            Text("Save Number", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showWhatsAppSettings = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun StatMiniCard(
    label: String, 
    value: String, 
    icon: ImageVector, 
    color: Color,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        elevation = 6.dp,
        borderColor = color.copy(alpha = 0.35f),
        backgroundColor = color.copy(alpha = 0.08f)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = color.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    label, 
                    style = MaterialTheme.typography.labelMedium, 
                    fontWeight = FontWeight.Black, 
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                value, 
                style = MaterialTheme.typography.headlineLarge, 
                fontWeight = FontWeight.Black, 
                color = if (isDark) TextWhitePrimary else TextDarkPrimary,
                letterSpacing = (-0.5).sp
            )
        }
    }
}

@Composable
fun ControlRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    activeColor: Color,
    inactiveColor: Color = Color.Gray
) {
    val isDark = isSystemInDarkTheme()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = if (checked) activeColor.copy(alpha = 0.18f) else (if (isDark) Color(0x30FFFFFF) else Color(0x18000000)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(42.dp),
            border = if (checked) BorderStroke(1.dp, activeColor.copy(alpha = 0.4f)) else null
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon, 
                    null, 
                    tint = if (checked) activeColor else (if (isDark) TextWhiteTertiary else TextDarkTertiary),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title, 
                fontWeight = FontWeight.Black, 
                style = MaterialTheme.typography.bodyLarge,
                color = if (isDark) TextWhitePrimary else TextDarkPrimary
            )
            Text(
                subtitle, 
                style = MaterialTheme.typography.labelSmall, 
                color = if (checked) activeColor else (if (isDark) TextWhiteTertiary else TextDarkSecondary),
                fontWeight = FontWeight.SemiBold
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = activeColor,
                checkedTrackColor = activeColor.copy(alpha = 0.35f)
            )
        )
    }
}

@Composable
fun MealLockChip(
    name: String,
    isLocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val statusColor = if (isLocked) ErrorRed else SuccessGreen
    
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(16.dp),
        color = statusColor.copy(alpha = if (isLocked) 0.18f else 0.12f),
        border = BorderStroke(
            1.dp, 
            statusColor.copy(alpha = if (isLocked) 0.5f else 0.3f)
        )
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    null,
                    modifier = Modifier.size(14.dp),
                    tint = statusColor
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    name, 
                    style = MaterialTheme.typography.labelMedium, 
                    fontWeight = FontWeight.Black, 
                    color = statusColor
                )
            }
        }
    }
}

@Composable
fun HubCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    GlassCard(
        onClick = onClick,
        modifier = modifier.height(115.dp),
        shape = RoundedCornerShape(22.dp),
        elevation = 4.dp,
        backgroundColor = accentColor.copy(alpha = 0.08f),
        borderColor = accentColor.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                color = accentColor.copy(alpha = 0.22f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(38.dp),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, modifier = Modifier.size(20.dp), tint = accentColor)
                }
            }
            Column {
                Text(
                    title, 
                    fontWeight = FontWeight.Black, 
                    style = MaterialTheme.typography.bodyLarge, 
                    color = if (isDark) TextWhitePrimary else TextDarkPrimary
                )
                Text(
                    subtitle, 
                    style = MaterialTheme.typography.labelSmall, 
                    color = if (isDark) TextWhiteTertiary else TextDarkSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
