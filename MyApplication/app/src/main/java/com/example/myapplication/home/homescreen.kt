package com.example.myapplication.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.myapplication.data.remote.PostDto
import com.example.myapplication.data.remote.StoryDto
import com.example.myapplication.data.remote.StoryReplyDto
import com.example.myapplication.data.remote.apiService
import com.example.myapplication.data.remote.SessionManager
import com.example.myapplication.data.remote.ShareResponse
import com.example.myapplication.data.remote.CommentRequest
import com.example.myapplication.data.remote.StoryReplyRequest
import kotlinx.coroutines.launch
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.ModalBottomSheet

// Data classes
data class Story(
    val id: String,
    val userName: String,
    val profileImage: String,
    val isAddStory: Boolean = false,
    val likeCount: Int = 0,
    val replyCount: Int = 0,
    val isLiked: Boolean = false
)

data class Post(
    val id: Int,
    val userName: String,
    val profileImage: String,
    val timeAgo: String,
    val content: String,
    val postImage: String? = null,
    val likes: Int,
    val comments: Int,
    val isLiked: Boolean = false,
    val isBookmarked: Boolean = false,
    val isAuthor: Boolean = false,
    val eventId: Int? = null
)

data class BottomNavItem(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToProfile: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToEventMap: () -> Unit = {},
    onNavigateToMessages: () -> Unit = {},
    onNavigateToCreatePost: () -> Unit = {},
    onNavigateToFriends: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToPostDetail: (String) -> Unit = {},
    onNavigateToCreateStory: () -> Unit = {}
) {

    var stories by remember { mutableStateOf<List<Story>>(emptyList()) }
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    fun loadStories() {
        scope.launch {
            try {
                val remoteStories: List<StoryDto> = apiService.getStories()
                // Only show "Share Story" + real stories if there are any
                if (remoteStories.isEmpty()) {
                    stories = listOf(Story("0", "Share Story", "", true))
                } else {
                    stories = listOf(Story("0", "Share Story", "", true)) + remoteStories.map { dto ->
                        Story(
                            id = dto.id.toString(),
                            userName = dto.author.full_name,
                            profileImage = dto.author.profile?.avatar_url ?: "",
                            likeCount = dto.like_count,
                            replyCount = dto.reply_count,
                            isLiked = dto.is_liked
                        )
                    }
                }
            } catch (e: Exception) {
                // If stories fail, just show the "Share Story" option
                stories = listOf(Story("0", "Share Story", "", true))
            }
        }
    }

    fun loadFeed() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val remotePosts: List<PostDto> = apiService.getFeed()

                // Only show posts in the feed (events are separate)
                posts = remotePosts.map { dto ->
                    Post(
                        id = dto.id,
                        userName = dto.author.full_name,
                        profileImage = dto.author.profile?.avatar_url ?: "",
                        timeAgo = dto.created_at,
                        content = dto.content,
                        postImage = dto.image_url,
                        likes = dto.like_count,
                        comments = dto.comment_count,
                        isLiked = dto.is_liked,
                        isBookmarked = false,
                        isAuthor = dto.is_author
                    )
                }

            } catch (e: Exception) {
                errorMessage = "Failed to load feed: ${e.message ?: "Unknown error"}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(refreshTrigger) {
        loadFeed()
        loadStories()
    }

    LaunchedEffect(Unit) {
        loadStories()
    }

    Scaffold(
        topBar = {
            HomeTopAppBar(
                onSearchClick = onNavigateToSearch,
                onNotificationsClick = onNavigateToNotifications,
                onMessagesClick = onNavigateToMessages
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(bottom = 80.dp) // Add padding to avoid bottom nav overlap
        ) {
            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = Color.Red,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }

            if (isLoading && posts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    item {
                        PostCreationSection(onCreatePostClick = onNavigateToCreatePost)
                    }

                    item {
                        StoriesSection(stories = stories, onAddStoryClick = onNavigateToCreateStory)
                    }

                    items(posts) { post ->
                        PostItem(
                            post = post,
                            onPostClick = { onNavigateToPostDetail(post.id.toString()) },
                            onRefresh = { loadFeed() }
                        )
                        Divider(
                            color = Color.Gray.copy(alpha = 0.2f),
                            thickness = 8.dp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopAppBar(
    onSearchClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onMessagesClick: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = "CC",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF42A5F5)
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
fun PostCreationSection(onCreatePostClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCreatePostClick)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Gray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "What's on your head?",
                    color = Color.Gray,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                PostCreationActionButton(
                    icon = Icons.Default.Image,
                    text = "Image",
                    color = Color(0xFF4CAF50),
                    onClick = onCreatePostClick
                )
                PostCreationActionButton(
                    icon = Icons.Default.VideoLibrary,
                    text = "Videos",
                    color = Color(0xFFFF9800),
                    onClick = onCreatePostClick
                )
                PostCreationActionButton(
                    icon = Icons.Default.AttachFile,
                    text = "Attach",
                    color = Color(0xFF9C27B0),
                    onClick = onCreatePostClick
                )
            }
        }
    }
}

@Composable
fun PostCreationActionButton(
    icon: ImageVector,
    text: String,
    color: Color,
    onClick: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun StoriesSection(
    stories: List<Story>,
    onAddStoryClick: () -> Unit = {}
) {
    // Only show section if there are stories (should always have at least "Share Story")
    if (stories.isNotEmpty()) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
        ) {
            items(stories) { story ->
                StoryItem(story = story, onAddStoryClick = onAddStoryClick)
            }
        }
    }
}

@Composable
fun StoryItem(
    story: Story,
    onAddStoryClick: () -> Unit = {}
) {
    var isLiked by remember { mutableStateOf(story.isLiked) }
    var likeCount by remember { mutableIntStateOf(story.likeCount) }
    var showStoryViewer by remember { mutableStateOf(false) }
    var storyReplies by remember { mutableStateOf<List<StoryReplyDto>>(emptyList()) }
    var replyText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(70.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(
                    if (story.isAddStory) Color(0xFF42A5F5)
                    else Color.Gray.copy(alpha = 0.3f)
                )
                .clickable(onClick = {
                    if (story.isAddStory) {
                        onAddStoryClick()
                    } else {
                        showStoryViewer = true
                        // Load replies when story is opened
                        scope.launch {
                            try {
                                storyReplies = apiService.getStoryReplies(story.id.toIntOrNull() ?: 0)
                            } catch (e: Exception) {
                                // Handle error silently
                            }
                        }
                    }
                }),
            contentAlignment = Alignment.Center
        ) {
            if (story.isAddStory) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Story",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = Color.Gray,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = story.userName,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = Color.Black
        )

        // Show like count if available and not "Share Story"
        if (!story.isAddStory && likeCount > 0) {
            Text(
                text = "$likeCount ❤️",
                fontSize = 10.sp,
                color = Color.Gray,
                maxLines = 1
            )
        }
    }

    // Story Viewer (Modal Bottom Sheet)
    if (showStoryViewer && !story.isAddStory) {
        @OptIn(ExperimentalMaterial3Api::class)
        ModalBottomSheet(
            onDismissRequest = { showStoryViewer = false },
            modifier = Modifier.fillMaxHeight(0.8f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Story Image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Gray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Story Image",
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Story Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Like Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            val wasLiked = isLiked
                            isLiked = !isLiked
                            likeCount += if (isLiked) 1 else -1
                            scope.launch {
                                try {
                                    if (isLiked) {
                                        apiService.likeStory(story.id.toIntOrNull() ?: 0)
                                    } else {
                                        apiService.unlikeStory(story.id.toIntOrNull() ?: 0)
                                    }
                                } catch (e: Exception) {
                                    // Revert on error
                                    isLiked = wasLiked
                                    likeCount += if (isLiked) -1 else 1
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) Color.Red else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = likeCount.toString(),
                            fontSize = 14.sp,
                            color = if (isLiked) Color.Red else Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Replies Section
                Text(
                    text = "Replies (${storyReplies.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Replies List
                LazyColumn(
                    modifier = Modifier.height(200.dp)
                ) {
                    items(storyReplies) { reply ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = reply.author.full_name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = reply.content,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Text(
                                    text = reply.created_at,
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Reply Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = { Text("Reply to story...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            if (replyText.isNotBlank()) {
                                scope.launch {
                                    try {
                                        val storyReplyRequest = StoryReplyRequest(
                                            story = story.id.toIntOrNull() ?: 0,
                                            content = replyText
                                        )
                                        val created = apiService.createStoryReply(storyReplyRequest)
                                        storyReplies = storyReplies + created
                                        replyText = ""
                                    } catch (e: Exception) {
                                        // Handle error
                                    }
                                }
                            }
                        },
                        enabled = replyText.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send Reply",
                            tint = if (replyText.isNotBlank()) Color(0xFF007AFF) else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PostItem(
    post: Post,
    onPostClick: () -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    var isLiked by remember { mutableStateOf(post.isLiked) }
    var isBookmarked by remember { mutableStateOf(post.isBookmarked) }
    var likeCount by remember { mutableIntStateOf(post.likes) }
    var commentCount by remember { mutableIntStateOf(post.comments) }
    var showMenu by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var shareUrl by remember { mutableStateOf<String?>(null) }
    var showCommentInput by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    var isSubmittingComment by remember { mutableStateOf(false) }
    var commentError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPostClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Post header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = post.userName.first().toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.userName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                    Text(
                        text = post.timeAgo,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                // Show menu only if user is author
                if (post.isAuthor) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = Color.Gray
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    showMenu = false
                                    // Navigate to edit screen
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    showMenu = false
                                    scope.launch {
                                        try {
                                            apiService.deletePost(post.id)
                                            onRefresh()
                                        } catch (e: Exception) {
                                            // Handle error
                                        }
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share") },
                                onClick = {
                                    showMenu = false
                                    scope.launch {
                                        try {
                                            val shareResponse = apiService.getShareUrl(post.id)
                                            shareUrl = shareResponse.share_url
                                            showShareDialog = true
                                            // Copy to clipboard
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Post URL", shareResponse.share_url)
                                            clipboard.setPrimaryClip(clip)
                                        } catch (e: Exception) {
                                            // Handle error
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Post content
            Text(
                text = post.content,
                fontSize = 14.sp,
                color = Color.Black,
                lineHeight = 20.sp
            )

            // Post image (if exists and not empty)
            post.postImage?.takeIf { it.isNotBlank() }?.let { imageUrl ->
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Gray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Post Image",
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Post stats
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "$likeCount likes",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "$commentCount comments",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Divider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)

            Spacer(modifier = Modifier.height(8.dp))

            // Post actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Like button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            onClick = {
                                val wasLiked = isLiked
                                isLiked = !isLiked
                                likeCount += if (isLiked) 1 else -1
                                scope.launch {
                                    try {
                                        if (isLiked) {
                                            apiService.likePost(post.id)
                                        } else {
                                            apiService.unlikePost(post.id)
                                        }
                                    } catch (e: Exception) {
                                        // Revert on error
                                        isLiked = wasLiked
                                        likeCount += if (isLiked) -1 else 1
                                    }
                                }
                            }
                        )
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) Color.Red else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Like",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                // Comment button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = { showCommentInput = !showCommentInput })
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Comment,
                        contentDescription = "Comment",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Comment",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                // Share button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = { /* Handle share */ })
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                // Bookmark button
                Icon(
                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = if (isBookmarked) Color(0xFF42A5F5) else Color.Gray,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(
                            onClick = { isBookmarked = !isBookmarked }
                        )
                )
            }

            // Comment Input Section (shown when comment button is clicked)
            if (showCommentInput) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    // Error message
                    if (commentError != null) {
                        Text(
                            text = commentError ?: "",
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Comment Input Field
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { 
                                commentText = it
                                commentError = null // Clear error when user types
                            },
                            placeholder = { Text("Write a comment...", fontSize = 14.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = false,
                            maxLines = 3,
                            enabled = !isSubmittingComment,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF007AFF),
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )

                        // Send Button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (commentText.isNotBlank() && !isSubmittingComment)
                                        Color(0xFF007AFF)
                                    else
                                        Color.Gray.copy(alpha = 0.3f)
                                )
                                .clickable(
                                    enabled = commentText.isNotBlank() && !isSubmittingComment,
                                    onClick = {
                                        if (commentText.isNotBlank() && !isSubmittingComment) {
                                            commentError = null
                                            isSubmittingComment = true
                                            scope.launch {
                                                try {
                                                    val commentRequest = CommentRequest(
                                                        post = post.id,
                                                        content = commentText.trim()
                                                    )
                                                    val created = apiService.createComment(commentRequest)
                                                    // Success - clear input and update comment count
                                                    commentText = ""
                                                    commentCount += 1
                                                    showCommentInput = false
                                                    commentError = null
                                                    // Refresh the feed to show the new comment count
                                                    onRefresh()
                                                } catch (e: Exception) {
                                                    // Error - keep the input visible so user can retry
                                                    commentError = "Failed to post comment: ${e.message ?: "Unknown error"}"
                                                    android.util.Log.e("PostItem", "Error creating comment", e)
                                                } finally {
                                                    isSubmittingComment = false
                                                }
                                            }
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                        if (isSubmittingComment) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send Comment",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
        }
    }
}


@Composable
fun BottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onProfileClick: () -> Unit = {},
    onEventMapClick: () -> Unit = {},
    onCreatePostClick: () -> Unit = {},
    onFriendsClick: () -> Unit = {}
) {
    val items = listOf(
        BottomNavItem(Icons.Default.Home, Icons.Outlined.Home, "Home"),
        BottomNavItem(Icons.Default.Group, Icons.Outlined.Group, "Friends"),
        BottomNavItem(Icons.Default.Add, Icons.Outlined.Add, "Create"),
        BottomNavItem(Icons.Default.LocationOn, Icons.Outlined.LocationOn, "Events"),
        BottomNavItem(Icons.Default.Person, Icons.Outlined.Person, "Profile")
    )

    // Floating bottom navigation bar
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            NavigationBar(
                containerColor = Color.Transparent,
                modifier = Modifier.height(60.dp)
            ) {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == index) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        selected = selectedTab == index,
                        onClick = {
                            onTabSelected(index)
                            when (index) {
                                0 -> { /* Home - already on home */ }
                                1 -> onFriendsClick()
                                2 -> onCreatePostClick()
                                3 -> onEventMapClick()
                                4 -> onProfileClick()
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            unselectedIconColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    }
}
