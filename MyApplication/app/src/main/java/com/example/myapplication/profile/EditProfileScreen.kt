package com.example.myapplication.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.remote.ProfileDto
import com.example.myapplication.data.remote.apiService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    onSaveClick: () -> Unit = {}
) {
    var bio by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }
    var interests by remember { mutableStateOf("") }
    var avatarUrl by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val profile = apiService.profile()
            bio = profile.bio ?: ""
            course = profile.course ?: ""
            interests = profile.interests ?: ""
            avatarUrl = profile.avatar_url
        } catch (e: Exception) {
            errorMessage = "Failed to load profile: ${e.message ?: "Unknown error"}"
        } finally {
            isLoading = false
        }
    }


    Scaffold(
        topBar = {
            EditProfileTopBar(
                onNavigateBack = onNavigateBack,
                onSaveClick = {
                    isSaving = true
                    scope.launch {
                        try {
                            apiService.updateProfile(
                                ProfileDto(
                                    bio = bio,
                                    course = course,
                                    interests = interests,
                                    avatar_url = avatarUrl
                                )
                            )
                            isSaving = false
                            onSaveClick()
                        } catch (e: Exception) {
                            isSaving = false
                            errorMessage = "Failed to save: ${e.message ?: "Unknown error"}"
                        }
                    }
                },
                isSaving = isSaving
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color(0xFAFAFA))
                        .verticalScroll(rememberScrollState())
                ) {
                    if (errorMessage != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f))
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = Color.Red,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Form Fields
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            // Course
                            Text(
                                text = "Course",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            OutlinedTextField(
                                value = course,
                                onValueChange = { course = it },
                                placeholder = { Text("Enter your course") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF007AFF),
                                    unfocusedBorderColor = Color(0xFFE5E5EA)
                                )
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Interests
                            Text(
                                text = "Interests",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            OutlinedTextField(
                                value = interests,
                                onValueChange = { interests = it },
                                placeholder = { Text("Enter your interests") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF007AFF),
                                    unfocusedBorderColor = Color(0xFFE5E5EA)
                                )
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Bio
                            Text(
                                text = "Bio",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            OutlinedTextField(
                                value = bio,
                                onValueChange = {
                                    if (it.length <= 150) {
                                        bio = it
                                    }
                                },
                                placeholder = { Text("Tell us about yourself") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 100.dp),
                                maxLines = 4,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF007AFF),
                                    unfocusedBorderColor = Color(0xFFE5E5EA)
                                )
                            )

                            // Character count for bio
                            Text(
                                text = "${bio.length}/150",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .padding(top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Save Button
                    Button(
                        onClick = {
                            isSaving = true
                            scope.launch {
                                try {
                                    apiService.updateProfile(
                                        ProfileDto(
                                            bio = bio,
                                            course = course,
                                            interests = interests,
                                            avatar_url = null
                                        )
                                    )
                                    isSaving = false
                                    onSaveClick()
                                } catch (e: Exception) {
                                    isSaving = false
                                    errorMessage = "Failed to save: ${e.message ?: "Unknown error"}"
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .padding(horizontal = 16.dp),
                        enabled = !isSaving && bio.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF007AFF),
                            disabledContainerColor = Color(0xFF007AFF).copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving...", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Changes", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileTopBar(
    onNavigateBack: () -> Unit,
    onSaveClick: () -> Unit,
    isSaving: Boolean
) {
    TopAppBar(
        title = {
            Text(
                text = "Edit Profile",
                fontSize = 20.sp,
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
        actions = {
            TextButton(
                onClick = onSaveClick,
                enabled = !isSaving
            ) {
                Text(
                    text = "Save",
                    color = if (isSaving) Color.Gray else Color(0xFF007AFF),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White
        )
    )
}
