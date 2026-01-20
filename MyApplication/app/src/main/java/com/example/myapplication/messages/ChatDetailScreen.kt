package com.example.myapplication.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import com.example.myapplication.data.remote.MessageDto
import com.example.myapplication.data.remote.SessionManager
import com.example.myapplication.data.remote.apiService
import kotlinx.coroutines.launch

// Data class for individual messages
data class Message(
    val id: Int,
    val senderName: String,
    val text: String,
    val timestamp: String,
    val isSent: Boolean, // true if current user sent it
    val isRead: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chatId: String,
    chatName: String,
    isOnline: Boolean = true,
    onNavigateBack: () -> Unit = {}
) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val convId = chatId.toIntOrNull() ?: 0

    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadMessages() {
        if (convId == 0) return
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val remote: List<MessageDto> = apiService.getMessages(convId)
                val currentUserId = SessionManager.currentUserId
                val newMessages = remote.map { m ->
                    Message(
                        id = m.id,
                        senderName = m.sender.full_name,
                        text = m.text,
                        timestamp = m.created_at,
                        isSent = currentUserId != null && m.sender.id == currentUserId,
                        isRead = m.is_read
                    )
                }
                messages = newMessages
                // Auto-scroll to bottom after messages are loaded
                if (newMessages.isNotEmpty()) {
                    kotlinx.coroutines.delay(100)
                    listState.animateScrollToItem(newMessages.size - 1)
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load messages: ${e.message ?: "Unknown error"}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(convId) {
        loadMessages()
    }

    // Auto-scroll when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            kotlinx.coroutines.delay(50) // Small delay to ensure list is rendered
            try {
                listState.animateScrollToItem(messages.size - 1)
            } catch (e: Exception) {
                // Fallback to instant scroll if animate fails
                try {
                    listState.scrollToItem(messages.size - 1)
                } catch (e2: Exception) {
                    // Ignore if scroll fails (list might be empty or not ready)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            ChatDetailTopAppBar(
                chatName = chatName,
                isOnline = isOnline,
                onNavigateBack = onNavigateBack,
                onCallClick = { /* TODO: Add call functionality */ },
                onInfoClick = { /* TODO: Add chat info */ }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            // Messages List
            if (errorMessage != null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = errorMessage ?: "", color = Color.Red)
                }
            } else if (isLoading && messages.isEmpty()) {
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
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = messages,
                        key = { it.id }
                    ) { message ->
                        if (message.isSent) {
                            SentMessageBubble(message = message)
                        } else {
                            ReceivedMessageBubble(message = message)
                        }
                    }
                }
            }

            // Message Input Area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFAFAFA)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Attachment Button
                    IconButton(
                        onClick = { /* TODO: Add attachments */ },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attach",
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Text Input Field
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.White, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        BasicTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                color = Color.Black
                            ),
                            decorationBox = { innerTextField ->
                                if (messageText.isEmpty()) {
                                    Text(
                                        text = "Type a message...",
                                        color = Color.Gray,
                                        fontSize = 16.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }

                    // Send Button
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank() && convId != 0) {
                                val textToSend = messageText.trim()
                                messageText = "" // Clear immediately for better UX
                                scope.launch {
                                    try {
                                        val created = apiService.sendMessage(
                                            convId,
                                            mapOf("text" to textToSend)
                                        )
                                        val currentUserId = SessionManager.currentUserId
                                        val newMessage = Message(
                                            id = created.id,
                                            senderName = created.sender.full_name,
                                            text = created.text,
                                            timestamp = created.created_at,
                                            isSent = currentUserId != null && created.sender.id == currentUserId,
                                            isRead = created.is_read
                                        )
                                        messages = messages + newMessage
                                        // Auto-scroll to bottom after sending
                                        kotlinx.coroutines.delay(100)
                                        if (messages.isNotEmpty()) {
                                            try {
                                                listState.animateScrollToItem(messages.size - 1)
                                            } catch (e: Exception) {
                                                // Fallback to instant scroll
                                                try {
                                                    listState.scrollToItem(messages.size - 1)
                                                } catch (e2: Exception) {
                                                    // Ignore if scroll fails
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        errorMessage =
                                            "Failed to send message: ${e.message ?: "Unknown error"}"
                                        messageText = textToSend // Restore text on error
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF007AFF)),
                        enabled = messageText.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailTopAppBar(
    chatName: String,
    isOnline: Boolean,
    onNavigateBack: () -> Unit,
    onCallClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = chatName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = if (isOnline) "Active now" else "Offline",
                    fontSize = 12.sp,
                    color = if (isOnline) Color(0xFF4CAF50) else Color.Gray
                )
            }
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
            IconButton(onClick = onCallClick) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call",
                    tint = Color(0xFF007AFF)
                )
            }
            IconButton(onClick = onInfoClick) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
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
fun SentMessageBubble(message: Message) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 4.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF007AFF)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.text,
                    color = Color.White,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = message.timestamp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    if (message.isRead) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = "Read",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReceivedMessageBubble(message: Message) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8E8E8)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.text,
                    color = Color.Black,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.timestamp,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
