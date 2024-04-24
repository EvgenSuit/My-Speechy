package com.example.myspeechy.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myspeechy.R
import com.example.myspeechy.presentation.SettingsScreenViewModel

@Composable
fun SettingsScreen(viewModel: SettingsScreenViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val toggleTheme = stringResource(R.string.toggle_theme)
    val themeToggleIconSize = dimensionResource(R.dimen.theme_toggle_icon_size)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(30.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(toggleTheme,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                ))
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                AnimatedContent(uiState.isDarkTheme) { isDarkTheme ->
                    Icon(
                        painterResource(
                            if (isDarkTheme) R.drawable.moon_icon
                            else R.drawable.day_sunny_icon
                        ),
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = toggleTheme,
                        modifier = Modifier.size(themeToggleIconSize))
                }
                Switch(checked = uiState.isDarkTheme, onCheckedChange = {viewModel.onToggleClick()},
                    modifier = Modifier.scale(1.2f))
            }
        }

    }
}