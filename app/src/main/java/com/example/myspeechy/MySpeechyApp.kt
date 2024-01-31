package com.example.myspeechy

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myspeechy.screens.AuthScreen
import com.example.myspeechy.screens.MainScreen
import com.example.myspeechy.screens.MeditationLessonItem
import com.example.myspeechy.screens.ReadingLessonItem
import com.example.myspeechy.screens.RegularLessonItem
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@Composable
fun MySpeechyApp(navController:NavHostController = rememberNavController()) {
    val startDestination = if (Firebase.auth.currentUser == null) "auth" else "main"
    NavHost(navController, startDestination,
        enterTransition = { fadeIn(
            animationSpec = tween(durationMillis = 200),
        ) },
        exitTransition = { fadeOut(
            animationSpec = tween(durationMillis = 200),
        )
        }) {
        composable("auth") {
            AuthScreen(onNavigateToMain = {navController.navigate("main") {popUpTo(0)} })
        }
        composable("main") {
            MainScreen(navController)
        }
        composable("regularLessonItem/{regularLessonItemId}",
            arguments = listOf(navArgument("regularLessonItemId")
            {type = NavType.IntType})) {
            RegularLessonItem()
            { navController.navigateUp() }
        }
        composable("readingLessonItem/{readingLessonItemId}",
            arguments = listOf(navArgument("readingLessonItemId")
            {type = NavType.IntType})
        ) {
            ReadingLessonItem()
            {navController.navigateUp()}
        }
        composable("meditationLessonItem/{meditationLessonItemId}",
            arguments = listOf(navArgument("meditationLessonItemId")
            {type = NavType.IntType})) {
            MeditationLessonItem()
            {navController.navigateUp()}
        }
    }
}