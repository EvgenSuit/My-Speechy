package com.example.myspeechy.utils

import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.myspeechy.services.AuthService
import com.google.firebase.auth.auth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


class AuthViewModel : ViewModel() {
    private val authService = AuthService(Firebase.auth)
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
    private fun updateExceptionState(value: String?) {
        _exceptionState.update { it.copy(value ?: "") }
    }
}

data class AuthExceptionState(val exceptionMessage: String = "")
data class AuthUiState(val email: String = "", val password: String = "")

fun String.isValidEmail() = !isNullOrEmpty() && Patterns.EMAIL_ADDRESS.matcher(this).matches()

fun String.isLongEnough() = length >= 8
fun String.hasEnoughDigits() = count(Char::isDigit) > 0
fun String.isMixedCase() = any(Char::isLowerCase) && any(Char::isUpperCase)
val passwordRequirements = listOf(String::isLongEnough, String::hasEnoughDigits, String::isMixedCase)
fun String.meetsPasswordRequirements() = passwordRequirements.all { check -> check(this) }