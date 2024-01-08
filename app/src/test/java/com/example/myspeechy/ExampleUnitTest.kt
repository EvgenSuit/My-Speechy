package com.example.myspeechy

import com.example.myspeechy.services.AuthService
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class ExampleUnitTest {
    private val email = "some@gmail.com"
    private val password = "SomePassword2"
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(ExampleUnitTest::class)
    }

    /*@OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun signUp_signIn_isSuccessful() = runTest {
        val authResult = mock(AuthResult::class.java)
        val auth = mock(FirebaseAuth::class.java)

        `when`(auth.signInWithEmailAndPassword(email, password)).thenReturn(Tasks.forResult(authResult))
        val authService = AuthService(auth)
        authService.logInUser(email, password) {}
    }*/



}