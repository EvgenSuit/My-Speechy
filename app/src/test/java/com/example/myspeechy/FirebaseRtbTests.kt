package com.example.myspeechy

import com.example.myspeechy.services.AuthService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebasTests {
    val email: String = "some@gmail.com"
    val password: String = "Some"
    @Test
    fun authTests() {
        val mockRef = mockk<DatabaseReference>()
        val mockAuth = mockk<FirebaseAuth>(relaxed = true)
        val authService = spyk(AuthService(mockAuth, mockRef), recordPrivateCalls = true) { coEvery {
            createUser(email, password)
            logInUser(email, password)
            logOut()
        } returns Unit }

        runBlocking {
            authService.createUser(email, password)
            authService.logInUser(email, password)
        }

        coVerify { authService.createUser(email, password) }
        coVerify { authService.logInUser(email, password) }
        
        authService.logOut()
        coVerify { authService.logOut() }

        confirmVerified(authService)
    }


}