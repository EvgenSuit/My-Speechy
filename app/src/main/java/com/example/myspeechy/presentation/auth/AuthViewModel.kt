package com.example.myspeechy.presentation.auth

import android.content.Intent
import android.content.IntentSender
import androidx.lifecycle.ViewModel
import com.example.myspeechy.services.auth.AuthService
import com.example.myspeechy.services.auth.GoogleAuthService
import com.example.myspeechy.services.Result
import com.example.myspeechy.services.error.EmailError
import com.example.myspeechy.services.error.PasswordError
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

    suspend fun signUp() {
        if (!_exceptionState.value.emailErrorMessage.isNullOrEmpty() ||
            !_exceptionState.value.passwordErrorMessage.isNullOrEmpty()) return
        try {
            _exceptionState.update { it.copy(authState = AuthState.IN_PROGRESS) }
            authService.createUser(_uiState.value.email, _uiState.value.password)
            authService.createRealtimeDbUser()
            authService.createFirestoreUser()
            _exceptionState.update {
                it.copy(
                    exceptionMessage = "",
                    authState = AuthState.SUCCESS
                )
            }
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
        if (!_exceptionState.value.emailErrorMessage.isNullOrEmpty() ||
            !_exceptionState.value.passwordErrorMessage.isNullOrEmpty()) return
        try {
            _exceptionState.update { it.copy(authState = AuthState.IN_PROGRESS) }
            authService.logInUser(_uiState.value.email, _uiState.value.password)
            _exceptionState.update { it.copy(exceptionMessage = "", authState = AuthState.SUCCESS) }
        } catch (e: Exception) {
            _exceptionState.update { it.copy(exceptionMessage = e.message ?: "", authState = AuthState.FAILURE) }
        }
    }
    suspend fun googleSignInWithIntent(intent: Intent) {
        googleAuthService.signInWithIntent(intent)
    }
    suspend fun googleSignIn(): IntentSender? {
        return googleAuthService.signIn()
    }
}

data class AuthExceptionState(val exceptionMessage: String = "",
                              val emailErrorMessage: String? = null,
                              val passwordErrorMessage: String? = null,
                              val authState: AuthState = AuthState.IDLE)
data class AuthUiState(val email: String = "", val password: String = "")

enum class AuthState {
    IDLE,
    SUCCESS,
    IN_PROGRESS,
    FAILURE
}
