package com.example.myspeechy

import android.content.Context
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myspeechy.screens.auth.AuthScreen
import com.example.myspeechy.screens.MainScreen
import com.example.myspeechy.screens.MeditationStatsScreen
import com.example.myspeechy.screens.auth.AccountDeletionScreen
import com.example.myspeechy.screens.auth.ErrorScreen
import com.example.myspeechy.screens.chat.ChatsScreen
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.collectLatest

val Context.navBarDataStore: DataStore<Preferences> by preferencesDataStore("NavBar")
val showNavBarDataStore = booleanPreferencesKey("showNavBar")
val Context.authDataStore: DataStore<Preferences> by preferencesDataStore("Auth")
val loggedOutDataStore = booleanPreferencesKey("loggedOut")
val errorKey = stringPreferencesKey("error")

open class NavScreens(val route: String, val icon: ImageVector, val label: String) {
    data object Main: NavScreens("main", Icons.Filled.Home, "Main")
    data object Stats: NavScreens("stats", Icons.Filled.Info, "Stats")
    data object ChatsScreen: NavScreens("chats", Icons.Filled.Face, "Chats")
}
open class OtherScreens(val route: String) {
    data object Auth: OtherScreens("auth")
    data object Error: OtherScreens("error")
    data object AccountDelete: OtherScreens("accountDelete")
}
val screens = listOf(NavScreens.Main, NavScreens.Stats, NavScreens.ChatsScreen)

@Composable
fun MySpeechyApp(navController: NavHostController = rememberNavController()) {
    val currUser = Firebase.auth.currentUser
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val startDestination by rememberSaveable {
        mutableStateOf(if (currUser == null) OtherScreens.Auth.route else NavScreens.Main.route)
    }
    var showNavBar by rememberSaveable {
        mutableStateOf(false)
    }
    val context = LocalContext.current
    var error by rememberSaveable {
        mutableStateOf<String?>(null)
    }
    val animationDuration = 400
    LaunchedEffect(Unit) {
        context.navBarDataStore.data.collectLatest {
            showNavBar = it[showNavBarDataStore] ?: false
        }
    }
    LaunchedEffect(Unit) {
        context.authDataStore.data.collectLatest {
            val loggedOut = it[loggedOutDataStore] ?: false
            val currRoute = navBackStackEntry?.destination?.route
            //if logged out and not in auth, navigate to auth
            if (loggedOut && currRoute != OtherScreens.Auth.route) {
                navController.navigate(OtherScreens.Auth.route) { popUpTo(OtherScreens.Auth.route) }
            }
            error = it[errorKey]
            if (!error.isNullOrEmpty() && currRoute != OtherScreens.Auth.route && currRoute != OtherScreens.AccountDelete.route) {
                navController.navigate(OtherScreens.Error.route) {popUpTo(OtherScreens.Error.route) }
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
                        val selected = navBackStackEntry?.destination?.route == screen.route
                        BottomNavigationItem(
                            selected = selected,
                            onClick = { if (showNavBar) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            } },
                            icon = { Icon(
                                    screen.icon,
                                tint = MaterialTheme.colorScheme.primary,
                                    contentDescription = null,
                                    modifier = Modifier.padding(bottom = 10.dp)
                                ) },
                            label = { if (selected) Text(
                                    screen.label,
                                color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 20.sp
                                ) else Text("") },
                            selectedContentColor = Color.White,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.background)
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
                            animationSpec = tween(animationDuration),
                            towards = AnimatedContentTransitionScope
                                .SlideDirection.End)}) {
                        MainScreen()
                    }
                    composable(NavScreens.Stats.route,
                        enterTransition = {slideIntoContainer(
                            animationSpec = tween(animationDuration),
                            towards = AnimatedContentTransitionScope
                                .SlideDirection.End)}) {
                        MeditationStatsScreen()
                    }
                    composable(NavScreens.ChatsScreen.route,
                        enterTransition = { slideIntoContainer(
                            animationSpec = tween(animationDuration),
                            towards = AnimatedContentTransitionScope
                                .SlideDirection.End)}) {
                        ChatsScreen {
                            navController.navigate("accountDelete") {popUpTo(0)}
                        }
                    }
                    composable(OtherScreens.Auth.route) {
                        AuthScreen(onNavigateToMain = {
                            navController.navigate(NavScreens.Main.route) { popUpTo(NavScreens.Main.route)}
                        })
                    }
                    composable(OtherScreens.Error.route,
                        enterTransition = { slideIntoContainer(
                            animationSpec = tween(animationDuration),
                            towards = AnimatedContentTransitionScope
                                .SlideDirection.Up)}) {
                        if (!error.isNullOrEmpty()) {
                            ErrorScreen(error = error!!, onTryAgain = {
                                navController.navigate(NavScreens.Main.route) { popUpTo(NavScreens.Main.route) }
                            })  { Firebase.auth.signOut() }
                        }
                    }
                    composable(OtherScreens.AccountDelete.route) {
                        BackHandler(true) {}
                        AccountDeletionScreen(onGoBack = {
                            navController.navigate(NavScreens.Main.route) {popUpTo(NavScreens.Main.route)}
                        })
                    }
                }
            }
    }
}
