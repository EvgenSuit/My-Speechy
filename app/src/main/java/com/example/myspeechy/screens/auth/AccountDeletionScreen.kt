package com.example.myspeechy.screens.auth

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myspeechy.R
import com.example.myspeechy.components.BackButton
import com.example.myspeechy.components.TryAgainOrLogoutButton
import com.example.myspeechy.domain.Result
import com.example.myspeechy.presentation.auth.AccountDeletionViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.launch

@Composable
fun AccountDeletionScreen(
    onGoBack: () -> Unit,
    viewModel: AccountDeletionViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        //run first try here because doing it in view model with viewModelScope is not suitable for testing
        viewModel.deleteUser()
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(10.dp)) {
        if (Firebase.auth.currentUser != null && uiState.result.error.isNotEmpty()) {
            BackButton { onGoBack() }
        }
        Box(
            Modifier
                .weight(1f)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.logout_delete_account_padding), Alignment.CenterVertically)) {
                when(val result = uiState.result) {
                    is Result.InProgress -> {
                        Text("Deleting account...", style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onBackground
                        ))
                        CircularProgressIndicator()
                    }
                    is Result.Success -> {
                        Toasty.success(context, result.data, Toast.LENGTH_SHORT, true).show()
                    }
                    else -> {
                        Text(result.error,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground))
                        TryAgainOrLogoutButton(true) {
                            coroutineScope.launch {
                                viewModel.deleteUser()
                            }
                        }
                    }
                }
            }
        }
    }
}