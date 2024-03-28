package com.example.myspeechy.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myspeechy.components.BackButton
import com.example.myspeechy.presentation.auth.AccountDeletionViewModel

@Composable
fun AccountDeletionScreen(
    onGoBack: (String) -> Unit,
    viewModel: AccountDeletionViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        Modifier
            .fillMaxSize()
            .padding(10.dp)) {
        if (viewModel.userId != null && uiState.error.isNotEmpty()) {
            BackButton { onGoBack(viewModel.userId) }
        }
        Box(
            Modifier
                .weight(1f)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(120.dp, Alignment.CenterVertically)) {
                if (uiState.error.isEmpty()) {
                    Text("Deleting account...", style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground
                    ))
                    CircularProgressIndicator()
                } else Text(uiState.error,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground))

                if (uiState.error.isNotEmpty()) {
                    ElevatedButton(onClick = viewModel::deleteUser,
                        modifier = Modifier.size(150.dp, 50.dp)) {
                        Text("Try again",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
        }
    }
}