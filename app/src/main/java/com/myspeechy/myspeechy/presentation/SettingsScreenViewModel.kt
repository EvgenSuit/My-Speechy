package com.myspeechy.myspeechy.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myspeechy.myspeechy.data.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsScreenViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager
): ViewModel(){
    private val _uiState = MutableStateFlow(SettingsScreenUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dataStoreManager.collectThemeMode { darkTheme ->
                _uiState.update { it.copy(darkTheme) }
            }
        }
    }

    fun onToggleClick() {
        viewModelScope.launch {
            dataStoreManager.editTheme(!uiState.value.isDarkTheme)
        }
    }

    data class SettingsScreenUiState(val isDarkTheme: Boolean = false)
}