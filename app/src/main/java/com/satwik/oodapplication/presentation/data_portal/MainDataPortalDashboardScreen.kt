package com.satwik.oodapplication.presentation.data_portal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val DATE_SWITCH_TIME = LocalTime.of(19, 40) // 7:40 PM

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDataPortalDashboardScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    onLogout: () -> Unit
) {
    val foodCountViewModel: AdminFoodCountViewModel = hiltViewModel()
    val menuViewModel: MenuManagementViewModel = hiltViewModel()
    
    val initialDate = remember {
        val now = LocalTime.now()
        if (now.isAfter(DATE_SWITCH_TIME)) {
            LocalDate.now().plusDays(1)
        } else {
            LocalDate.now()
        }
    }
    
    var currentDate by remember { mutableStateOf(initialDate) }
    val reportResource by foodCountViewModel.report.collectAsState()
    val menuResource by menuViewModel.menuState.collectAsState()
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentView by remember { mutableStateOf("Dashboard") }

    LaunchedEffect(currentDate) {
        foodCountViewModel.setDate(currentDate.toString())
        menuViewModel.loadMenu(currentDate.toString())
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Main Data Portal", 
                    modifier = Modifier.padding(16.dp), 
                    style = MaterialTheme.typography.titleLarge, 
                    fontWeight = FontWeight.Black
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                NavigationDrawerItem(
                    label = { Text("Main Dashboard", fontWeight = FontWeight.Bold) },
                    selected = currentView == "Dashboard",
                    onClick = { 
                        currentView = "Dashboard"
                        scope.launch { drawerState.close() } 
                    },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                
                NavigationDrawerItem(
                    label = { Text("Leave Section", fontWeight = FontWeight.Bold) },
                    selected = currentView == "Leave",
                    onClick = { 
                        currentView = "Leave"
                        scope.launch { drawerState.close() } 
                    },
                    icon = { Icon(Icons.Default.PersonOff, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                NavigationDrawerItem(
                    label = { Text("Logout", color = MaterialTheme.colorScheme.error) },
                    selected = false,
                    onClick = {
                        authViewModel.logout()
                        onLogout()
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            if (currentView == "Dashboard") "Data Analytics" else "Leave Tracker", 
                            style = MaterialTheme.typography.titleLarge, 
                            fontWeight = FontWeight.ExtraBold
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).navigationBarsPadding().fillMaxSize()) {
                if (currentView == "Dashboard") {
                    DashboardView(currentDate, reportResource, menuResource, onDateChange = { currentDate = it })
                } else {
                    LeaveTrackerView(currentDate, reportResource, onDateChange = { currentDate = it })
                }
            }
        }
    }
}

@Composable
fun DateNavigationCard(
    currentDate: LocalDate,
    onDateChange: (LocalDate) -> Unit
) {
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
                onClick = { if (canGoBack) onDateChange(currentDate.minusDays(1)) },
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
                onClick = { if (canGoForward) onDateChange(currentDate.plusDays(1)) },
                enabled = canGoForward
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }
    }
}

@Composable
fun DashboardView(
    currentDate: LocalDate,
    reportResource: Resource<com.satwik.oodapplication.presentation.admin.FoodCountReport>,
    menuResource: Resource<com.satwik.oodapplication.data.model.Menu>,
    onDateChange: (LocalDate) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Date Navigation
        item {
            DateNavigationCard(currentDate, onDateChange)
        }

        // Food Count Totals
        item {
            Text("Food Requirements", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
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
                        TotalCard("Students On Leave", report.totalOnLeave, MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f), Modifier.fillMaxWidth())
                    }
                }
                is Resource.Error -> Text("Error loading counts: ${resource.message}", color = MaterialTheme.colorScheme.error)
            }
        }

        item { HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)) }

        // Menu View
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

@Composable
fun LeaveTrackerView(
    currentDate: LocalDate,
    reportResource: Resource<com.satwik.oodapplication.presentation.admin.FoodCountReport>,
    onDateChange: (LocalDate) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(8.dp))
        DateNavigationCard(currentDate, onDateChange)
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Leave Tracker", 
                style = MaterialTheme.typography.headlineSmall, 
                fontWeight = FontWeight.Black
            )
            
            if (reportResource is Resource.Success) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "${reportResource.data?.totalOnLeave ?: 0} Total",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        when (val resource = reportResource) {
            is Resource.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is Resource.Success -> {
                val report = resource.data!!
                val leaveBatches = report.batchBreakdown.filter { it.onLeave > 0 }

                if (leaveBatches.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PersonOff, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No students are currently on leave.", color = MaterialTheme.colorScheme.outline)
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(leaveBatches) { batch ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Batch ${batch.batchName}", 
                                            fontWeight = FontWeight.ExtraBold, 
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "${batch.onLeave} Students",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    val studentsOnLeave = batch.students.filter { it.isLeave }
                                    studentsOnLeave.forEach { student ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                modifier = Modifier.size(8.dp),
                                                color = MaterialTheme.colorScheme.error,
                                                shape = RoundedCornerShape(2.dp)
                                            ) {}
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                student.name, 
                                                fontWeight = FontWeight.SemiBold, 
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            is Resource.Error -> Text("Error: ${resource.message}", color = MaterialTheme.colorScheme.error)
        }
    }
}
