package com.example.myapplication.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // --- Auth ---

    @POST("api/accounts/login/")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("api/accounts/register/")
    suspend fun register(@Body body: RegisterRequest): LoginResponse

            @GET("api/accounts/me/")
            suspend fun me(): UserDto

            @GET("api/accounts/users/{id}/")
            suspend fun getUserProfile(@Path("id") id: Int): UserDto

            @GET("api/accounts/profile/")
            suspend fun profile(): ProfileDto

            @PUT("api/accounts/profile/")
            suspend fun updateProfile(@Body body: ProfileDto): ProfileDto

            @GET("api/accounts/profile/{id}/share/")
            suspend fun getProfileShareUrl(@Path("id") id: Int): ShareResponse

            @PATCH("api/accounts/update-email/")
            suspend fun updateEmail(@Body body: Map<String, String>): Map<String, String>

            @POST("api/accounts/change-password/")
            suspend fun changePassword(@Body body: Map<String, String>): Map<String, String>

    // --- Posts ---

    @GET("api/posts/posts/")
    suspend fun getFeed(): List<PostDto>

    @GET("api/posts/posts/")
    suspend fun getUserPosts(@Query("user_id") userId: Int): List<PostDto>

    @POST("api/posts/posts/")
    suspend fun createPost(@Body body: PostRequest): PostDto

    @GET("api/posts/posts/{id}/")
    suspend fun getPost(@Path("id") id: Int): PostDto

    @POST("api/posts/posts/{id}/like/")
    suspend fun likePost(@Path("id") id: Int): Map<String, String>

    @POST("api/posts/posts/{id}/unlike/")
    suspend fun unlikePost(@Path("id") id: Int): Map<String, String>

    @PUT("api/posts/posts/{id}/")
    suspend fun updatePost(@Path("id") id: Int, @Body body: Map<String, String>): PostDto

    @retrofit2.http.DELETE("api/posts/posts/{id}/")
    suspend fun deletePost(@Path("id") id: Int): Map<String, String>

    @GET("api/posts/posts/{id}/share_url/")
    suspend fun getShareUrl(@Path("id") id: Int): ShareResponse

    // --- Comments ---

    @GET("api/posts/comments/")
    suspend fun getComments(@Query("post") postId: Int): List<CommentDto>

    @POST("api/posts/comments/")
    suspend fun createComment(@Body body: CommentRequest): CommentDto

    @POST("api/posts/comments/{id}/like/")
    suspend fun likeComment(@Path("id") id: Int): Map<String, String>

    @POST("api/posts/comments/{id}/unlike/")
    suspend fun unlikeComment(@Path("id") id: Int): Map<String, String>

    @retrofit2.http.DELETE("api/posts/comments/{id}/")
    suspend fun deleteComment(@Path("id") id: Int): Map<String, String>

    // --- Messaging ---

    @GET("api/messages/conversations/")
    suspend fun getConversations(): List<ConversationDto>

    @POST("api/messages/start/")
    suspend fun startConversation(@Body body: StartConversationRequest): Map<String, Int>

    @GET("api/messages/conversations/{id}/messages/")
    suspend fun getMessages(@Path("id") conversationId: Int): List<MessageDto>

    @POST("api/messages/conversations/{id}/messages/")
    suspend fun sendMessage(
        @Path("id") conversationId: Int,
        @Body body: MessageRequest
    ): MessageDto

    // --- Notifications ---

    @GET("api/notifications/")
    suspend fun getNotifications(): List<NotificationDto>

    @POST("api/notifications/mark-all-read/")
    suspend fun markAllNotificationsRead(): Map<String, String>

    @POST("api/notifications/{id}/mark-read/")
    suspend fun markNotificationRead(@Path("id") id: Int): Map<String, String>

    // --- Events ---

    @GET("api/events/events/")
    suspend fun getEvents(): List<EventDto>

    @POST("api/events/events/")
    suspend fun createEvent(@Body body: EventRequest): EventDto

    @retrofit2.http.DELETE("api/events/events/{id}/")
    suspend fun deleteEvent(@Path("id") id: Int): Map<String, String>

    @POST("api/events/events/{id}/interested/")
    suspend fun markEventInterested(@Path("id") id: Int): Map<String, String>

    @POST("api/events/events/{id}/going/")
    suspend fun markEventGoing(@Path("id") id: Int): Map<String, String>

    @POST("api/events/events/{id}/uninterested/")
    suspend fun markEventUninterested(@Path("id") id: Int): Map<String, String>

    // --- Stories ---

    @GET("api/posts/stories/")
    suspend fun getStories(): List<StoryDto>

    @POST("api/posts/stories/")
    suspend fun createStory(@Body body: Map<String, String>): StoryDto

    @POST("api/posts/stories/{id}/like/")
    suspend fun likeStory(@Path("id") id: Int): Map<String, String>

    @POST("api/posts/stories/{id}/unlike/")
    suspend fun unlikeStory(@Path("id") id: Int): Map<String, String>

    @GET("api/posts/story-replies/")
    suspend fun getStoryReplies(@Query("story") storyId: Int): List<StoryReplyDto>

    @POST("api/posts/story-replies/")
    suspend fun createStoryReply(@Body body: StoryReplyRequest): StoryReplyDto

    @retrofit2.http.DELETE("api/posts/story-replies/{id}/")
    suspend fun deleteStoryReply(@Path("id") id: Int): Map<String, String>

    // --- Friend Requests ---

    @GET("api/accounts/friend-requests/")
    suspend fun getFriendRequests(): List<UserDto>

    @GET("api/accounts/friend-suggestions/")
    suspend fun getFriendSuggestions(): List<UserDto>

    @GET("api/accounts/friends/")
    suspend fun getFriends(): List<UserDto>

    @POST("api/accounts/follow/{id}/")
    suspend fun followUser(@Path("id") id: Int): Map<String, String>

    @retrofit2.http.DELETE("api/accounts/follow/{id}/")
    suspend fun unfollowUser(@Path("id") id: Int): Map<String, String>

    // --- Search ---

    @GET("api/accounts/search/")
    suspend fun search(
        @Query("q") query: String,
        @Query("type") type: String? = null
    ): SearchResponse
}


