package com.example.myspeechy

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myspeechy.screens.AuthScreen
import com.example.myspeechy.screens.MainScreen
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@Composable
fun MySpeechyApp() {
    val navController = rememberNavController()
    val startDestination = if (Firebase.auth.currentUser == null) "auth" else "main"
    NavHost(navController, startDestination) {
        composable("auth") {
            AuthScreen(onNavigateToMain = {navController.navigate("main")})
        }
        composable("main") {
            MainScreen()
        }
    }
}