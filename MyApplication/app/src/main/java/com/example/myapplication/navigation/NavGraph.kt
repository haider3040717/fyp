package com.yourname.campusconnect.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.myapplication.auth.SignInScreen
import com.example.myapplication.auth.SignUpScreen
import com.example.myapplication.home.HomeScreen
import com.example.myapplication.profile.ProfileScreen
import com.example.myapplication.search.SearchScreen
import com.example.myapplication.eventmap.EventMapScreen
import com.example.myapplication.createpost.CreatePostScreen
import com.example.myapplication.friends.FriendsScreen
import com.example.myapplication.messages.MessagesScreen
import com.example.myapplication.messages.ChatDetailScreen
import com.example.myapplication.notifications.NotificationsScreen
import com.example.myapplication.postdetail.PostDetailScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.SignIn.route
    ) {
        composable(route = Screen.SignIn.route) {
            SignInScreen(
                onNavigateToSignUp = {
                    navController.navigate(Screen.SignUp.route)
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.SignIn.route) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.SignUp.route) {
            SignUpScreen(
                onNavigateToSignIn = {
                    navController.navigate(Screen.SignIn.route)
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.Home.route) {
            HomeScreen(
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route)
                },
                onNavigateToEventMap = {
                    navController.navigate(Screen.EventMap.route)
                },
                onNavigateToMessages = {
                    navController.navigate(Screen.Messages.route)
                },
                onNavigateToCreatePost = {
                    navController.navigate(Screen.CreatePost.route)
                },
                onNavigateToFriends = {
                    navController.navigate(Screen.Friends.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route)
                },
                onNavigateToPostDetail = { postId ->
                    navController.navigate("postdetail/$postId")
                }
            )
        }

        composable(route = Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.Search.route) {
            SearchScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.EventMap.route) {
            EventMapScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.CreatePost.route) {
            CreatePostScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onPostCreated = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.Friends.route) {
            FriendsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToProfile = { userId ->
                    // TODO: Navigate to user profile
                }
            )
        }

        composable(route = Screen.Messages.route) {
            MessagesScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onChatClick = { chatId, chatName, isOnline ->
                    navController.navigate("chatdetail/$chatId/$chatName/$isOnline")
                },
                onSearchClick = {
                    // TODO: Add search functionality
                }
            )
        }

        composable("chatdetail/{chatId}/{chatName}/{isOnline}") { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            val chatName = backStackEntry.arguments?.getString("chatName") ?: ""
            val isOnline = backStackEntry.arguments?.getString("isOnline")?.toBoolean() ?: true

            ChatDetailScreen(
                chatId = chatId,
                chatName = chatName,
                isOnline = isOnline,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.Notifications.route) {
            NotificationsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNotificationClick = { notificationId ->
                    // TODO: Navigate based on notification type
                }
            )
        }

        composable("postdetail/{postId}") { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: "1"

            PostDetailScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                postId = postId
            )
        }
    }
}

sealed class Screen(val route: String) {
    object SignIn : Screen("signin")
    object SignUp : Screen("signup")
    object Home : Screen("home")
    object Profile : Screen("profile")
    object Search : Screen("search")
    object EventMap : Screen("eventmap")
    object CreatePost : Screen("createpost")
    object Friends : Screen("friends")
    object Messages : Screen("messages")
    object ChatDetail : Screen("chatdetail")
    object Notifications : Screen("notifications")
    object PostDetail : Screen("postdetail")
}
