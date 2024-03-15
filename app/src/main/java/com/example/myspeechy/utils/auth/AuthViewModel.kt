package com.example.myspeechy.utils.auth

import android.content.Intent
import android.content.IntentSender
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.myspeechy.services.AuthService
import com.example.myspeechy.services.GoogleAuthService
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class AuthViewModel @Inject constructor(
   private val authService: AuthService,
    private val googleAuthService: GoogleAuthService,
) : ViewModel() {
    var uiState by mutableStateOf(AuthUiState())
        private set
    private var _exceptionState = MutableStateFlow(AuthExceptionState())
    val exceptionState = _exceptionState.asStateFlow()

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, e ->
        _exceptionState.update {
            it.copy(
                exceptionMessage = e.message ?: "",
                authState = AuthState.FAILURE
            )
        }
    }
    val scope = CoroutineScope(coroutineExceptionHandler)

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
            _exceptionState.update { it.copy(authState = AuthState.IN_PROGRESS) }
            authService.createUser(uiState.email, uiState.password)
            _exceptionState.update { it.copy(exceptionMessage = "", authState = AuthState.SUCCESS) }
        } catch (e: Exception) {
            _exceptionState.update {
                it.copy(
                    exceptionMessage = e.message ?: "",
                    authState = AuthState.FAILURE
                )
            }
        }
    }

    suspend fun logIn() {
        try {
            _exceptionState.update { it.copy(authState = AuthState.IN_PROGRESS) }
            authService.logInUser(uiState.email, uiState.password)
            _exceptionState.update { it.copy(exceptionMessage = "", authState = AuthState.SUCCESS) }
        } catch (e: Exception) {
            _exceptionState.update { it.copy(exceptionMessage = e.message ?: "", authState = AuthState.FAILURE) }
        }
    }
    suspend fun googleSignInWithIntent(intent: Intent) {
        googleAuthService.signInWithIntent(intent)
    }
    suspend fun googleSignIn(onError: (String) -> Unit): IntentSender? {
        return googleAuthService.signIn(onError)
    }
    fun updateAuthInputExceptionMessage(value: String?) {
        _exceptionState.update { it.copy(authInputExceptionMessage = value ?: "") }
    }
}

data class AuthExceptionState(val exceptionMessage: String = "",
                              val authInputExceptionMessage: String = "",
                              val authState: AuthState = AuthState.IDLE)
data class AuthUiState(val email: String = "", val password: String = "")

enum class AuthState {
    IDLE,
    SUCCESS,
    IN_PROGRESS,
    FAILURE
}