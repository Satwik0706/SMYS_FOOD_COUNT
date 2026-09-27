package com.satwik.oodapplication.presentation.auth

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.satwik.oodapplication.R
import com.satwik.oodapplication.ui.components.GlassCard
import com.satwik.oodapplication.ui.components.GlassmorphicBackground
import com.satwik.oodapplication.ui.theme.*
import com.satwik.oodapplication.utils.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: (String) -> Unit
) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()
    val forgotPasswordState by viewModel.forgotPasswordState.collectAsState()
    val context = LocalContext.current
    var showForgotDialog by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()

    // Entry animations
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    LaunchedEffect(authState) {
        if (authState is Resource.Success) {
            onLoginSuccess((authState as Resource.Success).data?.role ?: "")
        }
    }

    LaunchedEffect(forgotPasswordState) {
        forgotPasswordState?.let { resource ->
            when (resource) {
                is Resource.Success -> {
                    showForgotDialog = true
                }
                is Resource.Error -> {
                    Toast.makeText(context, resource.message ?: "Error", Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }

    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text(stringResource(R.string.reset_password), fontWeight = FontWeight.Bold) },
            text = { 
                Text(
                    stringResource(R.string.reset_message),
                    style = MaterialTheme.typography.bodyMedium
                ) 
            },
            confirmButton = {
                Button(
                    onClick = { showForgotDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("OK")
                }
            }
        )
    }

    GlassmorphicBackground(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(600)) + slideInVertically(animationSpec = tween(600))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Glowing Glass App Crest
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = RoundedCornerShape(22.dp),
                        color = if (isDark) Color(0x33FF5E36) else Color(0x24FF5E36),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            Brush.linearGradient(
                                listOf(PrimaryMain, PrimaryMain.copy(alpha = 0.3f))
                            )
                        ),
                        shadowElevation = 8.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = PrimaryMain,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Shri Madhwa Yuvaka Sangha",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp,
                            color = if (isDark) TextWhitePrimary else TextDarkPrimary
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = PrimaryMain.copy(alpha = 0.15f),
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryMain.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "✦ Food Count Portal ✦",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                                color = PrimaryMain
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))

            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(initialOffsetY = { 30 }) + fadeIn(animationSpec = tween(500, delayMillis = 200))
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    elevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(22.dp)
                                    .background(
                                        Brush.verticalGradient(listOf(PrimaryMain, SecondaryMain)),
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    stringResource(R.string.welcome_back),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDark) TextWhitePrimary else TextDarkPrimary
                                )
                                Text(
                                    "Sign in to your food attendance portal",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDark) TextWhiteTertiary else TextDarkSecondary
                                )
                            }
                        }

                        LoginTextField(
                            value = identifier,
                            onValueChange = { identifier = it },
                            label = "Email or User ID",
                            icon = Icons.Default.Person
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        LoginTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = "Password",
                            icon = Icons.Default.Lock,
                            isPassword = true
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        if (authState is Resource.Loading) {
                            Box(modifier = Modifier.padding(vertical = 12.dp)) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    strokeWidth = 3.5.dp,
                                    color = PrimaryMain
                                )
                            }
                        } else {
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val scale by infiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = 1.015f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1100, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "scale"
                            )

                            Button(
                                onClick = {
                                    if (identifier.isNotBlank() && password.isNotBlank()) {
                                        viewModel.login(identifier, password)
                                    } else if (identifier.isBlank()) {
                                        Toast.makeText(context, "Please enter your ID", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Please enter your password", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .scale(scale),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(0.dp),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 8.dp,
                                    pressedElevation = 2.dp
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(PrimaryMain, Color(0xFFFF7A45))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Login,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            if (authState is Resource.Success) "Connecting..." else stringResource(R.string.sign_in), 
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(visible = authState is Resource.Error) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                color = ErrorRed.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = ErrorRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = (authState as? Resource.Error)?.message ?: "Authentication failed",
                                        color = ErrorRed,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(800, delayMillis = 600))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TextButton(onClick = { viewModel.forgotPassword(identifier) }) {
                        Text(
                            stringResource(R.string.forgot_password),
                            color = PrimaryMain,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val privacyUrl = com.satwik.oodapplication.utils.Constants.PRIVACY_POLICY_URL
                    TextButton(onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(privacyUrl))
                        context.startActivity(intent)
                    }) {
                        Text(
                            "Privacy Policy & Security",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) TextWhiteTertiary else TextDarkTertiary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isPassword: Boolean = false
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { 
            Text(
                label,
                fontWeight = FontWeight.SemiBold
            ) 
        },
        leadingIcon = { 
            Icon(
                icon, 
                contentDescription = null, 
                tint = PrimaryMain,
                modifier = Modifier.size(20.dp)
            ) 
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = if (isDark) TextWhiteSecondary else TextDarkSecondary
                    )
                }
            }
        } else null,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryMain,
            unfocusedBorderColor = if (isDark) Color(0x33FFFFFF) else Color(0x28000000),
            focusedLabelColor = PrimaryMain,
            unfocusedLabelColor = if (isDark) TextWhiteSecondary else TextDarkSecondary,
            focusedContainerColor = if (isDark) Color(0x281E293B) else Color(0x60F8FAFC),
            unfocusedContainerColor = if (isDark) Color(0x181E293B) else Color(0x30F8FAFC),
            focusedTextColor = if (isDark) TextWhitePrimary else TextDarkPrimary,
            unfocusedTextColor = if (isDark) TextWhitePrimary else TextDarkPrimary
        ),
        singleLine = true
    )
}
