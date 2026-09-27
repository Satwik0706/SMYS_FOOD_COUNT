package com.satwik.oodapplication.presentation.common

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object ManagerDashboard : Screen("manager_dashboard")
    object AdminDashboard : Screen("admin_dashboard")
    object StudentDashboard : Screen("student_dashboard")
    
    // Nested Graphs
    object ManagerGraph : Screen("manager_graph")
    object AdminGraph : Screen("admin_graph")
    object StudentGraph : Screen("student_graph")
    object CookGraph : Screen("cook_graph")
    object DataEntryGraph : Screen("data_entry_graph")
    // Admin Specific
    object AdminMenuManagement : Screen("admin_menu_mgmt")
    object AdminStudentManagement : Screen("admin_student_mgmt")
    object AdminNotifications : Screen("admin_notifications")
    object AdminFoodCount : Screen("admin_food_count")
    object AdminAttendance : Screen("admin_attendance")
    object AdminSnackManagement : Screen("admin_snack_mgmt")
    object AdminRequests : Screen("admin_requests")
}
