package com.example.myspeechy.presentation.auth

import android.content.Intent
import android.content.IntentSender
import androidx.lifecycle.ViewModel
import com.example.myspeechy.R
import com.example.myspeechy.domain.Result
import com.example.myspeechy.domain.auth.AuthService
import com.example.myspeechy.domain.auth.GoogleAuthService
import com.example.myspeechy.domain.InputFormatCheckResult
import com.example.myspeechy.domain.error.EmailError
import com.example.myspeechy.domain.error.PasswordError
import com.example.myspeechy.presentation.UiText
import com.google.firebase.auth.FirebaseAuthException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authService: AuthService,
    private val googleAuthService: GoogleAuthService? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()
    private var _exceptionState = MutableStateFlow(PasswordValidatorState())
    val exceptionState = _exceptionState.asStateFlow()

    private fun validateEmailOnInput(value: String): UiText =
        when(val res = authService.validateEmail(value)) {
            is InputFormatCheckResult.Error -> {
                when (res.error) {
                    EmailError.IS_NOT_VALID -> UiText.StringResource(R.string.email_not_valid)
                    EmailError.IS_EMPTY -> UiText.StringResource(R.string.email_is_empty)
                }
            }
            is InputFormatCheckResult.Success -> { UiText.StringResource.DynamicString("") }
    }
    private fun validatePasswordOnInput(value: String): UiText =
        when(val res = authService.validatePassword(value)) {
            is InputFormatCheckResult.Error -> {
                when (res.error) {
                    PasswordError.IS_EMPTY -> UiText.StringResource(R.string.password_is_empty)
                    PasswordError.IS_NOT_MIXED_CASE -> UiText.StringResource(R.string.password_is_not_mixed_case)
                    PasswordError.IS_NOT_LONG_ENOUGH -> UiText.StringResource(R.string.password_is_not_long_enough)
                    PasswordError.NOT_ENOUGH_DIGITS -> UiText.StringResource(R.string.password_not_enough_digits)
                }
            }
            is InputFormatCheckResult.Success -> { UiText.StringResource.DynamicString("") }
        }

    fun onEmailChanged(value: String) {
        _exceptionState.update { it.copy(emailErrorMessage = validateEmailOnInput(value)) }
        _uiState.update { it.copy(email = value, result = Result.Idle) }
    }

    fun onPasswordChanged(value: String) {
        _exceptionState.update { it.copy(passwordErrorMessage = validatePasswordOnInput(value)) }
        _uiState.update { it.copy(password = value, result = Result.Idle) }
    }

    suspend fun signUp() {
        try {
            updateAuthResult(Result.InProgress)
            authService.createUser(_uiState.value.email, _uiState.value.password)
            authService.createRealtimeDbUser()
            authService.createFirestoreUser()
            updateAuthResult(Result.Success("Signed up"))
        } catch (e: Exception) {
            updateAuthResult(Result.Error(e.message!!))
        }
    }

    suspend fun logIn() {
        try {
            updateAuthResult(Result.InProgress)
            authService.logInUser(_uiState.value.email, _uiState.value.password)
            updateAuthResult(Result.Success("Logged in"))
        } catch (e: FirebaseAuthException) {
            updateAuthResult(Result.Error(e.message!!))
        }
    }
     suspend fun googleSignInWithIntent(intent: Intent) {
        try {
            googleAuthService?.signInWithIntent(intent)
            updateAuthResult(Result.Success("Logged in"))
        } catch (e: Exception) {
            updateAuthResult(Result.Error(e.message!!))
        }
    }
    //this function is called first when authenticating with google
    suspend fun googleSignIn(): IntentSender? {
        return try {
            updateAuthResult(Result.InProgress)
            googleAuthService?.signIn()
        } catch (e: Exception) {
            updateAuthResult(Result.Error(e.message!!))
            null
        }
    }
    private fun updateAuthResult(result: Result) {
        _uiState.update { it.copy(result = result) }
    }
}
data class PasswordValidatorState(val emailErrorMessage: UiText? = null,
                                  val passwordErrorMessage: UiText? = null)
data class AuthUiState(val email: String = "", val password: String = "",
                       val result: Result = Result.Idle)
