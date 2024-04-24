package com.example.myspeechy

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myspeechy.data.authDataStore
import com.example.myspeechy.data.errorKey
import com.example.myspeechy.data.isDataLoaded
import com.example.myspeechy.data.loadData
import com.example.myspeechy.data.loggedOutDataStore
import com.example.myspeechy.data.navBarDataStore
import com.example.myspeechy.data.showNavBarDataStore
import com.example.myspeechy.domain.AnimationConfig
import com.example.myspeechy.screens.auth.AuthScreen
import com.example.myspeechy.screens.MainScreen
import com.example.myspeechy.screens.MeditationStatsScreen
import com.example.myspeechy.screens.SettingsScreen
import com.example.myspeechy.screens.auth.AccountDeletionScreen
import com.example.myspeechy.screens.auth.ErrorScreen
import com.example.myspeechy.screens.chat.ChatsScreen
import com.example.myspeechy.screens.chat.PrivateChatScreen
import com.example.myspeechy.screens.chat.PublicChatScreen
import com.example.myspeechy.screens.chat.UserProfileScreen
import com.example.myspeechy.screens.lesson.MeditationLessonItem
import com.example.myspeechy.screens.lesson.ReadingLessonItem
import com.example.myspeechy.screens.lesson.RegularLessonItem
import com.example.myspeechy.screens.thoughtTracker.ThoughtTrackerItemScreen
import com.example.myspeechy.screens.thoughtTracker.ThoughtTrackerScreen
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.collectLatest


// TODO change icons
open class NavScreens(val route: String, val icon: Int, val label: String) {
    data object Main: NavScreens("main", R.drawable.house_icon, "Main")
    data object ThoughtTracker: NavScreens("thoughtTracker", R.drawable.thoughts_icon, "ThoughtTracker")
    data object Stats: NavScreens("stats", R.drawable.stats_icon, "Stats")
    data object ChatsScreen: NavScreens("chats", R.drawable.chat_icon, "Chats")
    data object SettingsScreen: NavScreens("settings", R.drawable.settings_icon, "Settings")
}
open class OtherScreens(val route: String) {
    data object Auth: OtherScreens("auth")
    data object Error: OtherScreens("error")
    data object AccountDelete: OtherScreens("accountDelete")
}
val screens = listOf(NavScreens.Main, NavScreens.ThoughtTracker,
    NavScreens.Stats, NavScreens.ChatsScreen, NavScreens.SettingsScreen)

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
    var dataLoaded by rememberSaveable {
        mutableStateOf(false)
    }
    val context = LocalContext.current
    var error by rememberSaveable {
        mutableStateOf<String?>(null)
    }
    LaunchedEffect(Unit) {
        context.loadData.data.collectLatest {
            dataLoaded = it[isDataLoaded] ?: false
        }
    }
    LaunchedEffect(navBackStackEntry, dataLoaded) {
        context.navBarDataStore.edit { navBar ->
            navBar[showNavBarDataStore] = screens.any { it.route == navBackStackEntry?.destination?.route }
        }
        context.navBarDataStore.data.collectLatest {
            showNavBar = dataLoaded && (it[showNavBarDataStore] ?: false)
        }
    }
    LaunchedEffect(Unit) {
        context.authDataStore.data.collectLatest {
            val loggedOut = it[loggedOutDataStore] ?: false
            val currRoute = navBackStackEntry?.destination?.route
            //if logged out and not in auth, navigate there
            if (loggedOut && currRoute != OtherScreens.Auth.route) {
                navController.navigate(OtherScreens.Auth.route) { popUpTo(OtherScreens.Auth.route)}
            }
            if (currRoute != OtherScreens.Auth.route && currRoute != OtherScreens.AccountDelete.route) {
                // if error was not null or empty, but now it is means there's no error anymore and we can navigate to Main
                if (!error.isNullOrEmpty() && it[errorKey].isNullOrEmpty()) {
                    // use popUpTo(NavScreens.Main.route) to not reinitialize view model
                    // if already navigated to Main before
                    navController.navigate(NavScreens.Main.route) {
                        popUpTo(NavScreens.Main.route)
                        launchSingleTop = true
                    }
                }
                error = it[errorKey]
                if (!error.isNullOrEmpty()) {
                    navController.navigate(OtherScreens.Error.route) {popUpTo(OtherScreens.Error.route) }
                }
            }

        }
    }
    Scaffold(
        bottomBar = {
            AnimatedVisibility(showNavBar,
                enter = slideInVertically()
                        + expandVertically(expandFrom = Alignment.Top) + fadeIn(initialAlpha = 0.3f),
                exit = shrinkVertically(tween(AnimationConfig.BOTTOM_NAV_BAR_SHRINK_DURATION))
            ) {
                BottomNavBar(
                    showNavBar = showNavBar,
                    navController = navController,
                    navBackStackEntry = navBackStackEntry
                )
            }
        }
    ) { innerPadding ->
            Surface(Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background) {
                NavHost(
                    navController, startDestination,
                    enterTransition = { EnterTransition.None},
                    exitTransition = { ExitTransition.None},
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    composable(NavScreens.Main.route) {
                        MainScreen(navController)
                    }
                    composable(NavScreens.ThoughtTracker.route) {
                        ThoughtTrackerScreen(navController)
                    }
                    composable(NavScreens.Stats.route) {
                        MeditationStatsScreen()
                    }
                    composable(NavScreens.ChatsScreen.route) {
                        ChatsScreen(navController)
                    }
                    composable(NavScreens.SettingsScreen.route) {
                        SettingsScreen()
                    }
                    composable(OtherScreens.Auth.route) {
                        AuthScreen(onNavigateToMain = {
                            navController.navigate(NavScreens.Main.route) { popUpTo(NavScreens.Main.route) }
                        })
                    }
                    composable(OtherScreens.Error.route) {
                        if (!error.isNullOrEmpty()) {
                            ErrorScreen(error = error!!, onTryAgain = {
                                navController.navigate(NavScreens.Main.route) { popUpTo(NavScreens.Main.route) }
                            })  { Firebase.auth.signOut() }
                        }
                    }
                    composable(OtherScreens.AccountDelete.route) {
                        BackHandler(true) {}
                        AccountDeletionScreen(onGoBack = {
                            //navController.navigate(NavScreens.ChatsScreen.route) { popUpTo(0) }
                            navController.navigateUp()
                        })
                    }
                    composable(
                        "regularLessonItem/{regularLessonItemId}",
                        arguments = listOf(navArgument("regularLessonItemId")
                        { type = NavType.IntType })
                    ) {
                        RegularLessonItem { navController.navigateUp() }
                    }
                    composable(
                        "readingLessonItem/{readingLessonItemId}",
                        arguments = listOf(navArgument("readingLessonItemId")
                        { type = NavType.IntType })
                    ) {
                        ReadingLessonItem()
                        { navController.navigateUp() }
                    }
                    composable(
                        "meditationLessonItem/{meditationLessonItemId}",
                        arguments = listOf(navArgument("meditationLessonItemId")
                        { type = NavType.IntType })
                    ) {
                        MeditationLessonItem()
                        { navController.navigateUp() }
                    }
                    composable("${NavScreens.ThoughtTracker.route}/{date}",
                        arguments = listOf(navArgument("date")
                        { type = NavType.StringType })) {
                        ThoughtTrackerItemScreen { navController.navigateUp() }
                    }
                    composable("chats/{type}/{chatId}",
                        arguments = listOf(
                            navArgument("type") {type = NavType.StringType},
                            navArgument("chatId") {type = NavType.StringType}),
                    ) {backStackEntry ->
                        if (backStackEntry.arguments!!.getString("type") == "public") {
                            PublicChatScreen(navController)
                        } else {
                            PrivateChatScreen(navController)
                        }
                    }
                    composable("userProfile/{userId}",
                        arguments = listOf(navArgument("userId") {type = NavType.StringType})) {
                        UserProfileScreen({ navController.navigateUp() },
                            onAccountDelete = {navController.navigate("accountDelete")})
                    }

                }
            }
    }
}

@Composable
fun BottomNavBar(
    showNavBar: Boolean,
    navController: NavHostController,
    navBackStackEntry: NavBackStackEntry?) {
    BottomNavigation {
        for (screen in screens) {
            val selected = navBackStackEntry?.destination?.route == screen.route
            BottomNavigationItem(
                selected = selected,
                onClick = {
                    if (showNavBar) {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                } },
                icon = { Icon(
                    painterResource(screen.icon),
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = null,
                    modifier = Modifier
                        .size(if (selected) dimensionResource(R.dimen.selected_bottom_bar_icon_size) else
                            dimensionResource(R.dimen.unselected_bottom_bar_icon_size))
                ) },

                selectedContentColor = Color.White,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .weight(1f)
                    .semantics {
                        contentDescription = screen.label
                    }
            )
        }
    }
}