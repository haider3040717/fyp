package com.example.myapplication.createpost

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.remote.apiService
import com.example.myapplication.data.remote.SessionManager
import com.example.myapplication.data.remote.UserDto
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    onNavigateBack: () -> Unit,
    onPostCreated: () -> Unit = {}
) {
    var postText by remember { mutableStateOf("") }
    var selectedVisibility by remember { mutableStateOf("Public") }
    var showVisibilityDropdown by remember { mutableStateOf(false) }
    var isPosting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var currentUser by remember { mutableStateOf<UserDto?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val visibilityOptions = listOf("Public", "Friends", "Only Me")

    // Load current user
    LaunchedEffect(Unit) {
        try {
            currentUser = apiService.me()
        } catch (e: Exception) {
            // Handle error
        }
    }

    Scaffold(
        topBar = {
            CreatePostTopBar(
                onNavigateBack = onNavigateBack,
                onPostClick = {
                    if (postText.isNotBlank()) {
                        isPosting = true
                        errorMessage = null
                        scope.launch {
                            try {
                                val postData: MutableMap<String, Any> = mutableMapOf("content" to postText)
                                selectedImageUrl?.takeIf { it.isNotBlank() }?.let { postData["image_url"] = it }
                                apiService.createPost(postData)
                                isPosting = false
                                onPostCreated()
                            } catch (e: Exception) {
                                isPosting = false
                                errorMessage = "Failed to create post: ${e.message ?: "Unknown error"}"
                            }
                        }
                    }
                },
                isPostEnabled = postText.isNotBlank() && !isPosting,
                isPosting = isPosting
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
                .verticalScroll(rememberScrollState())
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            // Error message
            if (errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color.Red,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // User Profile Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Picture
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFB3E5FC)), // Light blue like in image
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentUser?.full_name?.firstOrNull()?.toString() ?: "U",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = currentUser?.full_name ?: "User",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )

                    // Visibility Dropdown
                    Box {
                        Row(
                            modifier = Modifier
                                .clickable { showVisibilityDropdown = true }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedVisibility,
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Visibility",
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showVisibilityDropdown,
                            onDismissRequest = { showVisibilityDropdown = false }
                        ) {
                            visibilityOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        selectedVisibility = option
                                        showVisibilityDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Post Text Input
            BasicTextField(
                value = postText,
                onValueChange = { postText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .heightIn(min = 200.dp),
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = Color.Black,
                    lineHeight = 24.sp
                ),
                decorationBox = { innerTextField ->
                    if (postText.isEmpty()) {
                        Text(
                            text = "Share your thoughts. Add photos or hashtags.",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                    innerTextField()
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Bottom Action Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PostActionItem(
                        icon = Icons.Default.Image,
                        contentDescription = "Add Photo",
                        onClick = {
                            // For now, allow manual URL entry
                            // In production, integrate image picker
                            selectedImageUrl = "https://via.placeholder.com/400"
                        }
                    )

                    PostActionItem(
                        icon = Icons.Default.LocationOn,
                        contentDescription = "Add Location/Event",
                        onClick = {
                            // Create event and link to post
                            scope.launch {
                                try {
                                    // For now, create a simple event
                                    // In production, show location picker dialog
                                    val event = apiService.createEvent(mapOf(
                                        "title" to "Event from Post",
                                        "description" to postText.take(100),
                                        "location" to "Campus",
                                        "latitude" to "24.8607",
                                        "longitude" to "67.0011",
                                        "start_date" to java.time.Instant.now().toString(),
                                        "end_date" to java.time.Instant.now().plusSeconds(3600).toString()
                                    ))
                                    // Note: Event linking to post will be handled in backend
                                    // when creating post with event_id
                                } catch (e: Exception) {
                                    errorMessage = "Failed to create event: ${e.message ?: "Unknown error"}"
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostTopBar(
    onNavigateBack: () -> Unit,
    onPostClick: () -> Unit,
    isPostEnabled: Boolean,
    isPosting: Boolean
) {
    TopAppBar(
        title = {
            Text(
                text = "New Post",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
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
            // Post Button
            TextButton(
                onClick = onPostClick,
                enabled = isPostEnabled,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                if (isPosting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF007AFF)
                    )
                } else {
                    Text(
                        text = "Post",
                        color = if (isPostEnabled) Color(0xFF007AFF) else Color.Gray,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White
        )
    )
}

@Composable
fun PostActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color(0xFFF5F5F5))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color(0xFF007AFF),
            modifier = Modifier.size(24.dp)
        )
    }
}
