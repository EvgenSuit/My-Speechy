package com.example.myspeechy.domain.auth

class AccountDeletionService(private val authService: AuthService) {
    val userId = authService.userId
    suspend fun deleteUser() {
        if (userId == null) throw Exception("Couldn't delete account")
        authService.removeProfilePics(userId)
        authService.revokeMembership()
        authService.deleteRdbUser()
        authService.deleteFirestoreData(userId)
        authService.deleteUser()
    }
}