package com.example.myapplication.ui.navigation

sealed class Screen(val route: String) {
    object SignIn : Screen("signin")
    object SignUp : Screen("signup")
    object Home : Screen("home")
    object Profile : Screen("profile")
    object EditProfile : Screen("editprofile")
    object Search : Screen("search")
    object EventMap : Screen("eventmap")
    object CreatePost : Screen("createpost")
    object Friends : Screen("friends")
    object Messages : Screen("messages")
    object ChatDetail : Screen("chatdetail/{chatId}/{chatName}/{isOnline}") {
        fun createRoute(chatId: String, chatName: String, isOnline: Boolean) =
            "chatdetail/$chatId/$chatName/$isOnline"
    }
    object Notifications : Screen("notifications")
    object Settings : Screen("settings")
    object PostDetail : Screen("postdetail/{postId}") {
        fun createRoute(postId: String) = "postdetail/$postId"
    }
}
