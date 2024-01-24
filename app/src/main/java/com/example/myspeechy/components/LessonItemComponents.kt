package com.example.myspeechy.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myspeechy.R
import com.example.myspeechy.data.UiState

@Composable
fun <T: UiState> LessonItemWrapper(
    uiState: T,
    onNavigateUp: () -> Unit,
    onMarkAsComplete: () -> Unit,
    modifier: Modifier = Modifier,
    body: @Composable () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(itemBackgroundGradient)
            .verticalScroll(rememberScrollState())
            .padding(10.dp)
    ) {
        Row(modifier = Modifier.padding(bottom = 50.dp)) {
            GoBackButton(onNavigateUp,
                Modifier.align(Alignment.CenterVertically))
            Spacer(Modifier.width(40.dp))
            Text(uiState.lessonItem.title, style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterVertically))
            Spacer(Modifier.width(60.dp))
        }
        body()
        Spacer(modifier = Modifier.weight(1f))
        ElevatedButton(onClick = onMarkAsComplete) {
            Text(stringResource(id = R.string.mark_as_complete))
        }
    }
}
@Composable
fun GoBackButton(onClick: () -> Unit,
                 modifier: Modifier = Modifier) {
    IconButton(onClick = onClick,
        modifier = modifier
            .testTag("GoBackToMain")) {
        Icon(imageVector = Icons.Filled.ArrowBack,
            tint = MaterialTheme.colorScheme.onPrimary,
            contentDescription = null)
    }
}

@Composable
fun ReadingControllButton(
    text: String,
    onClick: () -> Unit) {
    ElevatedButton(onClick = { /*TODO*/ }) {
        Text(text)
    }
}