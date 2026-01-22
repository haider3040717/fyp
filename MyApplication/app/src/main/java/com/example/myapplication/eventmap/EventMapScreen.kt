package com.example.myapplication.eventmap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    var showMapView by remember { mutableStateOf(true) }
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
                title = { Text(if (showMapView) "Event Map" else "Events") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMapView = !showMapView }) {
                        Icon(
                            if (showMapView) Icons.Default.List else Icons.Default.Map,
                            contentDescription = if (showMapView) "List View" else "Map View"
                        )
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
            if (showMapView) {
                // Map View
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFE8F5E8)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Mock University of Karachi Map
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "🏛️ University of Karachi",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Pakistan",
                                fontSize = 14.sp,
                                color = Color(0xFF4CAF50)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Campus layout with event markers
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                                    .background(Color(0xFFE8F5E8), RoundedCornerShape(8.dp))
                                    .border(2.dp, Color(0xFF4CAF50), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                // Campus map layout
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceEvenly,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Top row - UBIT and Arts Department
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(70.dp)
                                                .background(Color(0xFF1976D2), RoundedCornerShape(4.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("🏢", fontSize = 16.sp)
                                                Text("UBIT", fontSize = 8.sp, color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(70.dp)
                                                .background(Color(0xFFFF9800), RoundedCornerShape(4.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("🎨", fontSize = 16.sp)
                                                Text("Arts", fontSize = 8.sp, color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                            }
                                        }
                                    }

                                    // Middle row - Commerce and Education
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(70.dp)
                                                .background(Color(0xFF4CAF50), RoundedCornerShape(4.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("💼", fontSize = 16.sp)
                                                Text("Commerce", fontSize = 7.sp, color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(70.dp)
                                                .background(Color(0xFF9C27B0), RoundedCornerShape(4.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("📚", fontSize = 16.sp)
                                                Text("Education", fontSize = 7.sp, color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                            }
                                        }
                                    }

                                    // Bottom row - IBA
                                    Box(
                                        modifier = Modifier
                                            .size(70.dp)
                                            .background(Color(0xFFF44336), RoundedCornerShape(4.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("🏦", fontSize = 16.sp)
                                            Text("IBA", fontSize = 8.sp, color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                        }
                                    }
                                }

                                // Event markers overlaid on map
                                if (campusEvents.isNotEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        campusEvents.forEachIndexed { index, event ->
                                            // Position markers near different buildings
                                            val positions = listOf(
                                                // UBIT (top left)
                                                Triple(Alignment.TopStart, 20.dp, 40.dp),
                                                // Arts Department (top right)
                                                Triple(Alignment.TopEnd, (-20).dp, 40.dp),
                                                // Commerce Department (middle left)
                                                Triple(Alignment.CenterStart, 20.dp, 0.dp),
                                                // Education Department (middle right)
                                                Triple(Alignment.CenterEnd, (-20).dp, 0.dp),
                                                // IBA (bottom center)
                                                Triple(Alignment.BottomCenter, 0.dp, (-20).dp)
                                            )
                                            val pos = positions.getOrNull(index % positions.size)
                                            if (pos != null) {
                                                val (alignment, offsetX, offsetY) = pos
                                                Box(
                                                    modifier = Modifier
                                                        .align(alignment)
                                                        .offset(x = offsetX, y = offsetY)
                                                        .size(28.dp)
                                                        .background(Color.Red, CircleShape)
                                                        .clickable { selectedEvent = event },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "📍",
                                                        fontSize = 14.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${campusEvents.size} event${if (campusEvents.size != 1) "s" else ""} marked on campus",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            // Events List (shown in both map and list view)
            if (!showMapView || true) { // Always show list, or only in list view
                Text(
                    text = if (showMapView) "Event Details" else "Upcoming Events",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (errorMessage != null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = errorMessage ?: "Error",
                            color = Color.Red
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(campusEvents) { event ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedEvent = event },
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = event.title,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${event.location} • ${event.date}",
                                        fontSize = 14.sp,
                                        color = Color.Gray
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = event.description,
                                        fontSize = 14.sp,
                                        color = Color.Black,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Event Details Modal
    if (selectedEvent != null) {
        AlertDialog(
            onDismissRequest = { selectedEvent = null },
            title = { Text(selectedEvent?.title ?: "") },
            text = {
                Column {
                    Text("Location: ${selectedEvent?.location}")
                    Text("Date: ${selectedEvent?.date}")
                    Text("Time: ${selectedEvent?.time}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(selectedEvent?.description ?: "")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        // Delete event
                        scope.launch {
                            try {
                                selectedEvent?.let { event ->
                                    apiService.deleteEvent(event.id.toInt())
                                    // Remove from list and close modal
                                    campusEvents = campusEvents.filter { it.id != event.id }
                                    selectedEvent = null
                                }
                            } catch (e: Exception) {
                                // Handle error - could show a snackbar
                            }
                        }
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedEvent = null }) {
                    Text("Close")
                }
            }
        )
    }
}