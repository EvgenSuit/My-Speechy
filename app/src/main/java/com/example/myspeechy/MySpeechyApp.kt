package com.example.myspeechy

import android.content.Context
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myspeechy.screens.AuthScreen
import com.example.myspeechy.screens.MainScreen
import com.example.myspeechy.screens.MeditationStatsScreen
import com.example.myspeechy.screens.chat.ChatsScreen
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest

val Context.navBarDataStore: DataStore<Preferences> by preferencesDataStore("NavBar")
val showNavBarDataStore = booleanPreferencesKey("showNavBar")
val Context.authDataStore: DataStore<Preferences> by preferencesDataStore("Auth")
val loggedOutDataStore = booleanPreferencesKey("loggedOut")

open class NavScreens(val route: String, val icon: ImageVector, val label: String) {
    data object Main: NavScreens("main", Icons.Filled.Home, "Main")
    data object Stats: NavScreens("stats", Icons.Filled.Info, "Stats")
    data object ChatsScreen: NavScreens("chats", Icons.Filled.Face, "Chats")
}
val screens = listOf(NavScreens.Main, NavScreens.Stats, NavScreens.ChatsScreen)

@OptIn(FlowPreview::class)
@Composable
fun MySpeechyApp(navController: NavHostController = rememberNavController()) {
    val startDestination = if (FirebaseAuth.getInstance().currentUser == null) "auth" else NavScreens.Main.route
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currDestination = navBackStackEntry?.destination
    var showNavBar by remember {
        mutableStateOf(false)
    }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        context.navBarDataStore.edit { navBar ->
            navBar[showNavBarDataStore] = startDestination != "auth"
        }
        context.navBarDataStore.data.collectLatest {
            showNavBar = it[showNavBarDataStore] ?: false
        }
    }
    LaunchedEffect(Unit) {
        context.authDataStore.edit { auth ->
            auth[loggedOutDataStore] = false
        }
        context.authDataStore.data.collectLatest {
            val loggedOut = it[loggedOutDataStore] ?: false
            if (loggedOut) {
                context.navBarDataStore.edit { navBar ->
                    navBar[showNavBarDataStore] = false
                }
                navController.navigate("auth") { popUpTo(0) }
            }
        }
    }
    Scaffold(
        bottomBar = {
            AnimatedVisibility(showNavBar,
                enter = slideInVertically()
                        + expandVertically(expandFrom = Alignment.Top) + fadeIn(initialAlpha = 0.3f),
                exit = shrinkVertically()
            ){
                BottomNavigation {
                    screens.forEach { screen ->
                        val selected = currDestination?.route == screen.route
                        BottomNavigationItem(
                            selected = selected,
                            onClick = { navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                } },
                            icon = { Icon(
                                    screen.icon,
                                    null,
                                    modifier = Modifier.padding(bottom = 10.dp)
                                ) },
                            label = { if (selected) Text(
                                    screen.label,
                                    fontSize = 20.sp
                                ) else Text("") },
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
            Surface(modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background) {
                NavHost(
                    navController, startDestination,
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
                            animationSpec = tween(700),
                            towards = AnimatedContentTransitionScope
                                .SlideDirection.End)}) {
                        MeditationStatsScreen()
                    }
                    composable(NavScreens.ChatsScreen.route,
                        enterTransition = { slideIntoContainer(
                            animationSpec = tween(700),
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
}
