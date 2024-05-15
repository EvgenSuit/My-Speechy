package com.myspeechy.myspeechy.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myspeechy.myspeechy.R

@Composable
fun CustomSplashScreen() {
    Box(contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .testTag(stringResource(R.string.load_screen))) {
        Image(painterResource(R.drawable.app_launch_icon), contentDescription = null)
    }
}

@Composable
fun BackButton(
    onClick: () -> Unit) {
    val description = stringResource(R.string.back_button)
    IconButton(onClick = onClick,
        modifier = Modifier.semantics { contentDescription = description }) {
        Icon(imageVector = Icons.Filled.ArrowBack,
            tint = MaterialTheme.colorScheme.onBackground,
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


@Composable
fun ScrollDownButton(onClick: () -> Unit) {
    val corner = dimensionResource(R.dimen.common_corner_size)
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.inversePrimary),
                RoundedCornerShape(corner)
            )
            .size(dimensionResource(R.dimen.scroll_down_button_size))
            .clip(RoundedCornerShape(corner))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))
    ) {
        Icon(
            Icons.Filled.KeyboardArrowDown,
            tint = MaterialTheme.colorScheme.onSurface,
            contentDescription = null
        )
    }
}