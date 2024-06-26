package com.myspeechy.myspeechy

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.datastore.preferences.core.edit
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.myspeechy.myspeechy.data.errorKey
import com.myspeechy.myspeechy.data.lesson.LessonCategories
import com.myspeechy.myspeechy.data.loggedOutDataStore
import com.myspeechy.myspeechy.data.navBarDataStore
import com.myspeechy.myspeechy.data.showNavBarDataStore
import com.myspeechy.myspeechy.domain.AnimationConfig
import com.myspeechy.myspeechy.presentation.MySpeechyViewModel
import com.myspeechy.myspeechy.screens.MainScreen
import com.myspeechy.myspeechy.screens.MeditationStatsScreen
import com.myspeechy.myspeechy.screens.auth.AccountDeletionScreen
import com.myspeechy.myspeechy.screens.auth.AuthScreen
import com.myspeechy.myspeechy.screens.auth.ErrorScreen
import com.myspeechy.myspeechy.screens.chat.ChatsScreen
import com.myspeechy.myspeechy.screens.chat.PrivateChatScreen
import com.myspeechy.myspeechy.screens.chat.PublicChatScreen
import com.myspeechy.myspeechy.screens.chat.UserProfileScreen
import com.myspeechy.myspeechy.screens.lesson.MeditationLessonItem
import com.myspeechy.myspeechy.screens.lesson.ReadingLessonItem
import com.myspeechy.myspeechy.screens.lesson.RegularLessonItem
import com.myspeechy.myspeechy.screens.settings.SettingsScreen
import com.myspeechy.myspeechy.screens.thoughtTracker.ThoughtTrackerItemScreen
import com.myspeechy.myspeechy.screens.thoughtTracker.ThoughtTrackerScreen
import kotlinx.coroutines.flow.collectLatest

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

@SuppressLint("UnrememberedGetBackStackEntry")
@Composable
fun MySpeechyApp(navController: NavHostController = rememberNavController(),
                 viewModel: MySpeechyViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
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
    LaunchedEffect(navBackStackEntry, uiState.dataLoaded) {
        context.navBarDataStore.edit { navBar ->
            navBar[showNavBarDataStore] = screens.any { it.route == navBackStackEntry?.destination?.route }
        }
        context.navBarDataStore.data.collectLatest {
            showNavBar = uiState.dataLoaded && (it[showNavBarDataStore] ?: false)
        }
    }
    LaunchedEffect(uiState.authPreferences) {
        val pref = uiState.authPreferences ?: return@LaunchedEffect
        val loggedOut = pref[loggedOutDataStore] ?: false
        val currRoute = navBackStackEntry?.destination?.route
        //if logged out and not in auth, navigate there
        if (currRoute != null) {
            if (loggedOut && currRoute != OtherScreens.Auth.route) {
                navController.navigate(OtherScreens.Auth.route) { popUpTo(0)}
                return@LaunchedEffect
            }
            if (currRoute != OtherScreens.Auth.route && currRoute != OtherScreens.AccountDelete.route) {
                // if error was not null or empty but now it is means there's no error anymore and we can navigate to Main
                if (!error.isNullOrEmpty() && pref[errorKey].isNullOrEmpty()) {
                    // use popUpTo(NavScreens.Main.route) to not reinitialize view model
                    // if already navigated to Main before
                    navController.navigate(NavScreens.Main.route) {
                        popUpTo(NavScreens.Main.route)
                        launchSingleTop = true
                    }
                }
                error = pref[errorKey]
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
                    enterTransition = { slideInHorizontally { -it } },
                    exitTransition = { fadeOut() },
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
                    composable(OtherScreens.Auth.route,
                        enterTransition = {EnterTransition.None}) {
                        AuthScreen {
                            navController.navigate(NavScreens.Main.route) {
                                popUpTo(0)
                            }
                        }
                    }
                    composable(OtherScreens.Error.route) {
                        if (!error.isNullOrEmpty()) {
                            ErrorScreen(error = error!!, onTryAgain = {
                                navController.navigate(NavScreens.Main.route) { popUpTo(NavScreens.Main.route) }
                            })  { Firebase.auth.signOut() }
                        }
                    }
                    composable(OtherScreens.AccountDelete.route) {
                        AccountDeletionScreen(onGoBack = {
                            //navController.navigate(NavScreens.ChatsScreen.route) { popUpTo(0) }
                            navController.navigateUp()
                        })
                    }
                    composable(
                        "${LessonCategories.PSYCHOLOGICAL}/{id}/{wasWelcomeDialogShown}",
                        arguments = listOf(navArgument("id")
                        { type = NavType.IntType },
                            navArgument("wasWelcomeDialogShown") {type = NavType.BoolType}
                        )
                    ) {
                        RegularLessonItem { navController.navigateUp() }
                    }
                    composable(
                        "${LessonCategories.READING}/{id}/{wasWelcomeDialogShown}",
                        arguments = listOf(navArgument("id") { type = NavType.IntType },
                            navArgument("wasWelcomeDialogShown") {type = NavType.BoolType})
                    ) {
                        ReadingLessonItem()
                        { navController.navigateUp() }
                    }
                    composable(
                        "${LessonCategories.MEDITATION}/{id}/{wasWelcomeDialogShown}",
                        arguments = listOf(navArgument("id") { type = NavType.IntType },
                            navArgument("wasWelcomeDialogShown") {type = NavType.BoolType})
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
    NavigationBar {
        for (screen in screens) {
            val currentRoute = navBackStackEntry?.destination?.route
            val selected = currentRoute == screen.route
            NavigationBarItem(selected = selected,
                onClick = {
                    if (showNavBar) {
                        navController.navigate(screen.route) {
                            popUpTo(NavScreens.Main.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        painterResource(screen.icon),
                        contentDescription = null,
                        modifier = Modifier
                            .size(
                                if (selected) dimensionResource(R.dimen.selected_bottom_bar_icon_size) else
                                    dimensionResource(R.dimen.unselected_bottom_bar_icon_size)
                            )
                    )
                })

            /*BottomNavigationItem(
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
                    contentDescription = null,
                    modifier = Modifier
                        .size(if (selected) dimensionResource(R.dimen.selected_bottom_bar_icon_size) else
                            dimensionResource(R.dimen.unselected_bottom_bar_icon_size))
                ) },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .semantics {
                        contentDescription = screen.label
                    }
            )*/
        }
    }
}