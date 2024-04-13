package com.example.myspeechy.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myspeechy.components.TryAgainOrLogoutButton

@Composable
fun ErrorScreen(error: String,
                onTryAgain: () -> Unit,
                onLogout: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()) {
        Text("Couldn't load data. Error code: $error",
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onBackground),
            textAlign = TextAlign.Center)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TryAgainOrLogoutButton(false, onLogout)
            TryAgainOrLogoutButton(true, onTryAgain)
        }
    }
}