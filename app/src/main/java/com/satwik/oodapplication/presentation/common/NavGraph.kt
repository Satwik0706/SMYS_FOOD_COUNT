package com.satwik.oodapplication.presentation.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.satwik.oodapplication.presentation.admin.*
import com.satwik.oodapplication.presentation.auth.AuthViewModel
import com.satwik.oodapplication.presentation.auth.LoginScreen
import com.satwik.oodapplication.presentation.cook.CookDashboardScreen
import com.satwik.oodapplication.presentation.data_portal.DataPortalDashboardScreen
import com.satwik.oodapplication.presentation.data_portal.MainDataPortalDashboardScreen
import com.satwik.oodapplication.presentation.manager.ManagerDashboardScreen
import com.satwik.oodapplication.presentation.student.MainStudentScreen
import com.satwik.oodapplication.utils.Constants
import androidx.compose.animation.*
import androidx.compose.animation.core.tween

@Composable
fun SetupNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(animationSpec = tween(500)) + slideInHorizontally(initialOffsetX = { 400 }) },
        exitTransition = { fadeOut(animationSpec = tween(500)) + slideOutHorizontally(targetOffsetX = { -400 }) },
        popEnterTransition = { fadeIn(animationSpec = tween(500)) + slideInHorizontally(initialOffsetX = { -400 }) },
        popExitTransition = { fadeOut(animationSpec = tween(500)) + slideOutHorizontally(targetOffsetX = { 400 }) }
    ) {
        composable(Screen.Login.route) {
            val viewModel: AuthViewModel = hiltViewModel()
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = { role ->
                    val normalizedRole = role.trim().lowercase()
                    
                    // SECURITY HARDENING: Re-validate session and clear old state on navigation
                    val route = when {
                        normalizedRole == Constants.ROLE_MANAGER || normalizedRole.contains("manager") -> Screen.ManagerGraph.route
                        normalizedRole == Constants.ROLE_ADMIN || normalizedRole.contains("admin") -> Screen.AdminGraph.route
                        normalizedRole == Constants.ROLE_COOK || normalizedRole.contains("cook") || normalizedRole.contains("prakesh") -> Screen.CookGraph.route
                        normalizedRole == Constants.ROLE_DATA_ENTRY || normalizedRole.contains("data") -> Screen.DataEntryGraph.route
                        else -> Screen.StudentGraph.route
                    }
                    navController.navigate(route) {
                        // Kill the entire backstack to prevent "Back" button into other portals
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // Manager Graph
        navigation(
            startDestination = Screen.ManagerDashboard.route,
            route = Screen.ManagerGraph.route
        ) {
            composable(Screen.ManagerDashboard.route) {
                ManagerDashboardScreen(
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.ManagerGraph.route) { inclusive = true }
                        }
                    }
                )
            }
        }

        // Admin Graph
        navigation(
            startDestination = Screen.AdminDashboard.route,
            route = Screen.AdminGraph.route
        ) {
            composable(Screen.AdminDashboard.route) {
                val viewModel: AdminViewModel = hiltViewModel()
                AdminDashboardScreen(
                    viewModel = viewModel,
                    onNavigateToMenu = { navController.navigate(Screen.AdminMenuManagement.route) },
                    onNavigateToStudents = { navController.navigate(Screen.AdminStudentManagement.route) },
                    onNavigateToNotifications = { navController.navigate(Screen.AdminNotifications.route) },
                    onNavigateToFoodCount = { navController.navigate(Screen.AdminFoodCount.route) },
                    onNavigateToAttendance = { navController.navigate(Screen.AdminAttendance.route) },
                    onNavigateToSnack = { navController.navigate(Screen.AdminSnackManagement.route) },
                    onNavigateToRequests = { navController.navigate(Screen.AdminRequests.route) },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.AdminGraph.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.AdminMenuManagement.route) {
                val viewModel: MenuManagementViewModel = hiltViewModel()
                MenuManagementScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.AdminStudentManagement.route) {
                StudentManagementScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.AdminNotifications.route) {
                AdminNotificationsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.AdminFoodCount.route) {
                AdminFoodCountScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.AdminAttendance.route) {
                AdminAttendanceScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.AdminSnackManagement.route) {
                AdminSnackScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.AdminRequests.route) {
                val viewModel: AdminViewModel = hiltViewModel()
                AdminRequestsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // Student Graph
        navigation(
            startDestination = Screen.StudentDashboard.route,
            route = Screen.StudentGraph.route
        ) {
            composable(Screen.StudentDashboard.route) {
                val viewModel: AuthViewModel = hiltViewModel()
                val userSession by viewModel.userSession.collectAsState()
                
                if (userSession != null) {
                    MainStudentScreen(
                        user = userSession!!,
                        onLogout = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.StudentGraph.route) { inclusive = true }
                            }
                        }
                    )
                } else {
                    // Fallback or loading if session is lost
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }

        // Cook Graph (Prakesh Portal)
        navigation(
            startDestination = "cook_dashboard",
            route = Screen.CookGraph.route
        ) {
            composable("cook_dashboard") {
                CookDashboardScreen(
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.CookGraph.route) { inclusive = true }
                        }
                    }
                )
            }
        }

        // Data Portal Graph
        navigation(
            startDestination = "data_portal_dashboard",
            route = Screen.DataEntryGraph.route
        ) {
            composable("data_portal_dashboard") {
                val authViewModel: AuthViewModel = hiltViewModel()
                val userSession by authViewModel.userSession.collectAsState()

                if (userSession?.email == "maindata@smys.com") {
                    MainDataPortalDashboardScreen(
                        onLogout = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.DataEntryGraph.route) { inclusive = true }
                            }
                        }
                    )
                } else {
                    DataPortalDashboardScreen(
                        onLogout = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.DataEntryGraph.route) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}
