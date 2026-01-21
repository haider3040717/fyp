package com.example.myapplication.data.remote

import android.os.Build

// Backend base URL:
// - Emulator: use http://10.0.2.2:8000/
// - Physical device: use your PC's LAN IP (make sure phone and PC are on same WiFi)
// This function detects if running on emulator and uses appropriate URL
fun getBaseUrl(): String {
    val isEmulator = Build.FINGERPRINT.startsWith("generic")
            || Build.FINGERPRINT.startsWith("unknown")
            || Build.MODEL.contains("google_sdk")
            || Build.MODEL.contains("Emulator")
            || Build.MODEL.contains("Android SDK built for x86")
            || Build.MANUFACTURER.contains("Genymotion")
            || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
            || "google_sdk" == Build.PRODUCT
    
    return if (isEmulator) {
        "http://10.0.2.2:8000/"
    } else {
        // For physical device, use your PC's LAN IP (same network as device)
        // Backend must be running on 0.0.0.0:8000 (not 127.0.0.1:8000) to accept connections
        "http://192.168.1.102:8000/"
    }
}

val BASE_URL = getBaseUrl()

// --- Auth ---

data class LoginRequest(
    val seat_number: String,
    val password: String
)

data class RegisterRequest(
    val seat_number: String,
    val full_name: String,
    val password: String,
    val university_email: String?
)

data class CommentRequest(
    val post: Int,
    val content: String
)

data class PostRequest(
    val content: String,
    val image_url: String? = null
)

data class StoryReplyRequest(
    val story: Int,
    val content: String
)

data class ProfileDto(
    val bio: String? = null,
    val avatar_url: String? = null,
    val course: String? = null,
    val interests: String? = null
)

data class UserDto(
    val id: Int,
    val seat_number: String,
    val full_name: String,
    val university_email: String?,
    val year: Int?,
    val department: String?,
    val profile: ProfileDto?,
    val posts_count: Int = 0,
    val friends_count: Int = 0
)

data class LoginResponse(
    val access: String,
    val refresh: String,
    val user: UserDto
)

// --- Posts & Comments ---

data class PostDto(
    val id: Int,
    val author: UserDto,
    val content: String,
    val image_url: String?,
    val event_id: Int? = null,
    val created_at: String,
    val updated_at: String,
    val like_count: Int,
    val comment_count: Int,
    val is_liked: Boolean,
    val is_author: Boolean = false
)

data class ShareResponse(
    val share_url: String,
    val post_id: Int? = null
)

data class ShareUrlResponse(
    val share_url: String
)

data class CommentDto(
    val id: Int,
    val post: Int,
    val author: UserDto,
    val content: String,
    val created_at: String,
    val parent: Int?,
    val like_count: Int
)

// --- Messaging ---

data class ConversationDto(
    val id: Int,
    val other_user: UserDto,
    val last_message: MessageDto?,
    val unread_count: Int
)

data class MessageDto(
    val id: Int,
    val conversation: Int,
    val sender: UserDto,
    val text: String,
    val created_at: String,
    val is_read: Boolean
)

data class StartConversationRequest(
    val user_id: Int
)

// --- Notifications ---

data class NotificationDto(
    val id: Int,
    val type: String,
    val text: String,
    val actor: UserDto?,
    val related_object_id: Int? = null,
    val is_read: Boolean,
    val created_at: String
)

// --- Events ---

data class EventDto(
    val id: Int,
    val title: String,
    val description: String,
    val location: String,
    val latitude: String, // Decimal as String from JSON
    val longitude: String,
    val start_date: String,
    val end_date: String,
    val created_by: UserDto,
    val created_at: String,
    val interested_count: Int,
    val going_count: Int,
    val is_interested: Boolean,
    val is_going: Boolean
)

// --- Stories ---

data class StoryDto(
    val id: Int,
    val author: UserDto,
    val image_url: String,
    val created_at: String,
    val expires_at: String,
    val like_count: Int = 0,
    val is_liked: Boolean = false,
    val reply_count: Int = 0
)

data class StoryReplyDto(
    val id: Int,
    val story: Int,
    val author: UserDto,
    val content: String,
    val created_at: String
)

// --- Friend Requests ---

data class FriendRequestDto(
    val id: Int,
    val seat_number: String,
    val full_name: String,
    val university_email: String?,
    val year: Int?,
    val department: String?,
    val profile: ProfileDto?
)

// --- Search ---

data class SearchResponse(
    val users: List<UserDto>,
    val posts: List<PostDto>
)


