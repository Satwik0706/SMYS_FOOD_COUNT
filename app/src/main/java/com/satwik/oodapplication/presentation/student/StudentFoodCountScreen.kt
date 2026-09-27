package com.satwik.oodapplication.presentation.student

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TakeoutDining
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.satwik.oodapplication.presentation.common.components.shimmerModifier
import com.satwik.oodapplication.utils.vibrateSensible
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.satwik.oodapplication.data.model.User
import com.satwik.oodapplication.ui.components.GlassCard
import com.satwik.oodapplication.ui.components.SectionHeader
import com.satwik.oodapplication.ui.theme.*
import com.satwik.oodapplication.utils.Resource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StudentFoodCountScreen(
    viewModel: StudentFoodCountViewModel,
    user: User
) {
    val studentId = user.uid
    val foodCountState by viewModel.foodCountState.collectAsState()
    val lockStatusState by viewModel.lockStatus.collectAsState()
    val snackStatusState by viewModel.snackStatus.collectAsState()
    val studentRequest by viewModel.studentRequest.collectAsState()
    val adminWhatsApp by viewModel.adminWhatsApp.collectAsState()
    val requestActionState by viewModel.requestActionState.collectAsState()
    
    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }
    var showRequestDialog by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()

    LaunchedEffect(Unit) { 
        viewModel.loadData(studentId)
        visible = true 
    }

    LaunchedEffect(requestActionState) {
        if (requestActionState is Resource.Success) {
            viewModel.resetRequestState()
        }
    }

    val lockData = (lockStatusState as? Resource.Success)?.data
    val snackLockData = (snackStatusState as? Resource.Success)?.data
    val isMasterLocked = lockData?.locked ?: false
    
    val isAnyMainMealLocked = lockData?.let { 
        it.breakfastLocked || it.lunchLocked || it.dinnerLocked 
    } ?: false

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(800)) + slideInVertically(initialOffsetY = { 20 })
    ) {
        when (val state = foodCountState) {
            is Resource.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(44.dp).shimmerModifier())
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(110.dp).shimmerModifier())
                    Spacer(modifier = Modifier.height(32.dp))
                    repeat(4) {
                        Box(modifier = Modifier.fillMaxWidth().height(64.dp).padding(vertical = 4.dp).shimmerModifier())
                    }
                }
            }
            is Resource.Success -> {
                val data = state.data ?: return@AnimatedVisibility
                val canToggleLunchBox = !isMasterLocked && !data.isOnLeave && !(lockData?.breakfastLocked ?: false)
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    HeaderSection("Meal Attendance", "Date: ${data.date}")

                    Spacer(modifier = Modifier.height(20.dp))

                    if (isMasterLocked) {
                        StatusAlertCard(
                            message = "The submission window is currently closed.",
                            icon = Icons.Default.Lock,
                            accentColor = ErrorRed
                        )
                    } else if (data.isOnLeave) {
                        StatusAlertCard(
                            message = "You are currently ON LEAVE. All meals are locked.",
                            icon = Icons.Default.Info,
                            accentColor = ErrorRed
                        )
                    } else if (isAnyMainMealLocked) {
                        StatusAlertCard(
                            message = "Main meals are finalized. Changes are disabled.",
                            icon = Icons.Default.Info,
                            accentColor = WarningAmber
                        )
                    }

                    SectionHeader("Today's Schedule", "Select the meals you'll attend")
                    
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(26.dp),
                        elevation = 6.dp
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            MealToggleItem(
                                label = "Breakfast",
                                subtitle = "07:30 AM - 09:00 AM",
                                isSelected = data.isBreakfast,
                                color = BreakfastColor,
                                enabled = !isMasterLocked && !data.isOnLeave && !(lockData?.breakfastLocked ?: false),
                                locked = lockData?.breakfastLocked ?: false,
                                onToggle = { 
                                    context.vibrateSensible()
                                    viewModel.toggleMeal(studentId, data.date, "breakfast") 
                                }
                            )
                            
                            // Lunch Box Sub-Toggle
                            AnimatedVisibility(visible = data.isBreakfast) {
                                Surface(
                                    modifier = Modifier
                                        .padding(start = 12.dp, end = 12.dp, bottom = 8.dp, top = 2.dp)
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (data.isLunchBox) LunchColor.copy(alpha = 0.15f) else Color.Transparent,
                                    border = if (data.isLunchBox) BorderStroke(1.dp, LunchColor.copy(alpha = 0.4f)) else null,
                                    onClick = { 
                                        if (canToggleLunchBox) {
                                            context.vibrateSensible()
                                            viewModel.toggleMeal(studentId, data.date, "lunchbox")
                                        }
                                    },
                                    enabled = canToggleLunchBox
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                modifier = Modifier.size(32.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (data.isLunchBox) LunchColor else (if (isDark) Color(0x30FFFFFF) else Color(0x18000000))
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Default.TakeoutDining, 
                                                        contentDescription = null, 
                                                        modifier = Modifier.size(18.dp),
                                                        tint = if (data.isLunchBox) Color.White else (if (isDark) TextWhiteTertiary else TextDarkTertiary)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    "Lunch Box", 
                                                    style = MaterialTheme.typography.bodyMedium, 
                                                    fontWeight = FontWeight.Black,
                                                    color = if (isDark) TextWhitePrimary else TextDarkPrimary
                                                )
                                                Text(
                                                    "Carry-away midday meal", 
                                                    style = MaterialTheme.typography.labelSmall, 
                                                    color = if (isDark) TextWhiteTertiary else TextDarkSecondary
                                                )
                                            }
                                        }
                                        Switch(
                                            checked = data.isLunchBox,
                                            onCheckedChange = null,
                                            enabled = canToggleLunchBox,
                                            modifier = Modifier.scale(0.75f),
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = LunchColor,
                                                checkedTrackColor = LunchColor.copy(alpha = 0.35f)
                                            )
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp), 
                                thickness = 0.5.dp, 
                                color = if (isDark) Color(0x20FFFFFF) else Color(0x15000000)
                            )

                            MealToggleItem(
                                label = "Lunch",
                                subtitle = if (data.isLunchBox) "Lunch Box Active" else "12:30 PM - 02:00 PM",
                                isSelected = data.isLunch,
                                color = LunchColor,
                                enabled = !isMasterLocked && !data.isOnLeave && !(lockData?.lunchLocked ?: false) && !data.isLunchBox,
                                locked = lockData?.lunchLocked ?: false,
                                onToggle = { 
                                    context.vibrateSensible()
                                    viewModel.toggleMeal(studentId, data.date, "lunch") 
                                }
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp), 
                                thickness = 0.5.dp, 
                                color = if (isDark) Color(0x20FFFFFF) else Color(0x15000000)
                            )

                            MealToggleItem(
                                label = "Snacks",
                                subtitle = "04:30 PM - 05:30 PM",
                                isSelected = data.isSnack,
                                color = SnackColor,
                                enabled = !isMasterLocked && !data.isOnLeave && !(snackLockData?.locked ?: false),
                                locked = snackLockData?.locked ?: false,
                                onToggle = { 
                                    context.vibrateSensible()
                                    viewModel.toggleMeal(studentId, data.date, "snack") 
                                }
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp), 
                                thickness = 0.5.dp, 
                                color = if (isDark) Color(0x20FFFFFF) else Color(0x15000000)
                            )

                            MealToggleItem(
                                label = "Dinner",
                                subtitle = "07:30 PM - 09:00 PM",
                                isSelected = data.isDinner,
                                color = DinnerColor,
                                enabled = !isMasterLocked && !data.isOnLeave && !(lockData?.dinnerLocked ?: false),
                                locked = lockData?.dinnerLocked ?: false,
                                onToggle = { 
                                    context.vibrateSensible()
                                    viewModel.toggleMeal(studentId, data.date, "dinner") 
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    SectionHeader("Availability", "Mark your leave status")
                    
                    val requestData = (studentRequest as? Resource.Success)?.data
                    val hasPendingRequest = requestData?.status == "PENDING"
                    
                    AvailabilityCard(
                        title = "On Leave / Unavailable",
                        subtitle = when {
                            hasPendingRequest -> "LOCKED: Pending admin approval"
                            isAnyMainMealLocked -> "Locked: Meals are finalized"
                            else -> "Auto-resets today's meals to 0"
                        },
                        isSelected = data.isOnLeave,
                        enabled = !isMasterLocked && !isAnyMainMealLocked && !hasPendingRequest,
                        onToggle = { 
                            context.vibrateSensible()
                            viewModel.toggleMeal(studentId, data.date, "leave") 
                        },
                        color = ErrorRed
                    )
                    
                    // REQUEST PORTAL SECTION
                    if (data.isOnLeave && isAnyMainMealLocked && !isMasterLocked) {
                        Spacer(modifier = Modifier.height(26.dp))
                        SectionHeader("Missed Count?", "Request admin to mark you present")
                        
                        MissedCountRequestCard(
                            request = requestData,
                            onSendRequest = { showRequestDialog = true },
                            onWhatsAppRedirect = {
                                val number = (adminWhatsApp as? Resource.Success)?.data ?: ""
                                if (number.isNotEmpty()) {
                                    val text = "Hi Admin, I (${user.name}) missed my food count for today (${data.date}). I've sent a request in the app. Please approve it. Thanks!"
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                                    intent.data = android.net.Uri.parse("https://wa.me/$number?text=${android.net.Uri.encode(text)}")
                                    context.startActivity(intent)
                                }
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
            is Resource.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Connection issue. Please try again.", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showRequestDialog) {
        var b by remember { mutableStateOf(true) }
        var l by remember { mutableStateOf(true) }
        var d by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showRequestDialog = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Request Food Count", fontWeight = FontWeight.Black) },
            text = {
                Column {
                    Text("Select meals you want to attend today:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    MealCheckRow("Breakfast", b) { b = it }
                    MealCheckRow("Lunch", l) { l = it }
                    MealCheckRow("Dinner", d) { d = it }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Note: Admin must approve this request before your status changes.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.submitRequest(user, b, l, d)
                        showRequestDialog = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryMain),
                    enabled = b || l || d
                ) {
                    Text("Submit Request", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRequestDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MealCheckRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = FontWeight.SemiBold)
        Checkbox(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = PrimaryMain)
        )
    }
}

@Composable
fun MissedCountRequestCard(
    request: com.satwik.oodapplication.data.model.FoodRequest?,
    onSendRequest: () -> Unit,
    onWhatsAppRedirect: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            if (request == null) {
                Text(
                    "Forgot to mark yourself as Present?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = if (isDark) TextWhitePrimary else TextDarkPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Since the portal is locked, you can send a request to the admin for manual approval.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) TextWhiteSecondary else TextDarkSecondary
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onSendRequest,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryMain)
                ) {
                    Icon(Icons.Default.Update, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send Request to Admin", fontWeight = FontWeight.ExtraBold)
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val statusColor = when (request.status) {
                        "PENDING" -> WarningAmber
                        "APPROVED" -> SuccessGreen
                        else -> ErrorRed
                    }

                    Surface(
                        color = statusColor.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = request.status,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = statusColor
                        )
                    }
                    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                    Text(
                        "Sent at ${timeFormatter.format(Date(request.timestamp))}", 
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) TextWhiteTertiary else TextDarkTertiary,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = when (request.status) {
                        "PENDING" -> "Your request is waiting for admin approval. You can also message them on WhatsApp."
                        "APPROVED" -> "Admin approved your request! Your meal count has been updated."
                        else -> "Request rejected: ${request.adminNote ?: "No reason provided"}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDark) TextWhiteSecondary else TextDarkSecondary
                )

                if (request.status == "PENDING") {
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = onWhatsAppRedirect,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF25D366),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Notify Admin on WhatsApp", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderSection(title: String, subtitle: String) {
    val isDark = isSystemInDarkTheme()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = if (isDark) TextWhitePrimary else TextDarkPrimary,
                letterSpacing = (-0.5).sp
            )
            Text(
                "Daily Food Selection",
                style = MaterialTheme.typography.labelSmall,
                color = if (isDark) TextWhiteTertiary else TextDarkSecondary,
                fontWeight = FontWeight.Medium
            )
        }
        Surface(
            color = PrimaryMain.copy(alpha = 0.15f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, PrimaryMain.copy(alpha = 0.35f))
        ) {
            Text(
                subtitle,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = PrimaryMain
            )
        }
    }
}

@Composable
fun StatusAlertCard(
    message: String,
    icon: ImageVector,
    accentColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
        shape = RoundedCornerShape(18.dp),
        color = accentColor.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = accentColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = accentColor,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun AvailabilityCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    color: Color = ErrorRed
) {
    val isDark = isSystemInDarkTheme()

    GlassCard(
        onClick = { if (enabled) onToggle() },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        borderColor = if (isSelected) color.copy(alpha = 0.6f) else null,
        backgroundColor = if (isSelected) color.copy(alpha = 0.12f) else null
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) color else (if (isDark) Color(0x30FFFFFF) else Color(0x18000000))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Info, 
                            null, 
                            tint = if (isSelected) Color.White else (if (isDark) TextWhiteTertiary else TextDarkTertiary),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Black,
                        color = if (isSelected) color else (if (isDark) TextWhitePrimary else TextDarkPrimary)
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) TextWhiteTertiary else TextDarkSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Switch(
                checked = isSelected,
                onCheckedChange = null,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = color,
                    checkedTrackColor = color.copy(alpha = 0.35f)
                )
            )
        }
    }
}

@Composable
fun MealToggleItem(
    label: String,
    subtitle: String,
    isSelected: Boolean,
    color: Color,
    enabled: Boolean,
    locked: Boolean = false,
    onToggle: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    Surface(
        onClick = { if (enabled) onToggle() },
        color = Color.Transparent,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    color = if (locked) (if (isDark) Color(0x30FFFFFF) else Color(0x20000000))
                            else if (isSelected) color 
                            else color.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = if (isSelected && !locked) BorderStroke(1.dp, color.copy(alpha = 0.5f)) else null
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (locked) {
                            Icon(Icons.Default.Lock, null, modifier = Modifier.size(18.dp), tint = if (isDark) TextWhiteTertiary else TextDarkTertiary)
                        } else {
                            Text(
                                label.take(1), 
                                style = MaterialTheme.typography.titleMedium, 
                                fontWeight = FontWeight.Black,
                                color = if (isSelected) Color.White else color
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Black,
                        color = if (!enabled) (if (isDark) TextWhiteTertiary else TextDarkTertiary)
                                else if (isDark) TextWhitePrimary 
                                else TextDarkPrimary
                    )
                    Text(
                        if (locked) "Locked by Admin" else subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) TextWhiteTertiary else TextDarkSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Switch(
                checked = isSelected,
                onCheckedChange = null,
                enabled = enabled,
                modifier = Modifier.scale(0.85f),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = color,
                    checkedTrackColor = color.copy(alpha = 0.35f)
                ),
                thumbContent = if (isSelected) {
                    {
                        Icon(
                            imageVector = if (locked) Icons.Default.Lock else Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                            tint = Color.White
                        )
                    }
                } else null
            )
        }
    }
}
