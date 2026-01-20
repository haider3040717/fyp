package com.example.myapplication.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.remote.apiService
import kotlinx.coroutines.launch

// Data class for settings items
data class SettingItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val icon: ImageVector,
    val action: String? = null, // "toggle", "navigate", "logout", etc.
    val isToggled: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onLogout: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {}
) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var darkModeEnabled by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var currentUser by remember { mutableStateOf<com.example.myapplication.data.remote.UserDto?>(null) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        try {
            currentUser = com.example.myapplication.data.remote.apiService.me()
        } catch (e: Exception) {
            // Handle error
        }
    }

    Scaffold(
        topBar = {
            SettingsTopAppBar(onNavigateBack = onNavigateBack)
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFAFAFA))
        ) {
            // User Profile Section
            item {
                UserProfileSection(
                    onEditClick = onNavigateToEditProfile
                )
            }

            // Account Settings Section
            item {
                SettingsSectionHeader(title = "Account")
            }

            item {
                SettingsItemRow(
                    icon = Icons.Default.Email,
                    title = "Email Address",
                    subtitle = currentUser?.university_email ?: "Not set",
                    onClick = { showEmailDialog = true }
                )
            }

            item {
                SettingsItemRow(
                    icon = Icons.Default.Lock,
                    title = "Change Password",
                    subtitle = "Update your password",
                    onClick = { showPasswordDialog = true }
                )
            }

            item {
                SettingsItemRow(
                    icon = Icons.Default.Info,
                    title = "Seat Number",
                    subtitle = currentUser?.seat_number ?: "",
                    onClick = { /* View seat info - read only */ }
                )
            }

            // Privacy & Security Section
            item {
                SettingsSectionHeader(title = "Privacy & Security")
            }

            item {
                SettingsItemRow(
                    icon = Icons.Default.Security,
                    title = "Privacy Settings",
                    subtitle = "Control who sees your profile",
                    onClick = onNavigateToPrivacy
                )
            }

            item {
                SettingsItemRow(
                    icon = Icons.Default.Block,
                    title = "Blocked Users",
                    subtitle = "Manage blocked accounts",
                    onClick = { /* Navigate to blocked users */ }
                )
            }

            item {
                SettingsItemRow(
                    icon = Icons.Default.Visibility,
                    title = "Profile Visibility",
                    subtitle = "Public",
                    onClick = { /* Change visibility */ }
                )
            }

            // Notifications Section
            item {
                SettingsSectionHeader(title = "Notifications")
            }

            item {
                SettingsToggleRow(
                    icon = Icons.Default.Notifications,
                    title = "Push Notifications",
                    subtitle = "Get alerts for messages and updates",
                    isToggled = notificationsEnabled,
                    onToggle = { notificationsEnabled = it }
                )
            }

            item {
                SettingsItemRow(
                    icon = Icons.Default.NotificationsActive,
                    title = "Notification Preferences",
                    subtitle = "Customize notification types",
                    onClick = onNavigateToNotifications
                )
            }

            // Display & Appearance Section
            item {
                SettingsSectionHeader(title = "Display & Appearance")
            }

            item {
                SettingsToggleRow(
                    icon = Icons.Default.DarkMode,
                    title = "Dark Mode",
                    subtitle = "Use dark theme",
                    isToggled = darkModeEnabled,
                    onToggle = { darkModeEnabled = it }
                )
            }

            item {
                SettingsItemRow(
                    icon = Icons.Default.TextFields,
                    title = "Text Size",
                    subtitle = "Medium",
                    onClick = { /* Change text size */ }
                )
            }

            // Help & Support Section
            item {
                SettingsSectionHeader(title = "Help & Support")
            }

            item {
                SettingsItemRow(
                    icon = Icons.Default.Help,
                    title = "Help Center",
                    subtitle = "FAQs and guides",
                    onClick = { /* Open help */ }
                )
            }

            item {
                SettingsItemRow(
                    icon = Icons.Default.BugReport,
                    title = "Report a Problem",
                    subtitle = "Send feedback or report bugs",
                    onClick = { /* Open bug report */ }
                )
            }

            item {
                SettingsItemRow(
                    icon = Icons.Default.Info,
                    title = "About Campus Connect",
                    subtitle = "Version 1.0.0",
                    onClick = { /* Show about dialog */ }
                )
            }

            item {
                SettingsItemRow(
                    icon = Icons.Default.Description,
                    title = "Terms of Service",
                    subtitle = "Read our terms",
                    onClick = { /* Open terms */ }
                )
            }

            item {
                SettingsItemRow(
                    icon = Icons.Default.Security,
                    title = "Privacy Policy",
                    subtitle = "Read our privacy policy",
                    onClick = { /* Open privacy policy */ }
                )
            }

            // Logout Section
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF3B30) // Red color
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Logout",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Logout",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout from Campus Connect?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Logout", color = Color(0xFFFF3B30))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Email Update Dialog
    if (showEmailDialog) {
        var emailText by remember { mutableStateOf(currentUser?.university_email ?: "") }
        var isUpdating by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        
        AlertDialog(
            onDismissRequest = { showEmailDialog = false },
            title = { Text("Update Email") },
            text = {
                Column {
                    if (errorMessage != null) {
                        Text(errorMessage!!, color = Color.Red, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    OutlinedTextField(
                        value = emailText,
                        onValueChange = { emailText = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (emailText.isNotBlank() && emailText.contains("@")) {
                            isUpdating = true
                            errorMessage = null
                            scope.launch {
                                try {
                                    apiService.updateEmail(mapOf("university_email" to emailText))
                                    currentUser = apiService.me() // Refresh user data
                                    isUpdating = false
                                    showEmailDialog = false
                                } catch (e: Exception) {
                                    isUpdating = false
                                    errorMessage = "Failed to update email: ${e.message ?: "Unknown error"}"
                                }
                            }
                        } else {
                            errorMessage = "Please enter a valid email address"
                        }
                    },
                    enabled = !isUpdating
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Update")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmailDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Password Change Dialog
    if (showPasswordDialog) {
        var oldPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var isChanging by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("Change Password") },
            text = {
                Column {
                    if (errorMessage != null) {
                        Text(errorMessage!!, color = Color.Red, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it },
                        label = { Text("Current Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm New Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (oldPassword.isBlank() || newPassword.isBlank()) {
                            errorMessage = "All fields are required"
                        } else if (newPassword.length < 8) {
                            errorMessage = "New password must be at least 8 characters"
                        } else if (newPassword != confirmPassword) {
                            errorMessage = "New passwords do not match"
                        } else {
                            isChanging = true
                            errorMessage = null
                            scope.launch {
                                try {
                                    apiService.changePassword(mapOf(
                                        "old_password" to oldPassword,
                                        "new_password" to newPassword,
                                        "confirm_new_password" to confirmPassword
                                    ))
                                    isChanging = false
                                    showPasswordDialog = false
                                } catch (e: Exception) {
                                    isChanging = false
                                    errorMessage = "Failed to change password: ${e.message ?: "Unknown error"}"
                                }
                            }
                        }
                    },
                    enabled = !isChanging
                ) {
                    if (isChanging) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Change Password")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopAppBar(
    onNavigateBack: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = "Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White
        )
    )
}

@Composable
fun UserProfileSection(
    onEditClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Profile Picture
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFB3E5FC)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "JD",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // User Info
                Column {
                    Text(
                        text = "John Doe",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "CS-2021-001",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "Computer Science",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            // Edit Button
            IconButton(
                onClick = onEditClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE3F2FD))
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = Color(0xFF007AFF),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.Gray,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
fun SettingsItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE3F2FD)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    isToggled: Boolean = false,
    onToggle: (Boolean) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE3F2FD)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Switch(
                checked = isToggled,
                onCheckedChange = { onToggle(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF007AFF),
                    uncheckedTrackColor = Color(0xFFE0E0E0)
                )
            )
        }
    }
}
