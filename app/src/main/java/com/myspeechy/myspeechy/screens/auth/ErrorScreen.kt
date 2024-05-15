package com.myspeechy.myspeechy.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.myspeechy.myspeechy.components.TryAgainOrLogoutButton
import kotlinx.coroutines.delay

@Composable
fun ErrorScreen(error: String,
                onTryAgain: () -> Unit,
                onLogout: () -> Unit) {
    var visible by remember {
        mutableStateOf(false)
    }
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }
    AnimatedVisibility(visible,
        enter = slideInVertically { it },
        exit = slideOutVertically { it }) {
        Column(verticalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()) {
            Text("Couldn't load data. Error code: $error",
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                textAlign = TextAlign.Center)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TryAgainOrLogoutButton(false, onLogout)
                TryAgainOrLogoutButton(true, onTryAgain)
            }
        }
    }
}