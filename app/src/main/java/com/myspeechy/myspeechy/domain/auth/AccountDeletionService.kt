package com.myspeechy.myspeechy.domain.auth

import com.google.firebase.auth.FirebaseAuth

class AccountDeletionService(private val authService: AuthService,
    private val auth: FirebaseAuth) {
    suspend fun deleteUser() {
        val userId = auth.currentUser?.uid ?: throw Exception("user reference is empty")
        authService.removeProfilePics(userId)
        authService.revokeMembership()
        authService.deleteFirestoreData(userId)
        authService.deleteRdbUser()
        authService.deleteUser()
    }
}