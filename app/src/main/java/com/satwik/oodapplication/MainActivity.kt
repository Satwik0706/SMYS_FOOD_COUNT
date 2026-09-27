package com.satwik.oodapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.rememberNavController
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.satwik.oodapplication.presentation.auth.AuthViewModel
import com.satwik.oodapplication.presentation.common.Screen
import com.satwik.oodapplication.presentation.common.SetupNavGraph
import com.satwik.oodapplication.ui.theme.OodapplicationTheme
import com.satwik.oodapplication.utils.Constants
import com.satwik.oodapplication.worker.FoodReminderWorker
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        askNotificationPermission()

        // Start daily 9:30 PM reminder
        FoodReminderWorker.scheduleNext(applicationContext)

        // Subscribe all users to the general announcements topic
        FirebaseMessaging.getInstance().subscribeToTopic("all_students")

        setContent {
            OodapplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authViewModel: AuthViewModel = hiltViewModel()
                    val isUpdateRequired by authViewModel.isUpdateRequired.collectAsState()
                    val updateUrl by authViewModel.updateUrl.collectAsState()
                    val userSession by authViewModel.userSession.collectAsState()
                    val navController = rememberNavController()
                    val context = LocalContext.current
                    
                    var showTermsDialog by remember {
                        mutableStateOf(
                            !context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                                .getBoolean(Constants.PREF_TERMS_ACCEPTED, false)
                        )
                    }

                    if (showTermsDialog) {
                        TermsAndConditionsDialog(
                            onAccept = {
                                context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                                    .edit()
                                    .putBoolean(Constants.PREF_TERMS_ACCEPTED, true)
                                    .apply()
                                showTermsDialog = false
                            }
                        )
                    } else if (isUpdateRequired) {
                        ForceUpdateScreen(updateUrl = updateUrl)
                    } else {
                        val startDestination = if (userSession != null) {
                            when (userSession?.role) {
                                Constants.ROLE_MANAGER -> Screen.ManagerGraph.route
                                Constants.ROLE_ADMIN -> Screen.AdminGraph.route
                                Constants.ROLE_STUDENT -> Screen.StudentGraph.route
                                Constants.ROLE_COOK -> Screen.CookGraph.route
                                Constants.ROLE_DATA_ENTRY -> Screen.DataEntryGraph.route
                                else -> Screen.Login.route
                            }
                        } else {
                            Screen.Login.route
                        }
                        
                        SetupNavGraph(
                            navController = navController,
                            startDestination = startDestination
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TermsAndConditionsDialog(onAccept: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = { /* Cannot dismiss without accepting */ },
        confirmButton = {
            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Agree and Continue", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(Constants.PRIVACY_POLICY_URL))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Read Full Privacy Policy", color = MaterialTheme.colorScheme.primary)
            }
        },
        title = { 
            Text(
                "Terms & Conditions", 
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleLarge
            ) 
        },
        text = {
            Column {
                Text(
                    "Welcome to SMYS Food Count Portal. To provide you with a seamless experience, we collect your food preferences and attendance data. This information is used exclusively for mess management and student identification.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "By clicking 'Agree and Continue', you confirm that you have read and understood our Privacy Policy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        },
        shape = RoundedCornerShape(28.dp),
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    )
}

@Composable
fun ForceUpdateScreen(updateUrl: String) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Update,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                stringResource(R.string.update_required),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                stringResource(R.string.update_message),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    if (updateUrl.isNotEmpty()) {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(updateUrl))
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(stringResource(R.string.update_now), fontWeight = FontWeight.Bold)
            }
        }
    }
}
