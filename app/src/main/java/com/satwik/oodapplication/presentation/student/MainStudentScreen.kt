package com.satwik.oodapplication.presentation.student

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.satwik.oodapplication.data.model.User
import com.satwik.oodapplication.presentation.admin.MenuManagementViewModel
import com.satwik.oodapplication.presentation.auth.AuthViewModel
import com.satwik.oodapplication.ui.components.GlassmorphicBackground
import com.satwik.oodapplication.ui.theme.*

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainStudentScreen(
    user: User,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val items = listOf("Home", "Menu", "Attendance", "Alerts")
    val routes = listOf("Home", "Menu", "Count", "Notifications")
    val icons = listOf(Icons.Default.Home, Icons.Default.ShoppingCart, Icons.Default.DateRange, Icons.Default.Notifications)
    var selectedItem by remember { mutableIntStateOf(0) }
    val isDark = isSystemInDarkTheme()

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
                                    shape = CircleShape,
                                    color = PrimaryMain,
                                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.4f))
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = user.name.firstOrNull()?.toString()?.uppercase() ?: "S",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "SMYS Portal", 
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-0.5).sp,
                                        color = if (isDark) TextWhitePrimary else TextDarkPrimary
                                    )
                                    Text(
                                        "Welcome back, ${user.name.split(" ").firstOrNull() ?: ""}",
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
            },
            bottomBar = {
                Surface(
                    color = if (isDark) Color(0xD90D1322) else Color(0xF2FFFFFF),
                    border = BorderStroke(
                        1.dp,
                        if (isDark) Color(0x30FFFFFF) else Color(0x2064748B)
                    )
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp
                    ) {
                        items.forEachIndexed { index, item ->
                            val isSelected = selectedItem == index
                            NavigationBarItem(
                                icon = { 
                                    Icon(
                                        icons[index], 
                                        contentDescription = item,
                                        tint = if (isSelected) PrimaryMain else if (isDark) TextWhiteTertiary else TextDarkTertiary,
                                        modifier = Modifier.size(22.dp)
                                    ) 
                                },
                                label = { 
                                    Text(
                                        item, 
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                        color = if (isSelected) PrimaryMain else if (isDark) TextWhiteTertiary else TextDarkTertiary
                                    ) 
                                },
                                selected = isSelected,
                                onClick = {
                                    if (selectedItem != index) {
                                        selectedItem = index
                                        navController.navigate(routes[index]) {
                                            popUpTo("Home") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = PrimaryMain.copy(alpha = 0.18f)
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "Home",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("Home") { StudentHomeScreen(user.name, user.year ?: "") }
                composable("Menu") { 
                    val viewModel: MenuManagementViewModel = hiltViewModel()
                    StudentMenuScreen(viewModel)
                }
                composable("Count") { 
                    val viewModel: StudentFoodCountViewModel = hiltViewModel()
                    StudentFoodCountScreen(viewModel, user)
                }
                composable("Notifications") { 
                    StudentNotificationsScreen(user.year ?: "")
                }
            }
        }
    }
}
