package com.satwik.oodapplication.presentation.student

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.satwik.oodapplication.data.model.MealInfo
import com.satwik.oodapplication.presentation.admin.MenuManagementViewModel
import com.satwik.oodapplication.ui.components.GlassCard
import com.satwik.oodapplication.ui.theme.*
import com.satwik.oodapplication.utils.Resource
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun StudentMenuScreen(
    viewModel: MenuManagementViewModel
) {
    val isDark = isSystemInDarkTheme()

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
    val menuState by viewModel.menuState.collectAsState()

    LaunchedEffect(currentDate) {
        viewModel.loadMenu(currentDate.toString())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Text(
            "Mess Menu", 
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = if (isDark) TextWhitePrimary else TextDarkPrimary,
            letterSpacing = (-0.5).sp
        )
        Text(
            "Delicious daily meals prepared with care",
            style = MaterialTheme.typography.bodySmall,
            color = if (isDark) TextWhiteTertiary else TextDarkSecondary,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(18.dp))

        // Glass Day Navigation
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            elevation = 4.dp
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
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft, 
                        contentDescription = "Previous Day",
                        tint = if (canGoBack) PrimaryMain else (if (isDark) TextWhiteTertiary.copy(alpha = 0.3f) else TextDarkTertiary.copy(alpha = 0.3f))
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (currentDate == LocalDate.now()) "Today" else currentDate.format(DateTimeFormatter.ofPattern("EEEE")),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = PrimaryMain
                    )
                    Text(
                        text = currentDate.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) TextWhitePrimary else TextDarkPrimary
                    )
                }

                IconButton(
                    onClick = { if (canGoForward) currentDate = currentDate.plusDays(1) },
                    enabled = canGoForward
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight, 
                        contentDescription = "Next Day",
                        tint = if (canGoForward) PrimaryMain else (if (isDark) TextWhiteTertiary.copy(alpha = 0.3f) else TextDarkTertiary.copy(alpha = 0.3f))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        when (val state = menuState) {
            is Resource.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(strokeWidth = 3.dp, color = PrimaryMain)
                }
            }
            is Resource.Success -> {
                val menu = state.data!!
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    contentPadding = PaddingValues(bottom = 28.dp)
                ) {
                    item { MenuMealCard("Breakfast", menu.breakfast, BreakfastColor, Icons.Default.BakeryDining) }
                    item { MenuMealCard("Lunch", menu.lunch, LunchColor, Icons.Default.Restaurant) }
                    item { MenuMealCard("Snacks", menu.snack, SnackColor, Icons.Default.Fastfood) }
                    item { MenuMealCard("Dinner", menu.dinner, DinnerColor, Icons.Default.DinnerDining) }
                }
            }
            is Resource.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No menu found for this date.", color = if (isDark) TextWhiteSecondary else TextDarkSecondary)
                }
            }
        }
    }
}

@Composable
fun MenuMealCard(name: String, info: MealInfo, accentColor: Color, icon: ImageVector) {
    val isDark = isSystemInDarkTheme()

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        elevation = 6.dp
    ) {
        Column {
            // Header area with accent color and subtle frosted sheen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                accentColor.copy(alpha = 0.22f),
                                accentColor.copy(alpha = 0.05f)
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = accentColor,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(42.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, null, modifier = Modifier.size(22.dp), tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        name, 
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) TextWhitePrimary else TextDarkPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(
                        color = accentColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
                    ) {
                        Text(
                            info.timing.ifBlank { "Schedule set by mess" },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = accentColor
                        )
                    }
                }
            }
            
            // Items area
            Column(modifier = Modifier.padding(20.dp)) {
                if (info.items.isEmpty() || (info.items.size == 1 && info.items[0].isBlank())) {
                    Text(
                        "No specific items listed for this meal today.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) TextWhiteTertiary else TextDarkTertiary
                    )
                } else {
                    info.items.forEach { item ->
                        if (item.isNotBlank()) {
                            Row(
                                modifier = Modifier.padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(8.dp),
                                    shape = CircleShape,
                                    color = accentColor,
                                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.5f))
                                ) {}
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    item.trim(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) TextWhitePrimary else TextDarkPrimary,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
