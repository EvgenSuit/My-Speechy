package com.example.myspeechy.utils

import android.content.Intent
import android.content.IntentSender
import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.myspeechy.services.AuthService
import com.example.myspeechy.services.GoogleAuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject


@HiltViewModel
class AuthViewModel @Inject constructor(
   private val authService: AuthService,
    private val googleAuthService: GoogleAuthService,
) : ViewModel() {
    var uiState by mutableStateOf(AuthUiState())
        private set
    private var _exceptionState = MutableStateFlow(AuthExceptionState())
    val exceptionState  = _exceptionState.asStateFlow()

    fun onEmailChanged(value: String) {
        uiState = uiState.copy(email = value)
        _exceptionState.update { it.copy("") }
    }
    fun onPasswordChanged(value: String) {
        uiState = uiState.copy(password = value)
        _exceptionState.update { it.copy("") }
    }
    suspend fun signUp() {
            try {
                authService.createUser(uiState.email, uiState.password) {updateExceptionState(it?.message)}
            } catch (e: Exception) {
            }
    }
    suspend fun logIn() {
        try {
            authService.logInUser(uiState.email, uiState.password) {updateExceptionState(it?.message)}
        } catch (e: Exception) {
        }
    }
    suspend fun googleSignInWithIntent(intent: Intent) {
        googleAuthService.signInWithIntent(intent)
    }
    suspend fun googleSignIn(): IntentSender? {
        return googleAuthService.signIn()
    }
    private fun updateExceptionState(value: String?) {
        _exceptionState.update { it.copy(value ?: "") }
    }
}

data class AuthExceptionState(val exceptionMessage: String = "")
data class AuthUiState(val email: String = "some@gmail.com", val password: String = "Some2223")

fun String.isValidEmail() = !isNullOrEmpty() && Patterns.EMAIL_ADDRESS.matcher(this).matches()

fun String.isLongEnough() = length >= 8
fun String.hasEnoughDigits() = count(Char::isDigit) > 0
fun String.isMixedCase() = any(Char::isLowerCase) && any(Char::isUpperCase)
val passwordRequirements = listOf(String::isLongEnough, String::hasEnoughDigits, String::isMixedCase)
fun String.meetsPasswordRequirements() = passwordRequirements.all { check -> check(this) }