package com.myspeechy.myspeechy.presentation

import androidx.datastore.preferences.core.Preferences
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
class MySpeechyViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager
): ViewModel() {
    private val _uiState = MutableStateFlow(MySpeechyViewModelUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dataStoreManager.collectOnDataLoad { loaded ->
                _uiState.update { it.copy(dataLoaded = loaded) }
            }
        }
        viewModelScope.launch {
            dataStoreManager.collectAuthPreferences { pref ->
                _uiState.update { it.copy(authPreferences = pref) }
            }
        }
    }

    data class MySpeechyViewModelUiState(val dataLoaded: Boolean = false,
        val authPreferences: Preferences? = null)
}