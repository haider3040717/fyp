package com.example.myapplication.eventmap

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
import com.example.myapplication.data.remote.EventDto
import com.example.myapplication.data.remote.apiService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Data classes for events
data class CampusEvent(
    val id: String,
    val title: String,
    val location: String,
    val date: String,
    val time: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val isInterested: Boolean = false,
    val isGoing: Boolean = false,
    val interestedCount: Int = 0,
    val goingCount: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventMapScreen(
    onNavigateBack: () -> Unit
) {
    var selectedEvent by remember { mutableStateOf<CampusEvent?>(null) }
    var campusEvents by remember { mutableStateOf<List<CampusEvent>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun loadEvents() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val remoteEvents: List<EventDto> = apiService.getEvents()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                val displayDateFormat = SimpleDateFormat("d MMM", Locale.getDefault())
                
                val newEvents = remoteEvents.map { dto ->
                    try {
                        val startDate = dateFormat.parse(dto.start_date) ?: Date()
                        val endDate = dateFormat.parse(dto.end_date) ?: Date()
                        CampusEvent(
                            id = dto.id.toString(),
                            title = dto.title,
                            location = dto.location,
                            date = displayDateFormat.format(startDate),
                            time = "${timeFormat.format(startDate)} - ${timeFormat.format(endDate)}",
                            description = dto.description,
                            latitude = dto.latitude.toDouble(),
                            longitude = dto.longitude.toDouble(),
                            interestedCount = dto.interested_count,
                            goingCount = dto.going_count,
                            isInterested = dto.is_interested,
                            isGoing = dto.is_going
                        )
                    } catch (e: Exception) {
                        CampusEvent(
                            id = dto.id.toString(),
                            title = dto.title,
                            location = dto.location,
                            date = dto.start_date,
                            time = dto.end_date,
                            description = dto.description,
                            latitude = dto.latitude.toDouble(),
                            longitude = dto.longitude.toDouble(),
                            interestedCount = dto.interested_count,
                            goingCount = dto.going_count,
                            isInterested = dto.is_interested,
                            isGoing = dto.is_going
                        )
                    }
                }
                campusEvents = newEvents
                // Update selectedEvent if it exists to reflect latest data
                selectedEvent?.let { currentSelected ->
                    selectedEvent = newEvents.find { it.id == currentSelected.id }
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load events: ${e.message ?: "Unknown error"}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadEvents()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Event Map") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Map Section (Placeholder with Event Markers Info)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFE8F5E8)),
                    contentAlignment = Alignment.Center
                ) {
                    if (campusEvents.isNotEmpty()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Map",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Campus Event Map",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF2E7D32)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${campusEvents.size} event(s) on map",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Click events below to view details",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Map",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Campus Event Map",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF2E7D32)
                            )
                            Text(
                                text = "No events available",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            // Events List
            Text(
                text = "Upcoming Events",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

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
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = errorMessage ?: "Error loading events",
                                color = Color.Red,
                                modifier = Modifier.padding(16.dp)
                            )
                            Button(onClick = { loadEvents() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(all = 16.dp)
                    ) {
                        items(campusEvents) { event ->
                            EventCard(
                                event = event,
                                onEventClick = { selectedEvent = event },
                                onInterestClick = {
                                    scope.launch {
                                        try {
                                            if (event.isInterested) {
                                                apiService.markEventUninterested(event.id.toInt())
                                            } else {
                                                apiService.markEventInterested(event.id.toInt())
                                            }
                                            // Update local state immediately for better UX
                                            campusEvents = campusEvents.map { e ->
                                                if (e.id == event.id) {
                                                    e.copy(
                                                        isInterested = !e.isInterested,
                                                        interestedCount = if (e.isInterested) e.interestedCount - 1 else e.interestedCount + 1
                                                    )
                                                } else e
                                            }
                                            loadEvents() // Reload to sync with backend
                                        } catch (e: Exception) {
                                            // Handle error - revert on failure
                                            loadEvents()
                                        }
                                    }
                                },
                                onGoingClick = {
                                    scope.launch {
                                        try {
                                            val wasGoing = event.isGoing
                                            apiService.markEventGoing(event.id.toInt())
                                            // Update local state immediately
                                            campusEvents = campusEvents.map { e ->
                                                if (e.id == event.id) {
                                                    e.copy(
                                                        isGoing = !wasGoing,
                                                        goingCount = if (wasGoing) e.goingCount - 1 else e.goingCount + 1
                                                    )
                                                } else e
                                            }
                                            loadEvents() // Reload to sync with backend
                                        } catch (e: Exception) {
                                            // Handle error - revert on failure
                                            loadEvents()
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

    // Event Detail Bottom Sheet
    selectedEvent?.let { event ->
        EventDetailBottomSheet(
            event = event,
            onDismiss = { selectedEvent = null },
            onInterestClick = {
                scope.launch {
                    try {
                        val wasInterested = event.isInterested
                        if (wasInterested) {
                            apiService.markEventUninterested(event.id.toInt())
                        } else {
                            apiService.markEventInterested(event.id.toInt())
                        }
                        // Update local state immediately for better UX
                        val updatedEvents = campusEvents.map { e ->
                            if (e.id == event.id) {
                                e.copy(
                                    isInterested = !wasInterested,
                                    interestedCount = if (wasInterested) e.interestedCount - 1 else e.interestedCount + 1
                                )
                            } else e
                        }
                        campusEvents = updatedEvents
                        selectedEvent = updatedEvents.find { it.id == event.id }
                        // Reload to sync with backend (this will update selectedEvent again)
                        loadEvents()
                    } catch (e: Exception) {
                        // Handle error
                        loadEvents()
                    }
                }
            },
            onGoingClick = {
                scope.launch {
                    try {
                        val wasGoing = event.isGoing
                        apiService.markEventGoing(event.id.toInt())
                        // Update local state immediately for better UX
                        val updatedEvents = campusEvents.map { e ->
                            if (e.id == event.id) {
                                e.copy(
                                    isGoing = !wasGoing,
                                    goingCount = if (wasGoing) e.goingCount - 1 else e.goingCount + 1
                                )
                            } else e
                        }
                        campusEvents = updatedEvents
                        selectedEvent = updatedEvents.find { it.id == event.id }
                        // Reload to sync with backend (this will update selectedEvent again)
                        loadEvents()
                    } catch (e: Exception) {
                        // Handle error
                        loadEvents()
                    }
                }
            }
        )
    }
}

@Composable
fun EventCard(
    event: CampusEvent,
    onEventClick: () -> Unit,
    onInterestClick: () -> Unit,
    onGoingClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEventClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top
            ) {
                // Event Icon
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF007AFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = "Event",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = event.location,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Date",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${event.date} • ${event.time}",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = event.description,
                fontSize = 14.sp,
                color = Color.Black,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onInterestClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (event.isInterested) Color(0xFF007AFF) else Color(0xFFE5E5EA),
                        contentColor = if (event.isInterested) Color.White else Color.Black
                    )
                ) {
                    Text("Interested (${event.interestedCount})")
                }

                Button(
                    onClick = onGoingClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (event.isGoing) Color(0xFF4CAF50) else Color(0xFFE5E5EA),
                        contentColor = if (event.isGoing) Color.White else Color.Black
                    )
                ) {
                    Text("Going (${event.goingCount})")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailBottomSheet(
    event: CampusEvent,
    onDismiss: () -> Unit,
    onInterestClick: () -> Unit,
    onGoingClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Map placeholder for specific event
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFE3F2FD)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = event.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF007AFF)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Event Details
            Text(
                text = event.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = event.location,
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Date",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = event.date,
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = "Time",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = event.time,
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = event.description,
                fontSize = 14.sp,
                color = Color.Black,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onInterestClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (event.isInterested) Color(0xFF007AFF) else Color(0xFFE5E5EA),
                        contentColor = if (event.isInterested) Color.White else Color.Black
                    )
                ) {
                    Text("Interested (${event.interestedCount})")
                }

                Button(
                    onClick = onGoingClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (event.isGoing) Color(0xFF4CAF50) else Color(0xFFE5E5EA),
                        contentColor = if (event.isGoing) Color.White else Color.Black
                    )
                ) {
                    Text("Going (${event.goingCount})")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
