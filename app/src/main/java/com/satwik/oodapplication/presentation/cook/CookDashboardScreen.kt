package com.satwik.oodapplication.presentation.cook

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.satwik.oodapplication.presentation.admin.*
import com.satwik.oodapplication.presentation.auth.AuthViewModel
import com.satwik.oodapplication.ui.theme.*
import com.satwik.oodapplication.utils.Resource
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookDashboardScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    onLogout: () -> Unit
) {
    val foodCountViewModel: AdminFoodCountViewModel = hiltViewModel()
    val menuViewModel: MenuManagementViewModel = hiltViewModel()
    
    var currentDate by remember { mutableStateOf(LocalDate.now()) }
    val dateStr = currentDate.toString()
    
    val reportResource by foodCountViewModel.report.collectAsState()
    val menuState by menuViewModel.menuState.collectAsState()
    var editingMeal by remember { mutableStateOf<MealData?>(null) }

    LaunchedEffect(currentDate) {
        foodCountViewModel.setDate(dateStr)
        menuViewModel.loadMenu(dateStr)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mess Operations", fontWeight = FontWeight.ExtraBold) },
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
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 0. Date Navigation
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val canGoBack = currentDate.isAfter(LocalDate.now())
                        val canGoForward = currentDate.isBefore(LocalDate.now().plusDays(6))

                        IconButton(
                            onClick = { if (canGoBack) currentDate = currentDate.minusDays(1) },
                            enabled = canGoBack,
                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Day")
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (currentDate == LocalDate.now()) "Today" else currentDate.format(DateTimeFormatter.ofPattern("EEEE")),
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
                            enabled = canGoForward,
                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Day")
                        }
                    }
                }
            }

            // 1. Food Count Totals Section (NEW)
            item {
                Text("Required Quantities", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(12.dp))
                
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
                                TotalCard("Night (D)", report.totalDinner, Color(0xFFFFE082), Modifier.weight(1f))
                            }
                            TotalCard("Students On Leave", report.totalOnLeave, MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f), Modifier.fillMaxWidth())
                        }
                    }
                    is Resource.Error -> Text("Error loading counts: ${resource.message}", color = MaterialTheme.colorScheme.error)
                }
            }

            item { HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)) }

            // 2. Menu Editing Section
            item {
                Text("Menu Configuration", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(12.dp))

                when (val state = menuState) {
                    is Resource.Loading -> Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    is Resource.Success -> {
                        val menu = state.data!!
                        val breakfastInfo = menu.breakfast.let { if (it.timing.isBlank()) it.copy(timing = "7:30 am to 11:00 am") else it }
                        val lunchInfo = menu.lunch.let { if (it.timing.isBlank()) it.copy(timing = "12:30 to 2:30 pm") else it }
                        val snackInfo = menu.snack.let { if (it.timing.isBlank()) it.copy(timing = "5:00 pm onwards") else it }
                        val dinnerInfo = menu.dinner.let { if (it.timing.isBlank()) it.copy(timing = "7:40 pm onwards") else it }

                        val meals = listOf(
                            MealData("Breakfast", breakfastInfo, BreakfastColor, Icons.Default.Edit),
                            MealData("Lunch", lunchInfo, LunchColor, Icons.Default.Edit),
                            MealData("Snack", snackInfo, SnackColor, Icons.Default.Edit),
                            MealData("Dinner", dinnerInfo, DinnerColor, Icons.Default.Edit)
                        )

                        // Using a Column with Rows since Grid is not easily used inside a scrollable LazyColumn
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                AdminMealCard(meals[0], Modifier.weight(1f)) { editingMeal = meals[0] }
                                AdminMealCard(meals[1], Modifier.weight(1f)) { editingMeal = meals[1] }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                AdminMealCard(meals[2], Modifier.weight(1f)) { editingMeal = meals[2] }
                                AdminMealCard(meals[3], Modifier.weight(1f)) { editingMeal = meals[3] }
                            }
                        }
                    }
                    is Resource.Error -> Text("Error loading menu")
                }
            }
        }

        editingMeal?.let { meal ->
            EditMealDialog(
                mealName = meal.name,
                currentInfo = meal.info,
                onDismiss = { editingMeal = null },
                onSave = { items, timing ->
                    menuViewModel.updateMeal(dateStr, meal.name, items, timing)
                    editingMeal = null
                }
            )
        }
    }
}

@Composable
fun AdminMealCard(meal: MealData, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedCard(
        modifier = modifier
            .height(180.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(
                color = meal.color.copy(alpha = 0.9f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(meal.name.take(1), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleSmall)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(meal.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            Text(
                meal.info.timing, 
                style = MaterialTheme.typography.labelSmall, 
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                if (meal.info.items.all { it.isBlank() }) "Not set" else meal.info.items.joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                lineHeight = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
