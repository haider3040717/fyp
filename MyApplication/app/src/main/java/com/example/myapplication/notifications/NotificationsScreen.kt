package com.example.myapplication.notifications

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.remote.NotificationDto
import com.example.myapplication.data.remote.apiService
import kotlinx.coroutines.launch

// Data class for notifications
data class Notification(
    val id: Int,
    val type: NotificationType, // Like, Comment, FriendRequest, Mention, PostUpdate
    val userName: String,
    val userInitials: String,
    val action: String, // e.g., "liked your post", "commented on your post"
    val content: String?, // e.g., post preview or comment text
    val timestamp: String,
    val isRead: Boolean = false,
    val icon: ImageVector = Icons.Default.Favorite,
    val relatedObjectId: Int? = null
)

enum class NotificationType {
    LIKE, COMMENT, FRIEND_REQUEST, MENTION, POST_UPDATE, EVENT, MESSAGE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onNavigateBack: () -> Unit = {},
    onNotificationClick: (String) -> Unit = {},
    onNavigateToPost: (String) -> Unit = {},
    onNavigateToProfile: (Int) -> Unit = {},
    onNavigateToEvent: (Int) -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf("All") }
    var notifications by remember { mutableStateOf<List<Notification>>(emptyList()) }
    var remoteNotifications by remember { mutableStateOf<List<NotificationDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun loadNotifications() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                remoteNotifications = apiService.getNotifications()
                notifications = remoteNotifications.map { n ->
                    Notification(
                        id = n.id,
                        type = when (n.type) {
                            "like" -> NotificationType.LIKE
                            "comment" -> NotificationType.COMMENT
                            "follow" -> NotificationType.FRIEND_REQUEST
                            "message" -> NotificationType.MESSAGE
                            "event" -> NotificationType.EVENT
                            else -> NotificationType.POST_UPDATE
                        },
                        userName = n.actor?.full_name ?: "System",
                        userInitials = n.actor?.full_name?.split(" ")
                            ?.mapNotNull { it.firstOrNull()?.toString() }?.joinToString("") ?: "CC",
                        action = n.text,
                        content = null,
                        timestamp = n.created_at,
                        isRead = n.is_read,
                        relatedObjectId = n.related_object_id,
                        icon = when (n.type) {
                            "like" -> Icons.Default.Favorite
                            "comment" -> Icons.Default.Comment
                            "follow" -> Icons.Default.PersonAdd
                            "message" -> Icons.Default.Send
                            "event" -> Icons.Default.Event
                            else -> Icons.Default.Notifications
                        }
                    )
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load notifications: ${e.message ?: "Unknown error"}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadNotifications()
    }

    val filterOptions = listOf("All", "Requests", "Likes", "Comments", "Events")
    val filteredNotifications = when (selectedFilter) {
        "Requests" -> notifications.filter { it.type == NotificationType.FRIEND_REQUEST }
        "Likes" -> notifications.filter { it.type == NotificationType.LIKE }
        "Comments" -> notifications.filter { it.type == NotificationType.COMMENT }
        "Events" -> notifications.filter { it.type in listOf(NotificationType.EVENT, NotificationType.POST_UPDATE) }
        else -> notifications
    }

    Scaffold(
        topBar = {
            NotificationsTopAppBar(
                onNotificationsClick = { /* Already on notifications */ }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(bottom = 80.dp) // Add padding to avoid bottom nav overlap
                .background(Color(0xFAFAFA))
        ) {
            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                filterOptions.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF007AFF),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFFE5E5EA),
                            labelColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            // Notifications List
            if (isLoading && filteredNotifications.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredNotifications.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color.Red,
                            modifier = Modifier.padding(8.dp)
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsNone,
                                contentDescription = "No notifications",
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No notifications yet",
                                fontSize = 16.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(
                        items = filteredNotifications,
                        key = { it.id }
                    ) { notification ->
                        NotificationItem(
                            notification = notification,
                            onClick = { 
                                // Mark as read when clicked
                                scope.launch {
                                    try {
                                        apiService.markNotificationRead(notification.id)
                                        // Update local state
                                        notifications = notifications.map { 
                                            if (it.id == notification.id) it.copy(isRead = true) else it 
                                        }
                                    } catch (e: Exception) {
                                        // Handle error silently
                                    }
                                }
                                // Navigate based on notification type
                                when (notification.type) {
                                    NotificationType.LIKE, NotificationType.COMMENT -> {
                                        notification.relatedObjectId?.let { 
                                            onNavigateToPost(it.toString()) 
                                        }
                                    }
                                    NotificationType.FRIEND_REQUEST -> {
                                        remoteNotifications.find { it.id == notification.id }?.actor?.id?.let { 
                                            onNavigateToProfile(it) 
                                        }
                                    }
                                    NotificationType.EVENT -> {
                                        notification.relatedObjectId?.let { 
                                            onNavigateToEvent(it) 
                                        }
                                    }
                                    else -> onNotificationClick(notification.id.toString())
                                }
                            },
                            onAccept = {
                                if (notification.type == NotificationType.FRIEND_REQUEST) {
                                    scope.launch {
                                        try {
                                            // Accept friend request by following back
                                            val actorId = remoteNotifications.find { it.id == notification.id }?.actor?.id
                                            if (actorId != null) {
                                                apiService.followUser(actorId)
                                                notifications = notifications.filter { it.id != notification.id }
                                                loadNotifications() // Refresh list
                                            }
                                        } catch (e: Exception) {
                                            // Handle error
                                        }
                                    }
                                }
                            },
                            onReject = {
                                if (notification.type == NotificationType.FRIEND_REQUEST) {
                                    scope.launch {
                                        try {
                                            apiService.markNotificationRead(notification.id)
                                            notifications = notifications.filter { it.id != notification.id }
                                        } catch (e: Exception) {
                                            // Handle error
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsTopAppBar(
    onNotificationsClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Notifications",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        },
        actions = {
            IconButton(onClick = onNotificationsClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
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
fun NotificationItem(
    notification: Notification,
    onClick: () -> Unit,
    onAccept: () -> Unit = {},
    onReject: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) Color.White else Color(0xFFF0F7FF)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar with Icon
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFB3E5FC)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = notification.userInitials,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Notification Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // User name and action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = notification.userName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                        Text(
                            text = notification.action,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                    Text(
                        text = notification.timestamp,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                // Content preview
                notification.content?.let {
                    Text(
                        text = it,
                        fontSize = 13.sp,
                        color = Color(0xFF666666),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Action buttons for friend requests
                if (notification.type == NotificationType.FRIEND_REQUEST) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onAccept,
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF007AFF)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                "Accept",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        OutlinedButton(
                            onClick = onReject,
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                "Reject",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            // Unread indicator
            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF007AFF))
                )
            }
        }
    }
}

@Composable
fun NotificationsBottomNavigationBar(
    selectedTab: Int = 3
) {
    val items = listOf(
        Pair(Icons.Default.Home, Icons.Outlined.Home),
        Pair(Icons.Default.Group, Icons.Outlined.Group),
        Pair(Icons.Default.Add, Icons.Outlined.Add),
        Pair(Icons.Default.Notifications, Icons.Outlined.Notifications),
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
