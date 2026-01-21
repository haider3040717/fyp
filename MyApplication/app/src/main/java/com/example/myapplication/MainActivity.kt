package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.auth.SignInScreen
import com.example.myapplication.auth.SignUpScreen
import com.example.myapplication.home.HomeScreen
import com.example.myapplication.home.BottomNavigationBar
import com.example.myapplication.profile.ProfileScreen
import com.example.myapplication.profile.EditProfileScreen
import com.example.myapplication.search.SearchScreen
import com.example.myapplication.eventmap.EventMapScreen
import com.example.myapplication.createpost.CreatePostScreen
import com.example.myapplication.friends.FriendsScreen
import com.example.myapplication.messages.MessagesScreen
import com.example.myapplication.messages.ChatDetailScreen
import com.example.myapplication.notifications.NotificationsScreen
import com.example.myapplication.settings.SettingsScreen
import com.example.myapplication.postdetail.PostDetailScreen
import com.example.myapplication.data.remote.SessionManager
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize SessionManager to load persisted token
        SessionManager.init(this)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CampusConnectApp()
                }
            }
        }
    }
}

@Composable
fun CampusConnectApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var selectedBottomTab by remember { mutableIntStateOf(0) }
    var profileRefreshTrigger by remember { mutableIntStateOf(0) }

    // Determine if current screen should show bottom navigation
    val showBottomNav = currentRoute in listOf("home", "profile", "search", "eventmap", "friends")

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                BottomNavigationBar(
                    selectedTab = selectedBottomTab,
                    onTabSelected = { index ->
                        selectedBottomTab = index
                        when (index) {
                            0 -> navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }
                            1 -> navController.navigate("friends") {
                                popUpTo("friends") { inclusive = true }
                            }
                            2 -> navController.navigate("createpost")
                            3 -> navController.navigate("eventmap") {
                                popUpTo("eventmap") { inclusive = true }
                            }
                            4 -> navController.navigate("profile") {
                                popUpTo("profile") { inclusive = true }
                            }
                        }
                    },
                    onProfileClick = {
                        navController.navigate("profile") {
                            popUpTo("profile") { inclusive = true }
                        }
                    },
                    onEventMapClick = {
                        navController.navigate("eventmap") {
                            popUpTo("eventmap") { inclusive = true }
                        }
                    },
                    onCreatePostClick = {
                        navController.navigate("createpost")
                    },
                    onFriendsClick = {
                        navController.navigate("friends") {
                            popUpTo("friends") { inclusive = true }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "signin",
            modifier = Modifier.padding(paddingValues)
        ) {
        composable("signin") {
            SignInScreen(
                onNavigateToSignUp = {
                    navController.navigate("signup")
                },
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("signin") { inclusive = true }
                    }
                }
            )
        }

        composable("signup") {
            SignUpScreen(
                onNavigateToSignIn = {
                    navController.navigate("signin")
                },
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("signup") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                onNavigateToProfile = {
                    navController.navigate("profile")
                },
                onNavigateToSearch = {
                    navController.navigate("search")
                },
                onNavigateToEventMap = {
                    navController.navigate("eventmap")
                },
                onNavigateToMessages = {
                    navController.navigate("messages")
                },
                onNavigateToCreatePost = {
                    navController.navigate("createpost")
                },
                onNavigateToFriends = {
                    navController.navigate("friends")
                },
                onNavigateToNotifications = {
                    navController.navigate("notifications")
                },
                onNavigateToPostDetail = { postId ->
                    navController.navigate("postdetail/$postId")
                }
            )
        }

        composable("profile/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")?.toIntOrNull()
            ProfileScreen(
                userId = userId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToEditProfile = {
                    navController.navigate("editprofile")
                },
                onNavigateToPostDetail = { postId ->
                    navController.navigate("postdetail/$postId")
                },
                externalRefreshTrigger = profileRefreshTrigger
            )
        }
        composable("profile") {
            ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToEditProfile = {
                    navController.navigate("editprofile")
                },
                onNavigateToPostDetail = { postId ->
                    navController.navigate("postdetail/$postId")
                },
                externalRefreshTrigger = profileRefreshTrigger
            )
        }

        composable("editprofile") {
            EditProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSaveClick = {
                    navController.popBackStack()
                }
            )
        }

        composable("search") {
            SearchScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("eventmap") {
            EventMapScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("createpost") {
            CreatePostScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onPostCreated = {
                    navController.popBackStack()
                }
            )
        }

        composable("friends") {
            FriendsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToProfile = { userId ->
                    navController.navigate("profile/$userId")
                },
                onFriendAdded = {
                    // Trigger profile refresh when friend is added
                    profileRefreshTrigger++
                    android.util.Log.d("MainActivity", "Profile refresh triggered, new trigger value: $profileRefreshTrigger")
                    // Navigate to own profile to show updated friends count
                    navController.navigate("profile") {
                        popUpTo("profile") { inclusive = true }
                    }
                }
            )
        }

        composable("messages") {
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

        composable("notifications") {
            NotificationsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNotificationClick = { notificationId ->
                    // Navigate based on notification type - handled in NotificationsScreen
                    // This callback can be used for additional navigation if needed
                },
                onNavigateToPost = { postId ->
                    navController.navigate("postdetail/$postId")
                },
                onNavigateToProfile = { userId ->
                    navController.navigate("profile")
                },
                onNavigateToEvent = { eventId ->
                    navController.navigate("eventmap")
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLogout = {
                    navController.navigate("signin") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onNavigateToEditProfile = {
                    navController.navigate("editprofile")
                },
                onNavigateToPrivacy = {
                    // TODO: Navigate to privacy settings
                },
                onNavigateToNotifications = {
                    // TODO: Navigate to notification settings
                }
            )
        }

        composable("postdetail/{postId}") { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""

            PostDetailScreen(
                postId = postId,
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }
    }
    }
}
