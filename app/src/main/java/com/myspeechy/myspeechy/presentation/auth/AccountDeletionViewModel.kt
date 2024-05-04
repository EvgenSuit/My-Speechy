package com.myspeechy.myspeechy.presentation.auth


import android.content.Intent
import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myspeechy.myspeechy.domain.Result
import com.myspeechy.myspeechy.domain.auth.AccountDeletionService
import com.myspeechy.myspeechy.domain.auth.AuthService
import com.myspeechy.myspeechy.domain.auth.GoogleAuthService
import com.myspeechy.myspeechy.domain.useCases.ValidateEmailUseCase
import com.myspeechy.myspeechy.domain.useCases.ValidatePasswordUseCase
import com.myspeechy.myspeechy.presentation.UiText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountDeletionViewModel @Inject constructor(
    private val accountDeletionService: AccountDeletionService,
    private val authService: AuthService,
    private val googleAuthService: GoogleAuthService? = null,
    private val validateEmailUseCase: ValidateEmailUseCase,
    private val validatePasswordUseCase: ValidatePasswordUseCase,
    private val auth: FirebaseAuth
): ViewModel() {
    private val _uiState = MutableStateFlow(AccountDeletionUiState())
    val uiState = _uiState.asStateFlow()
    var authResultFlow = _uiState.map { it.authResult }

    suspend fun deleteUser() {
        try {
            // for testing purposes throw an appropriate exception on first call
            /*if (_uiState.value.deletionResult !is Result.Error) {
                throw FirebaseAuthRecentLoginRequiredException("error", "error")
            }*/
            updateAccountDeletionResult(Result.InProgress)
            updateAuthResult(Result.Idle)
            accountDeletionService.deleteUser()
            updateAccountDeletionResult(Result.Success("Successfully deleted account"))
        } catch (e: Exception) {
            if (e is FirebaseAuthRecentLoginRequiredException) {
                updateProvider()
            }
            updateAccountDeletionResult(Result.Error("Couldn't delete account: ${e.message}"))
        }
    }
    private fun updateProvider() {
        auth.currentUser?.providerData?.forEach { userInfo ->
            _uiState.update { it.copy(authProvider = when(userInfo.providerId) {
                "password" -> AuthProvider.EMAIL_AND_PASSWORD
                else -> AuthProvider.GOOGLE
            }) }
        }
    }
    suspend fun logIn() {
        try {
            updateAuthResult(Result.InProgress)
            authService.logInUser(_uiState.value.email, _uiState.value.password)
            updateAuthResult(Result.Success("Logged in"))
            _uiState.update { it.copy(authProvider = AuthProvider.NONE) }
            deleteUser()
        } catch (e: FirebaseAuthException) {
            updateAuthResult(Result.Error(e.message!!))
        }
    }
    suspend fun googleSignInWithIntent(intent: Intent) {
        try {
            googleAuthService?.signInWithIntent(intent)
            updateAuthResult(Result.Success("Logged in"))
        } catch (e: Exception) {
            updateAuthResult(Result.Error("${e.message!!}. Make sure to add your Google account to this device"))
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
    private fun updateAccountDeletionResult(result: Result) {
        _uiState.update { it.copy(deletionResult = result) }
    }
    private fun updateAuthResult(result: Result) {
        _uiState.update { it.copy(authResult = result) }
    }

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(
            email = value,
            emailErrorMessage = validateEmailUseCase(value),
            authResult = Result.Idle) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(
            password = value,
            passwordErrorMessage = validatePasswordUseCase(value),
            authResult = Result.Idle) }
    }
    data class AccountDeletionUiState(
        val email: String = "",
        val password: String = "",
        val emailErrorMessage: UiText? = null,
        val passwordErrorMessage: UiText? = null,
        val authProvider: AuthProvider = AuthProvider.NONE,
        val authResult: Result = Result.Idle,
        val deletionResult: Result = Result.InProgress)

}

enum class AuthProvider {
    NONE,
    GOOGLE,
    EMAIL_AND_PASSWORD
}