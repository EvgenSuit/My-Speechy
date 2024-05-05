package com.myspeechy.myspeechy.presentation.auth

import android.content.Intent
import android.content.IntentSender
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuthException
import com.myspeechy.myspeechy.domain.Result
import com.myspeechy.myspeechy.domain.auth.AuthService
import com.myspeechy.myspeechy.domain.auth.GoogleAuthService
import com.myspeechy.myspeechy.domain.useCases.ValidateEmailUseCase
import com.myspeechy.myspeechy.domain.useCases.ValidatePasswordUseCase
import com.myspeechy.myspeechy.domain.useCases.ValidateUsernameUseCase
import com.myspeechy.myspeechy.presentation.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authService: AuthService,
    private val googleAuthService: GoogleAuthService? = null,
    private val validateUsernameUseCase: ValidateUsernameUseCase,
    private val validateEmailUseCase: ValidateEmailUseCase,
    private val validatePasswordUseCase: ValidatePasswordUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()
    private var _exceptionState = MutableStateFlow(PasswordValidatorState())
    val exceptionState = _exceptionState.asStateFlow()

    private fun validateUsernameOnInput(value: String): UiText = validateUsernameUseCase(value)
    private fun validateEmailOnInput(value: String): UiText = validateEmailUseCase(value)
    private fun validatePasswordOnInput(value: String): UiText = validatePasswordUseCase(value)

    fun onUsernameChanged(value: String) {
        _exceptionState.update { it.copy(usernameErrorMessage = validateUsernameOnInput(value)) }
        _uiState.update { it.copy(username = value, result = Result.Idle) }
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
            authService.createRealtimeDbUser(_uiState.value.username)
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
            updateAuthResult(Result.Error("${e.message!!} Make sure to add your Google account to this device"))
            null
        }
    }
    fun updateAuthOption() {
        _uiState.update { it.copy(logIn = !it.logIn) }
    }
    private fun updateAuthResult(result: Result) {
        _uiState.update { it.copy(result = result) }
    }
}
data class PasswordValidatorState(
    val usernameErrorMessage: UiText? = null,
    val emailErrorMessage: UiText? = null,
                                  val passwordErrorMessage: UiText? = null)
data class AuthUiState(
    val username: String = "",
    val email: String = "",
                       val password: String = "",
    val logIn: Boolean = true,
                       val result: Result = Result.Idle)
