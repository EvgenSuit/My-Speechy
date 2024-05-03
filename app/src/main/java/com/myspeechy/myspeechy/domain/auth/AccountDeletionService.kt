package com.myspeechy.myspeechy.domain.auth

import com.google.firebase.auth.FirebaseAuth

class AccountDeletionService(private val authService: AuthService,
    private val auth: FirebaseAuth) {
    suspend fun deleteUser() {
        val userId = auth.currentUser?.uid ?: throw Exception("Couldn't delete account: authentication error")
        authService.removeProfilePics(userId)
        authService.revokeMembership()
        authService.deleteFirestoreData(userId)
        authService.deleteRdbUser()
        authService.deleteUser()
    }
}