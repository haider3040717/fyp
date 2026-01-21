package com.example.myapplication.search

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.remote.UserDto
import com.example.myapplication.data.remote.PostDto
import com.example.myapplication.data.remote.StartConversationRequest
import com.example.myapplication.data.remote.apiService
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit = {},
    onNavigateToPostDetail: (String) -> Unit = {},
    onNavigateToChat: (String, String, Boolean) -> Unit = { _, _, _ -> }
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    var searchUsers by remember { mutableStateOf<List<SearchUser>>(emptyList()) }
    var searchPosts by remember { mutableStateOf<List<SearchPost>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun performSearch(query: String) {
        if (query.isBlank()) {
            searchUsers = emptyList()
            searchPosts = emptyList()
            return
        }

        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val searchType = when (selectedTab) {
                    0 -> "users"
                    1 -> "posts"
                    else -> null
                }
                val response = apiService.search(query, searchType)
                
                searchUsers = response.users.map { user ->
                    SearchUser(
                        id = user.id.toString(),
                        name = user.full_name,
                        seatNo = user.seat_number,
                        department = user.department ?: "",
                        profileImage = user.profile?.avatar_url ?: ""
                    )
                }
                
                searchPosts = response.posts.map { post ->
                    SearchPost(
                        id = post.id.toString(),
                        userName = post.author.full_name,
                        content = post.content,
                        timeAgo = post.created_at
                    )
                }
            } catch (e: Exception) {
                errorMessage = "Search failed: ${e.message ?: "Unknown error"}"
            } finally {
                isLoading = false
            }
        }
    }

    // Debounce search
    LaunchedEffect(searchQuery) {
        delay(500) // Wait 500ms after user stops typing
        performSearch(searchQuery)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                },
                placeholder = { Text("Search students, posts...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp)
            )

            // Search Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Students") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Posts") }
                )
            }

            // Success message
            if (successMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f))
                ) {
                    Text(
                        text = successMessage ?: "",
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Error message
            if (errorMessage != null && !errorMessage!!.contains("Search failed")) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color.Red,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Search Results
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = errorMessage ?: "Error",
                            color = Color.Red
                        )
                    }
                }
                searchQuery.isBlank() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Start typing to search...",
                            color = Color.Gray
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        if (selectedTab == 0) {
                            // Students Tab
                            if (searchUsers.isEmpty()) {
                                item {
                                    Text(
                                        text = "No users found",
                                        color = Color.Gray,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            } else {
                                items(searchUsers) { user ->
                                    UserSearchItem(
                                        user = user,
                                        onUserClick = {
                                            onNavigateToProfile(user.id)
                                        },
                                        onAddFriend = {
                                            scope.launch {
                                                try {
                                                    apiService.followUser(user.id.toInt())
                                                    successMessage = "Friend request sent successfully!"
                                                    // Clear success message after 3 seconds
                                                    kotlinx.coroutines.delay(3000)
                                                    successMessage = null
                                                } catch (e: Exception) {
                                                    errorMessage = "Failed to send friend request: ${e.message ?: "Unknown error"}"
                                                    // Clear error message after 3 seconds
                                                    kotlinx.coroutines.delay(3000)
                                                    errorMessage = null
                                                }
                                            }
                                        },
                                        onMessageClick = {
                                            scope.launch {
                                                try {
                                                    // Start a conversation with this user
                                                    val response = apiService.startConversation(StartConversationRequest(user_id = user.id.toInt()))
                                                    val conversationId = response["conversation_id"] ?: 0
                                                    // Navigate to chat
                                                    onNavigateToChat(conversationId.toString(), user.name, false)
                                                } catch (e: Exception) {
                                                    errorMessage = "Failed to start conversation: ${e.message ?: "Unknown error"}"
                                                    // Clear error message after 3 seconds
                                                    kotlinx.coroutines.delay(3000)
                                                    errorMessage = null
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        } else {
                            // Posts Tab
                            if (searchPosts.isEmpty()) {
                                item {
                                    Text(
                                        text = "No posts found",
                                        color = Color.Gray,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            } else {
                                items(searchPosts) { post ->
                                    PostSearchItem(
                                        post = post,
                                        onPostClick = {
                                            onNavigateToPostDetail(post.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserSearchItem(
    user: SearchUser,
    onUserClick: () -> Unit,
    onAddFriend: () -> Unit,
    onMessageClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onUserClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Picture
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF007AFF)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.name.split(" ").map { it.first() }.joinToString(""),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = user.seatNo,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = user.department,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onAddFriend,
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onMessageClick,
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Message", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun PostSearchItem(
    post: SearchPost,
    onPostClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPostClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF007AFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = post.userName.split(" ").map { it.first() }.joinToString(""),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = post.userName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = post.timeAgo,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = post.content,
                fontSize = 14.sp,
                color = Color.Black
            )
        }
    }
}

// Data classes for mock data
data class SearchUser(
    val id: String,
    val name: String,
    val seatNo: String,
    val department: String,
    val profileImage: String
)

data class SearchPost(
    val id: String,
    val userName: String,
    val content: String,
    val timeAgo: String
)
