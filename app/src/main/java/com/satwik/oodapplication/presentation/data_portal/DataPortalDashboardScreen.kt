package com.satwik.oodapplication.presentation.data_portal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.satwik.oodapplication.presentation.admin.AdminFoodCountViewModel
import com.satwik.oodapplication.presentation.admin.MenuManagementViewModel
import com.satwik.oodapplication.presentation.admin.TotalCard
import com.satwik.oodapplication.presentation.auth.AuthViewModel
import com.satwik.oodapplication.presentation.student.MenuMealCard
import com.satwik.oodapplication.ui.theme.BreakfastColor
import com.satwik.oodapplication.ui.theme.DinnerColor
import com.satwik.oodapplication.ui.theme.LunchColor
import com.satwik.oodapplication.ui.theme.SnackColor
import com.satwik.oodapplication.utils.Resource
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataPortalDashboardScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    onLogout: () -> Unit
) {
    val foodCountViewModel: AdminFoodCountViewModel = hiltViewModel()
    val menuViewModel: MenuManagementViewModel = hiltViewModel()
    
    // Daily at 7:40 PM, default view switches to Tomorrow
    val initialDate = remember {
        val now = LocalTime.now()
        if (now.isAfter(LocalTime.of(19, 40))) {
            LocalDate.now().plusDays(1)
        } else {
            LocalDate.now()
        }
    }
    
    var currentDate by remember { mutableStateOf(initialDate) }
    val reportResource by foodCountViewModel.report.collectAsState()
    val menuResource by menuViewModel.menuState.collectAsState()

    LaunchedEffect(currentDate) {
        foodCountViewModel.setDate(currentDate.toString())
        menuViewModel.loadMenu(currentDate.toString())
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Data Analytics Portal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        authViewModel.logout()
                        onLogout()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 0. Date Navigation (Added as requested)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val canGoBack = currentDate.isAfter(LocalDate.now().minusDays(5))
                        val canGoForward = currentDate.isBefore(LocalDate.now().plusDays(1))

                        IconButton(
                            onClick = { if (canGoBack) currentDate = currentDate.minusDays(1) },
                            enabled = canGoBack
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val label = when (currentDate) {
                                LocalDate.now() -> "Today"
                                LocalDate.now().plusDays(1) -> "Tomorrow"
                                LocalDate.now().minusDays(1) -> "Yesterday"
                                else -> currentDate.format(DateTimeFormatter.ofPattern("EEEE"))
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = currentDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        IconButton(
                            onClick = { if (canGoForward) currentDate = currentDate.plusDays(1) },
                            enabled = canGoForward
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                        }
                    }
                }
            }

            // 1. Food Count Totals Section
            item {
                Text("Total Food Requirements", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(16.dp))
                
                when (val resource = reportResource) {
                    is Resource.Loading -> Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    is Resource.Success -> {
                        val report = resource.data!!
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TotalCard("Morning (B)", report.totalBreakfast, MaterialTheme.colorScheme.primaryContainer, Modifier.weight(1f))
                                TotalCard("Afternoon (L)", report.totalLunch, MaterialTheme.colorScheme.secondaryContainer, Modifier.weight(1f))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TotalCard("Evening (S)", report.totalSnack, MaterialTheme.colorScheme.tertiaryContainer, Modifier.weight(1f))
                                TotalCard("Night (D)", report.totalDinner, DinnerColor, Modifier.weight(1f))
                            }
                            TotalCard("Students On Leave / Out", report.totalOnLeave, MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f), Modifier.fillMaxWidth())
                        }
                    }
                    is Resource.Error -> Text("Error loading counts: ${resource.message}", color = MaterialTheme.colorScheme.error)
                }
            }

            item { HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)) }

            // 2. Menu View Section
            item {
                Text("Mess Menu View", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(16.dp))

                when (val state = menuResource) {
                    is Resource.Loading -> Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    is Resource.Success -> {
                        val menu = state.data!!
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            MenuMealCard("Breakfast", menu.breakfast, BreakfastColor, Icons.Default.BakeryDining)
                            MenuMealCard("Lunch", menu.lunch, LunchColor, Icons.Default.Restaurant)
                            MenuMealCard("Snacks", menu.snack, SnackColor, Icons.Default.Fastfood)
                            MenuMealCard("Dinner", menu.dinner, DinnerColor, Icons.Default.DinnerDining)
                        }
                    }
                    is Resource.Error -> Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Text("No menu found for this date.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}
