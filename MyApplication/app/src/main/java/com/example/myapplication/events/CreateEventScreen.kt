package com.example.myapplication.events

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.remote.EventRequest
import com.example.myapplication.data.remote.apiService
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    onNavigateBack: () -> Unit = {},
    onEventCreated: () -> Unit = {}
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }

    var isCreating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    fun createEvent() {
        if (title.isBlank() || description.isBlank() || location.isBlank()) {
            errorMessage = "Please fill in all required fields"
            return
        }

        scope.launch {
            isCreating = true
            errorMessage = null
            try {
                // Parse date and time
                val startDateTime = try {
                    val dateTimeStr = "$startDate $startTime"
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    LocalDateTime.parse(dateTimeStr, formatter)
                } catch (e: Exception) {
                    LocalDateTime.now()
                }

                val endDateTime = try {
                    val dateTimeStr = "$endDate $endTime"
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    LocalDateTime.parse(dateTimeStr, formatter)
                } catch (e: Exception) {
                    startDateTime.plusHours(2)
                }

                val eventRequest = EventRequest(
                    title = title.trim(),
                    description = description.trim(),
                    location = location.trim(),
                    start_date = startDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z",
                    end_date = endDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z"
                )

                val createdEvent = apiService.createEvent(eventRequest)
                successMessage = "Event created successfully!"

                // Clear form
                title = ""
                description = ""
                location = ""
                startDate = ""
                startTime = ""
                endDate = ""
                endTime = ""

                onEventCreated() 

            } catch (e: Exception) {
                errorMessage = "Failed to create event: ${e.message ?: "Unknown error"}"
            } finally {
                isCreating = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Create Event",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { createEvent() },
                        enabled = !isCreating && title.isNotBlank(),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF007AFF)
                            )
                        } else {
                            Text(
                                text = "Create",
                                color = if (title.isNotBlank()) Color(0xFF007AFF) else Color.Gray,
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Success message
            if (successMessage != null) {
                Card(
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
            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color.Red,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Title field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Event Title *") },
                placeholder = { Text("Enter event title") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )

            // Description field
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description *") },
                placeholder = { Text("Describe your event") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4
            )

            // Location field
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location *") },
                placeholder = { Text("Where will the event take place?") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )


            // Start Date & Time
            Text(
                text = "Start Date & Time",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Date") },
                    placeholder = { Text("2024-01-25") },
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = { Text("Time") },
                    placeholder = { Text("14:00") },
                    modifier = Modifier.weight(1f)
                )
            }

            // End Date & Time
            Text(
                text = "End Date & Time",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("Date") },
                    placeholder = { Text("2024-01-25") },
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = endTime,
                    onValueChange = { endTime = it },
                    label = { Text("Time") },
                    placeholder = { Text("16:00") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info text
            Text(
                text = "* Required fields",
                fontSize = 12.sp,
                color = Color.Gray
            )

            Text(
                text = "Date format: YYYY-MM-DD\nTime format: HH:MM (24-hour)",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}