package com.myspeechy.myspeechy.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myspeechy.myspeechy.R
import com.myspeechy.myspeechy.presentation.SettingsScreenViewModel

@Composable
fun SettingsScreen(viewModel: SettingsScreenViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val icons8Link = "icons8.com"
    val privacyPolicyLink = stringResource(R.string.privacy_policy_link)
    BoxWithConstraints {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(30.dp)
                .let {
                    if (maxHeight < 500.dp) it
                        .height(500.dp)
                        .verticalScroll(rememberScrollState()) else it
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(30.dp)
        ) {
            ThemeSwitcher(uiState.isDarkTheme) {
                viewModel.onToggleClick()
            }
            PrivacyPolicySection {
                val urlIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(privacyPolicyLink)
                )
                context.startActivity(urlIntent)
            }
            Spacer(Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()) {
                Text("Icons by ")
                Link(icons8Link) {
                    val urlIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.${icons8Link}")
                    )
                    context.startActivity(urlIntent)
                }
            }
        }
    }
}

@Composable
fun ThemeSwitcher(
    isDark: Boolean,
    onClick: (Boolean) -> Unit
) {
    val toggleTheme = stringResource(R.string.toggle_theme)
    val themeToggleIconSize = dimensionResource(R.dimen.theme_toggle_icon_size)
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
            AnimatedContent(isDark) { isDarkTheme ->
                Icon(
                    painterResource(
                        if (isDarkTheme) R.drawable.moon_icon
                        else R.drawable.day_sunny_icon
                    ),
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = toggleTheme,
                    modifier = Modifier.size(themeToggleIconSize))
            }
            Switch(checked = isDark, onCheckedChange = onClick,
                modifier = Modifier.scale(1.2f))
        }
    }
}

@Composable
fun PrivacyPolicySection(onPrivacyPolicyNavigate: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPrivacyPolicyNavigate() }
    ) {
        Text(
            stringResource(R.string.privacy_policy),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onBackground
            ))
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            Icons.Filled.ArrowForward,
            contentDescription = null)
    }
}

@Composable
fun Link(link: String,
         onClick: () -> Unit) {
    Text(link, style = MaterialTheme.typography.labelSmall.copy(
        textDecoration = TextDecoration.Underline,
        color = MaterialTheme.colorScheme.onBackground
    ),
        modifier = Modifier.clickable { onClick() })
}