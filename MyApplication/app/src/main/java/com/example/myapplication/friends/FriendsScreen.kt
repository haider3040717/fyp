package com.example.myapplication.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.remote.UserDto
import com.example.myapplication.data.remote.apiService
import kotlinx.coroutines.launch

// Data class for friend request
data class FriendRequest(
    val id: String,
    val name: String,
    val info: String, // e.g., "4th yr", "Alumni"
    val profileImage: String = "",
    val isAccepted: Boolean = false,
    val isRejected: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToProfile: (String) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var friendRequests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var invites by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var suggestions by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun loadFriendRequests() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val users: List<UserDto> = apiService.getFriendRequests()
                friendRequests = users.map { user ->
                    FriendRequest(
                        id = user.id.toString(),
                        name = user.full_name,
                        info = user.year?.let { "${it}th yr" } ?: user.department ?: "",
                        profileImage = user.profile?.avatar_url ?: ""
                    )
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load friend requests: ${e.message ?: "Unknown error"}"
            } finally {
                isLoading = false
            }
        }
    }

    fun loadSuggestions() {
        scope.launch {
            try {
                val users: List<UserDto> = apiService.getFriendSuggestions()
                suggestions = users.map { user ->
                    FriendRequest(
                        id = user.id.toString(),
                        name = user.full_name,
                        info = user.year?.let { "${it}th yr" } ?: user.department ?: "",
                        profileImage = user.profile?.avatar_url ?: ""
                    )
                }
            } catch (e: Exception) {
                // Handle error silently for suggestions
            }
        }
    }

    LaunchedEffect(Unit) {
        loadFriendRequests()
        loadSuggestions()
    }

    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            0 -> loadFriendRequests()
            2 -> loadSuggestions()
        }
    }

    Scaffold(
        topBar = {
            FriendsTopAppBar(
                onSearchClick = { /* TODO: Add search */ },
                onNotificationsClick = { /* TODO: Add notifications */ },
                onMessagesClick = { /* TODO: Add messages */ }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF9F9F9))
        ) {
            // Tab Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FriendTabButton(
                    label = "Requests",
                    isSelected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                FriendTabButton(
                    label = "Invites",
                    isSelected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                FriendTabButton(
                    label = "Suggestions",
                    isSelected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
            }

            // Content based on selected tab
            when (selectedTab) {
                0 -> {
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        FriendRequestsList(
                            requests = friendRequests,
                            onAccept = { requestId ->
                                scope.launch {
                                    try {
                                        apiService.followUser(requestId.toInt())
                                        loadFriendRequests() // Reload to update list
                                    } catch (e: Exception) {
                                        // Handle error
                                    }
                                }
                            },
                            onReject = { requestId ->
                                friendRequests = friendRequests.filter { it.id != requestId }
                            },
                            onProfileClick = onNavigateToProfile
                        )
                    }
                }
                1 -> FriendRequestsList(
                    requests = invites,
                    onAccept = { /* Invites not implemented in backend yet */ },
                    onReject = { /* Invites not implemented in backend yet */ },
                    onProfileClick = onNavigateToProfile
                )
                2 -> {
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        FriendRequestsList(
                            requests = suggestions,
                            onAccept = { requestId ->
                                scope.launch {
                                    try {
                                        apiService.followUser(requestId.toInt())
                                        loadSuggestions() // Reload to update list
                                    } catch (e: Exception) {
                                        // Handle error
                                    }
                                }
                            },
                            onReject = { requestId ->
                                suggestions = suggestions.filter { it.id != requestId }
                            },
                            onProfileClick = onNavigateToProfile
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsTopAppBar(
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onMessagesClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Friends",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.Black
                )
            }
            IconButton(onClick = onNotificationsClick) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = Color.Black
                )
            }
            IconButton(onClick = onMessagesClick) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Messages",
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
fun FriendTabButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF007AFF) else Color(0xFFCCCCCC),
            disabledContainerColor = Color(0xFFCCCCCC)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

@Composable
fun FriendRequestsList(
    requests: List<FriendRequest>,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit,
    onProfileClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            items = requests.filterNot { it.isRejected },
            key = { it.id }
        ) { request ->
            FriendRequestCard(
                request = request,
                onAccept = { onAccept(request.id) },
                onReject = { onReject(request.id) },
                onProfileClick = { onProfileClick(request.id) }
            )
        }
    }
}

@Composable
fun FriendRequestCard(
    request: FriendRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onProfileClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProfileClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Profile Avatar
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFB3E5FC)), // Light blue
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = request.name.split(" ").map { it.first() }.joinToString(""),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // User Info
                Column {
                    Text(
                        text = request.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                    Text(
                        text = request.info,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                // Accept Button (Checkmark)
                if (!request.isAccepted) {
                    IconButton(
                        onClick = onAccept,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF007AFF))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Accept",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    // Show accepted state
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Accepted",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Reject Button (X)
                IconButton(
                    onClick = onReject,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0E0E0))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Reject",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FriendsBottomNavigationBar(
    selectedTab: Int = 1
) {
    val items = listOf(
        Pair(Icons.Default.Home, Icons.Outlined.Home),
        Pair(Icons.Default.Group, Icons.Outlined.Group),
        Pair(Icons.Default.Add, Icons.Outlined.Add),
        Pair(Icons.Default.LocationOn, Icons.Outlined.LocationOn),
        Pair(Icons.Default.Person, Icons.Outlined.Person)
    )

    NavigationBar(
        containerColor = Color.Black,
        modifier = Modifier.height(60.dp)
    ) {
        items.forEachIndexed { index, (selectedIcon, unselectedIcon) ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (selectedTab == index) selectedIcon else unselectedIcon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                selected = selectedTab == index,
                onClick = { },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    unselectedIconColor = Color.Gray,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
