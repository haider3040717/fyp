package com.example.myapplication.postdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.myapplication.data.remote.CommentDto
import com.example.myapplication.data.remote.PostDto
import com.example.myapplication.data.remote.apiService
import com.example.myapplication.data.remote.SessionManager
import com.example.myapplication.data.remote.ShareResponse
import kotlinx.coroutines.launch
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.platform.LocalContext

// Data classes
data class PostDetail(
    val id: Int,
    val userName: String,
    val userSeatNo: String,
    val userDepartment: String,
    val timeAgo: String,
    val content: String,
    val postImage: String? = null,
    val likes: Int,
    val comments: Int,
    val shares: Int,
    val isLiked: Boolean = false,
    val isBookmarked: Boolean = false,
    val userInitials: String = ""
)

data class Comment(
    val id: Int,
    val userName: String,
    val userInitials: String,
    val content: String,
    val timeAgo: String,
    val likes: Int,
    val replies: Int,
    val isLiked: Boolean = false,
    val userDepartment: String = "",
    val isAuthor: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    onNavigateBack: () -> Unit,
    postId: String = "1"
) {
    val parsedId = postId.toIntOrNull() ?: 0

    var commentText by remember { mutableStateOf("") }
    var isLiked by remember { mutableStateOf(false) }
    var isBookmarked by remember { mutableStateOf(false) }
    var likeCount by remember { mutableIntStateOf(0) }
    var commentCount by remember { mutableIntStateOf(0) }
    var post by remember { mutableStateOf<PostDetail?>(null) }
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPostAuthor by remember { mutableStateOf(false) }
    var showPostMenu by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var shareUrl by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(parsedId) {
        if (parsedId == 0) return@LaunchedEffect
        isLoading = true
        errorMessage = null
        try {
            val dto: PostDto = apiService.getPost(parsedId)
            post = PostDetail(
                id = dto.id,
                userName = dto.author.full_name,
                userSeatNo = dto.author.seat_number,
                userDepartment = dto.author.department ?: "",
                timeAgo = dto.created_at,
                content = dto.content,
                postImage = dto.image_url,
                likes = dto.like_count,
                comments = dto.comment_count,
                shares = 0,
                userInitials = dto.author.full_name.split(" ").mapNotNull { it.firstOrNull()?.toString() }
                    .joinToString("")
            )
            likeCount = dto.like_count
            commentCount = dto.comment_count
            isLiked = dto.is_liked
            isPostAuthor = dto.is_author

            val commentDtos: List<CommentDto> = apiService.getComments(parsedId)
            comments = commentDtos.map { c ->
                Comment(
                    id = c.id,
                    userName = c.author.full_name,
                    userInitials = c.author.full_name.split(" ").mapNotNull { it.firstOrNull()?.toString() }
                        .joinToString(""),
                    content = c.content,
                    timeAgo = c.created_at,
                    likes = c.like_count,
                    replies = 0,
                    userDepartment = c.author.department ?: ""
                )
            }
        } catch (e: Exception) {
            errorMessage = "Failed to load post: ${e.message ?: "Unknown error"}"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            PostDetailTopAppBar(onNavigateBack = onNavigateBack)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFAFAFA))
        ) {
            if (errorMessage != null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = errorMessage ?: "", color = Color.Red)
                }
            } else if (isLoading && post == null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    post?.let { p ->
                        // Post Content
                        item {
                            PostDetailCard(
                                post = p,
                                isLiked = isLiked,
                                isBookmarked = isBookmarked,
                                likeCount = likeCount,
                                isPostAuthor = isPostAuthor,
                                onLikeClick = {
                                    isLiked = !isLiked
                                    likeCount += if (isLiked) 1 else -1
                                    scope.launch {
                                        try {
                                            if (isLiked) {
                                                apiService.likePost(parsedId)
                                            } else {
                                                apiService.unlikePost(parsedId)
                                            }
                                        } catch (_: Exception) {
                                        }
                                    }
                                },
                                onBookmarkClick = { isBookmarked = !isBookmarked },
                                onShareClick = {
                                    scope.launch {
                                        try {
                                            val shareResponse = apiService.getShareUrl(parsedId)
                                            shareUrl = shareResponse.share_url
                                            showShareDialog = true
                                            // Copy to clipboard
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Post URL", shareResponse.share_url)
                                            clipboard.setPrimaryClip(clip)
                                        } catch (e: Exception) {
                                            errorMessage = "Failed to share: ${e.message ?: "Unknown error"}"
                                        }
                                    }
                                },
                                onDeleteClick = {
                                    scope.launch {
                                        try {
                                            apiService.deletePost(parsedId)
                                            onNavigateBack()
                                        } catch (e: Exception) {
                                            errorMessage = "Failed to delete post: ${e.message ?: "Unknown error"}"
                                        }
                                    }
                                }
                            )
                        }
                    }

                    // Comments Header
                    item {
                        CommentsHeaderSection(commentCount = commentCount)
                    }

                    // Comments List
                    items(comments) { comment ->
                        CommentItem(
                            comment = comment,
                            onDelete = {
                                scope.launch {
                                    try {
                                        apiService.deleteComment(comment.id)
                                        // Reload comments
                                        val commentDtos: List<CommentDto> = apiService.getComments(parsedId)
                                        comments = commentDtos.map { c ->
                                            Comment(
                                                id = c.id,
                                                userName = c.author.full_name,
                                                userInitials = c.author.full_name.split(" ").mapNotNull { it.firstOrNull()?.toString() }
                                                    .joinToString(""),
                                                content = c.content,
                                                timeAgo = c.created_at,
                                                likes = c.like_count,
                                                replies = 0,
                                                isLiked = false,
                                                userDepartment = c.author.department ?: "",
                                                isAuthor = c.author.id == SessionManager.currentUserId
                                            )
                                        }
                                        commentCount = comments.size
                                    } catch (e: Exception) {
                                        errorMessage = "Failed to delete comment: ${e.message ?: "Unknown error"}"
                                    }
                                }
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            // Share Dialog
            if (showShareDialog && shareUrl != null) {
                AlertDialog(
                    onDismissRequest = { showShareDialog = false },
                    title = { Text("Share Post") },
                    text = {
                        Column {
                            Text("Post URL copied to clipboard:")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = shareUrl ?: "",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showShareDialog = false }) {
                            Text("OK")
                        }
                    }
                )
            }

            // Comment Input Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // User Avatar
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFB3E5FC)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "H",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Comment Input
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFAFAFA), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        BasicTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(
                                fontSize = 14.sp,
                                color = Color.Black
                            ),
                            decorationBox = { innerTextField ->
                                if (commentText.isEmpty()) {
                                    Text(
                                        text = "Write a comment...",
                                        color = Color.Gray,
                                        fontSize = 14.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }

                    // Send Button
                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank() && parsedId != 0) {
                                scope.launch {
                                    try {
                                        val created = apiService.createComment(
                                            mapOf(
                                                "post" to parsedId,
                                                "content" to commentText
                                            )
                                        )
                                        comments = comments + Comment(
                                            id = created.id,
                                            userName = created.author.full_name,
                                            userInitials = created.author.full_name.split(" ")
                                                .mapNotNull { it.firstOrNull()?.toString() }
                                                .joinToString(""),
                                            content = created.content,
                                            timeAgo = created.created_at,
                                            likes = created.like_count,
                                            replies = 0,
                                            isLiked = false,
                                            userDepartment = created.author.department ?: "",
                                            isAuthor = created.author.id == SessionManager.currentUserId
                                        )
                                        commentText = ""
                                        commentCount += 1
                                    } catch (_: Exception) {
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF007AFF)),
                        enabled = commentText.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailTopAppBar(
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Post",
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
            IconButton(onClick = { /* More options */ }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More",
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
fun PostDetailCard(
    post: PostDetail,
    isLiked: Boolean,
    isBookmarked: Boolean,
    likeCount: Int,
    isPostAuthor: Boolean = false,
    onLikeClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit = {}
) {
    var showPostMenu by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Post Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFB3E5FC)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = post.userInitials,
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
                        text = post.userSeatNo,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${post.userDepartment} • ${post.timeAgo}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                if (isPostAuthor) {
                    Box {
                        IconButton(onClick = { showPostMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = Color.Gray
                            )
                        }
                        DropdownMenu(
                            expanded = showPostMenu,
                            onDismissRequest = { showPostMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    showPostMenu = false
                                    // TODO: Navigate to edit screen
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    showPostMenu = false
                                    onDeleteClick()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Post Content
            Text(
                text = post.content,
                fontSize = 15.sp,
                color = Color.Black,
                lineHeight = 22.sp
            )

            // Post Image
            post.postImage?.let {
                if (it.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .clip(RoundedCornerShape(12.dp))
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
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Post Stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Likes",
                        tint = Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$likeCount likes",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${post.comments} comments",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${post.shares} shares",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalDivider(
                color = Color.Gray.copy(alpha = 0.2f),
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Post Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Like Button
                Button(
                    onClick = onLikeClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLiked) Color(0xFFFFE0E0) else Color(0xFAFAFA)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) Color.Red else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Like",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isLiked) Color.Red else Color.Gray
                    )
                }

                // Comment Button
                Button(
                    onClick = { /* Scroll to comment input */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFAFAFA)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Comment,
                        contentDescription = "Comment",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Comment",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                }

                // Share Button
                Button(
                    onClick = onShareClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFAFAFA)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Share",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                }

                // Bookmark Button
                IconButton(
                    onClick = onBookmarkClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFAFAFA))
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) Color(0xFF007AFF) else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CommentsHeaderSection(commentCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Comments ($commentCount)",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Most Relevant",
            fontSize = 13.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    onDelete: () -> Unit = {}
) {
    var isCommentLiked by remember { mutableStateOf(comment.isLiked) }
    var commentLikeCount by remember { mutableIntStateOf(comment.likes) }
    var showMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Comment Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFB3E5FC)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = comment.userInitials,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = comment.userName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                    Text(
                        text = "${comment.userDepartment} • ${comment.timeAgo}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                if (comment.isAuthor) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Comment Content
            Text(
                text = comment.content,
                fontSize = 14.sp,
                color = Color.Black,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 50.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Comment Actions
            Row(
                modifier = Modifier
                    .padding(start = 50.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        val wasLiked = isCommentLiked
                        isCommentLiked = !isCommentLiked
                        commentLikeCount += if (isCommentLiked) 1 else -1
                        scope.launch {
                            try {
                                if (isCommentLiked) {
                                    apiService.likeComment(comment.id)
                                } else {
                                    apiService.unlikeComment(comment.id)
                                }
                            } catch (e: Exception) {
                                // Revert on error
                                isCommentLiked = wasLiked
                                commentLikeCount += if (isCommentLiked) -1 else 1
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isCommentLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isCommentLiked) Color.Red else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (commentLikeCount > 0) commentLikeCount.toString() else "Like",
                        fontSize = 12.sp,
                        color = if (isCommentLiked) Color.Red else Color.Gray,
                        fontWeight = if (isCommentLiked) FontWeight.Medium else FontWeight.Normal
                    )
                }

                // Reply
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { /* Handle reply */ }
                ) {
                    Icon(
                        imageVector = Icons.Default.Reply,
                        contentDescription = "Reply",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Reply",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                // Replies count
                if (comment.replies > 0) {
                    Text(
                        text = "${comment.replies} replies",
                        fontSize = 12.sp,
                        color = Color(0xFF007AFF),
                        modifier = Modifier.clickable { /* Show replies */ }
                    )
                }
            }
        }
    }
}
