package com.satwik.oodapplication.presentation.student

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.satwik.oodapplication.ui.components.GlassCard
import com.satwik.oodapplication.ui.components.SectionHeader
import com.satwik.oodapplication.ui.theme.*
import com.satwik.oodapplication.utils.Resource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StudentHomeScreen(
    studentName: String,
    studentYear: String,
    viewModel: StudentHomeViewModel = hiltViewModel()
) {
    val notificationState by viewModel.notifications.collectAsState()
    val isDark = isSystemInDarkTheme()
    
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(800)) + slideInVertically(initialOffsetY = { 30 })
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(18.dp))
            
            // Hero Greeting GlassCard
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                elevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Initial with Glowing Gradient
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = Color.Transparent,
                        border = BorderStroke(
                            2.dp,
                            Brush.sweepGradient(
                                listOf(PrimaryMain, SecondaryMain, AccentColor, PrimaryMain)
                            )
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(PrimaryMain, Color(0xFFFF7A45))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = studentName.firstOrNull()?.toString()?.uppercase() ?: "S",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(18.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Good day,", 
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDark) TextWhiteSecondary else TextDarkSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = studentName.ifEmpty { "Student" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = if (isDark) TextWhitePrimary else TextDarkPrimary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = SecondaryMain.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, SecondaryMain.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = studentYear, 
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SecondaryMain,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Surface(
                                color = SuccessGreen.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(6.dp).background(SuccessGreen, CircleShape))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Active", 
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SuccessGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            SectionHeader("Notice Board", "Official mess announcements & updates")
            
            Spacer(modifier = Modifier.height(6.dp))

            when (val resource = notificationState) {
                is Resource.Loading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp), 
                    contentAlignment = Alignment.Center
                ) { 
                    CircularProgressIndicator(strokeWidth = 3.dp, color = PrimaryMain) 
                }
                is Resource.Success -> {
                    val list = resource.data?.filter { (it.targetYear == "All") || (it.targetYear == studentYear) } ?: emptyList()
                    
                    if (list.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            list.asSequence().sortedByDescending { it.timestamp }.take(5).forEach { notification ->
                                AnnouncementGlassCard(notification)
                            }
                        }
                    } else {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(36.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = if (isDark) TextWhiteTertiary else TextDarkTertiary,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "No new announcements for today.", 
                                    style = MaterialTheme.typography.bodyMedium, 
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) TextWhiteSecondary else TextDarkSecondary
                                )
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        color = ErrorRed.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f))
                    ) {
                        Text(
                            "Announcements temporarily unavailable", 
                            color = ErrorRed,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun AnnouncementGlassCard(notification: com.satwik.oodapplication.data.model.AppNotification) {
    val isDark = isSystemInDarkTheme()
    val isHighPriority = notification.priority == "High"
    val accentColor = if (isHighPriority) ErrorRed else SecondaryMain

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = accentColor.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = if (isHighPriority) "⚠ URGENT" else "NOTICE",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = accentColor
                    )
                }
                
                Surface(
                    color = if (isDark) Color(0x30FFFFFF) else Color(0x18000000),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(notification.timestamp)),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) TextWhiteTertiary else TextDarkTertiary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            Text(
                text = notification.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = if (isDark) TextWhitePrimary else TextDarkPrimary
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = notification.body,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDark) TextWhiteSecondary else TextDarkSecondary,
                lineHeight = 20.sp
            )
        }
    }
}
