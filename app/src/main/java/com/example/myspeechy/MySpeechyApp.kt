package com.example.myspeechy

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myspeechy.screens.AuthScreen
import com.example.myspeechy.screens.MainScreen
import com.example.myspeechy.screens.RegularLessonItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@Composable
fun MySpeechyApp(navController:NavHostController = rememberNavController()) {
    val startDestination = if (Firebase.auth.currentUser == null) "auth" else "main"
    NavHost(navController, startDestination) {
        composable("auth") {
            AuthScreen(onNavigateToMain = {navController.navigate("main") {popUpTo(0)} })
        }
        composable("main") {
            MainScreen(navController)
        }
        composable("regularLessonItem/{regularLessonItemId}",
            arguments = listOf(navArgument("regularLessonItemId")
            {type = NavType.IntType})) {backStackEntry ->
            RegularLessonItem(backStackEntry.arguments?.getInt("regularLessonItemId") ?: 0)
            { navController.navigate("main") }
        }
    }
}