package com.example.myapplication.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.myapplication.data.remote.PostDto
import com.example.myapplication.data.remote.apiService
import com.example.myapplication.home.PostItem
import com.example.myapplication.home.Post
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToPostDetail: (String) -> Unit = {},
    onNavigateToFriends: () -> Unit = {},
    userId: Int? = null, // Optional: for viewing other users' profiles
    externalRefreshTrigger: Int = 0
) {
    var currentUserId by remember { mutableIntStateOf(0) }
    val isOwnProfile = userId == null || userId == currentUserId
    
    LaunchedEffect(Unit) {
        try {
            val user = apiService.me()
            currentUserId = user.id
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    var userData by remember { mutableStateOf<UserProfile?>(null) }
    var userPosts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var localRefreshTrigger by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun loadProfile() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val user = if (userId != null && !isOwnProfile) {
                    apiService.getUserProfile(userId)
                } else {
                    apiService.me()
                }
                val profile = apiService.profile()

                android.util.Log.d("ProfileScreen", "Loaded user: ${user.full_name}, friends: ${user.friends_count}")
                android.util.Log.d("ProfileScreen", "Loaded profile bio: ${profile.bio}")
                
                val userProfile = UserProfile(
                    id = user.id,
                    name = user.full_name,
                    seatNo = user.seat_number,
                    email = user.university_email ?: "",
                    department = user.department ?: "",
                    year = user.year?.toString() ?: "",
                    bio = profile.bio ?: "",
                    friends = user.friends_count,
                    posts = user.posts_count,
                    profileImage = profile.avatar_url ?: ""
                )

                android.util.Log.d("ProfileScreen", "Created UserProfile with friends count: ${userProfile.friends}")
                userData = userProfile

                // Load user's posts
                val targetUserId = userId ?: currentUserId
                if (targetUserId != null) {
                    val posts = apiService.getUserPosts(targetUserId)
                    val dateFormatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    userPosts = posts.map { dto ->
                        Post(
                            id = dto.id,
                            userName = dto.author.full_name,
                            profileImage = dto.author.profile?.avatar_url ?: "",
                            timeAgo = try {
                                val instant = Instant.parse(dto.created_at)
                                val date = Date.from(instant)
                                dateFormatter.format(date)
                            } catch (e: Exception) {
                                // Fallback to original date if parsing fails
                                dateFormatter.format(Date())
                            },
                            content = dto.content,
                            postImage = dto.image_url,
                            likes = dto.like_count,
                            comments = dto.comment_count,
                            isLiked = dto.is_liked,
                            isBookmarked = false,
                            isAuthor = dto.is_author
                        )
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load profile: ${e.message ?: "Unknown error"}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(userId, localRefreshTrigger, externalRefreshTrigger) {
        android.util.Log.d("ProfileScreen", "Loading profile - userId: $userId, isOwnProfile: $isOwnProfile, localTrigger: $localRefreshTrigger, externalTrigger: $externalRefreshTrigger")
        loadProfile()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isOwnProfile) {
                        IconButton(onClick = {
                            // Manual refresh for testing
                            localRefreshTrigger++
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(errorMessage ?: "Error", color = Color.Red)
                }
            }
            userData != null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(bottom = 80.dp), // Add padding to avoid bottom nav overlap
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        ProfileHeader(userData!!)
                    }

                    item {
                        ProfileStats(userData!!, onFriendsClick = onNavigateToFriends)
                    }

                    if (isOwnProfile) {
                        item {
                            ProfileActions(
                                onNavigateToEditProfile = onNavigateToEditProfile,
                                onCopyProfileUrl = {
                                    scope.launch {
                                        try {
                                            val shareResponse = apiService.getProfileShareUrl(userData!!.id)
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Profile URL", shareResponse.share_url)
                                            clipboard.setPrimaryClip(clip)
                                            // Show snackbar or toast
                                        } catch (e: Exception) {
                                            errorMessage = "Failed to copy URL: ${e.message ?: "Unknown error"}"
                                        }
                                    }
                                }
                            )
                        }
                    }

                    item {
                        Text(
                            text = if (isOwnProfile) "My Posts" else "Posts",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    if (userPosts.isEmpty()) {
                        item {
                            Text(
                                text = "No posts yet",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    } else {
                        items(userPosts) { post ->
                            PostItem(
                                post = post,
                                onPostClick = { onNavigateToPostDetail(post.id.toString()) },
                                onRefresh = { localRefreshTrigger++ }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = Color.Gray.copy(alpha = 0.1f),
                                thickness = 1.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileHeader(userData: UserProfile) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Picture - Use AsyncImage if URL exists, otherwise show initials
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        if (userData.profileImage.isNotBlank()) Color.Transparent
                        else Color(0xFF007AFF)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (userData.profileImage.isNotBlank()) {
                    AsyncImage(
                        model = userData.profileImage,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Text(
                        text = userData.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }
                            .joinToString(""),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = userData.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = userData.seatNo,
                fontSize = 16.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${userData.department} • ${userData.year}",
                fontSize = 14.sp,
                color = Color.Gray
            )

            if (userData.bio.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = userData.bio,
                    fontSize = 14.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
fun ProfileStats(
    userData: UserProfile,
    onFriendsClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("Posts", userData.posts.toString())
            StatItem("Friends", userData.friends.toString(), onClick = onFriendsClick)
        }
    }
}

@Composable
fun StatItem(label: String, value: String, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF007AFF)
        )
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun ProfileActions(
    onNavigateToEditProfile: () -> Unit,
    onCopyProfileUrl: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onNavigateToEditProfile,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
        ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Edit Profile")
        }

        OutlinedButton(
            onClick = onCopyProfileUrl,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Copy URL")
        }
    }
}

// Data classes
data class UserProfile(
    val id: Int,
    val name: String,
    val seatNo: String,
    val email: String,
    val department: String,
    val year: String,
    val bio: String,
    val friends: Int,
    val posts: Int,
    val profileImage: String
)
