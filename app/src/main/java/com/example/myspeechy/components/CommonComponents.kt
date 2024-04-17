package com.example.myspeechy.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.sp
import com.example.myspeechy.R

@Composable
fun BackButton(
    onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            tint = MaterialTheme.colorScheme.secondary,
            contentDescription = null,
            modifier = Modifier.size(dimensionResource(R.dimen.back_button_size)))
    }
}

@Composable
fun TryAgainOrLogoutButton(tryAgain: Boolean,
                           onClick: () -> Unit) {
    val buttonWidth = dimensionResource(R.dimen.try_again_button_width)
    val buttonHeight = dimensionResource(R.dimen.try_again_button_height)
    ElevatedButton(onClick = onClick,
        modifier = Modifier.size(buttonWidth, buttonHeight)) {
        Text(if (tryAgain) "Try again" else "Log out",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground)
    }
}