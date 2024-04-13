package com.example.myspeechy.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myspeechy.domain.Result
import com.example.myspeechy.domain.auth.AccountDeletionService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountDeletionViewModel @Inject constructor(
    private val accountDeletionService: AccountDeletionService
): ViewModel() {
    private val _uiState = MutableStateFlow(AccountDeletionUiState())
    val uiState = _uiState.asStateFlow()
    suspend fun deleteUser() {
        try {
            updateAccountDeletionResult(Result.InProgress)
            accountDeletionService.deleteUser()
            updateAccountDeletionResult(Result.Success("Successfully deleted account"))
        } catch (e: Exception) {
            updateAccountDeletionResult(Result.Error("Couldn't delete account: ${e.message}"))
        }
    }
    private fun updateAccountDeletionResult(result: Result) {
        _uiState.update { it.copy(result = result) }
    }

    data class AccountDeletionUiState(val result: Result = Result.InProgress)
}
