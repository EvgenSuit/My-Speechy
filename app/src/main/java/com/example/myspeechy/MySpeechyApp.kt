package com.example.myspeechy

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myspeechy.screens.AuthScreen
import com.example.myspeechy.screens.chat.ChatsScreen
import com.example.myspeechy.screens.MainScreen
import com.example.myspeechy.screens.MeditationStatsScreen
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

sealed class NavScreens(val route: String, val icon: ImageVector, val label: String) {
    data object Main: NavScreens("main", Icons.Filled.Home, "Main")
    data object Stats: NavScreens("stats", Icons.Filled.Info, "Stats")
    data object ChatsScreen: NavScreens("chats", Icons.Filled.Face, "Chats")
}
val screens = listOf(NavScreens.Main, NavScreens.Stats, NavScreens.ChatsScreen)

@Composable
fun MySpeechyApp(navController:NavHostController = rememberNavController()) {
    val startDestination = if (Firebase.auth.currentUser == null) "auth" else NavScreens.Main.route
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currDestination = navBackStackEntry?.destination
    val showNavBar = screens.any { it.route == currDestination?.route }
    Scaffold(
        bottomBar = {
            if (showNavBar) {
                BottomNavigation {
                    screens.forEach { screen ->
                        val selected = currDestination?.route == screen.route
                        BottomNavigationItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    screen.icon,
                                    null,
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )
                            },
                            label = {
                                if (selected) Text(
                                    screen.label,
                                    fontSize = 20.sp
                                ) else Text("")
                            },
                            selectedContentColor = Color.White,
                            modifier = Modifier
                                .offset(y = if (!selected) 10.dp else 0.dp)
                                .weight(1f)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController, startDestination,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(NavScreens.Main.route,
                enterTransition = { slideIntoContainer(
                    animationSpec = tween(500),
                    towards = AnimatedContentTransitionScope
                    .SlideDirection.End)}) {
                MainScreen()
            }
            composable(NavScreens.Stats.route,
                enterTransition = {slideIntoContainer(
                    animationSpec = tween(500),
                    towards = AnimatedContentTransitionScope
                        .SlideDirection.End)}) {
                MeditationStatsScreen()
            }
            composable(NavScreens.ChatsScreen.route,
                enterTransition = {slideIntoContainer(
                    animationSpec = tween(500),
                    towards = AnimatedContentTransitionScope
                        .SlideDirection.End)}) {
                ChatsScreen()
            }
            composable("auth") {
                AuthScreen(onNavigateToMain = {
                    navController.navigate(NavScreens.Main.route) { popUpTo(0) }
                })
            }
        }
    }
}
