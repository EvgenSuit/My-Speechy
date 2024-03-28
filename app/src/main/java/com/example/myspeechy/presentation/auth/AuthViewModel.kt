package com.example.myspeechy.presentation.auth

import android.content.Intent
import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myspeechy.domain.auth.AuthService
import com.example.myspeechy.domain.auth.GoogleAuthService
import com.example.myspeechy.domain.Result
import com.example.myspeechy.domain.error.EmailError
import com.example.myspeechy.domain.error.PasswordError
import com.google.firebase.auth.FirebaseAuthException
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()
    private var _exceptionState = MutableStateFlow(AuthExceptionState())
    val exceptionState = _exceptionState.asStateFlow()

    private fun validateEmail(value: String): String =
        when(val res = authService.validateEmail(value)) {
            is Result.Error -> {
                when (res.error) {
                    EmailError.IS_NOT_VALID -> "Email is of a wrong format"
                    EmailError.IS_EMPTY -> "Email is empty"
                }
            }
            is Result.Success -> { res.data }
    }
    private fun validatePassword(value: String): String =
        when(val res = authService.validatePassword(value)) {
            is Result.Error -> {
                when (res.error) {
                    PasswordError.IS_EMPTY -> "Password is empty"
                    PasswordError.IS_NOT_MIXED_CASE -> "Password must contain mixed letters"
                    PasswordError.IS_NOT_LONG_ENOUGH -> "Password is not long enough"
                    PasswordError.NOT_ENOUGH_DIGITS -> "Password must contain at least 1 digit"
                }
            }
            is Result.Success -> { res.data }
        }

    fun onEmailChanged(value: String) {
        _exceptionState.update { it.copy("", emailErrorMessage = validateEmail(value)) }
        _uiState.update { it.copy(email = value) }
    }

    fun onPasswordChanged(value: String) {
        _exceptionState.update { it.copy("", passwordErrorMessage = validatePassword(value)) }
        _uiState.update { it.copy(password = value) }
    }

    fun signUp() {
        viewModelScope.launch {
            if (!_exceptionState.value.emailErrorMessage.isNullOrEmpty() ||
                !_exceptionState.value.passwordErrorMessage.isNullOrEmpty()) return@launch
            try {
                updateAuthState(AuthState.IN_PROGRESS)
                authService.createUser(_uiState.value.email, _uiState.value.password)
                authService.createRealtimeDbUser()
                authService.createFirestoreUser()
                updateExceptionMessage()
                updateAuthState(AuthState.SUCCESS, AuthType.SIGN_UP)
            } catch (e: Exception) {
                updateExceptionMessage(e.message!!)
                updateAuthState(AuthState.FAILURE)
            }
        }
    }

    fun logIn() {
        viewModelScope.launch {
            if (!_exceptionState.value.emailErrorMessage.isNullOrEmpty() ||
                !_exceptionState.value.passwordErrorMessage.isNullOrEmpty()) return@launch
            try {
                updateAuthState(AuthState.IN_PROGRESS)
                authService.logInUser(_uiState.value.email, _uiState.value.password)
                updateExceptionMessage()
                updateAuthState(AuthState.SUCCESS, AuthType.SIGN_IN)
            } catch (e: FirebaseAuthException) {
                updateExceptionMessage(e.message!!)
                updateAuthState(AuthState.FAILURE)
            }
        }
    }
    suspend fun googleSignInWithIntent(intent: Intent) {
        googleAuthService.signInWithIntent(intent)
        updateAuthState(AuthState.SUCCESS)
    }
    suspend fun googleSignIn(): IntentSender? {
        return googleAuthService.signIn()
    }
    private fun updateExceptionMessage(m: String = "") {
        _exceptionState.update { it.copy(exceptionMessage = m) }
    }
    private fun updateAuthState(state: AuthState, type: AuthType? = null) {
        _uiState.update { it.copy(authState = state, authType = type ?: it.authType) }
    }
}

data class AuthExceptionState(val exceptionMessage: String = "",
                              val emailErrorMessage: String? = null,
                              val passwordErrorMessage: String? = null)
data class AuthUiState(val email: String = "", val password: String = "",
                       val authState: AuthState = AuthState.IDLE,
                       val authType: AuthType = AuthType.NONE)

enum class AuthState {
    IDLE,
    SUCCESS,
    IN_PROGRESS,
    FAILURE
}
enum class AuthType {
    NONE,
    SIGN_IN,
    SIGN_UP
}