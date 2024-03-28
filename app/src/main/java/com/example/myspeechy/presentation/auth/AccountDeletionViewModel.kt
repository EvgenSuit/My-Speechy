package com.example.myspeechy.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val userId = accountDeletionService.userId
    fun deleteUser() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(error = "") }
                accountDeletionService.deleteUser()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Couldn't delete account: ${e.message!!}") }
            }
        }
    }
    init {
        deleteUser()
    }

    data class AccountDeletionUiState(val error: String = "")
}