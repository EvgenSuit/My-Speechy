package com.myspeechy.myspeechy.components

import android.Manifest
import android.os.Build
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.myspeechy.myspeechy.R
import com.myspeechy.myspeechy.data.lesson.Lesson
import com.myspeechy.myspeechy.data.lesson.LessonCategories
import com.myspeechy.myspeechy.data.lesson.LessonItemUiState
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun <T: LessonItemUiState> LessonItemWrapper(
    uiState: T,
    contentArrangement: Arrangement.Vertical = Arrangement.Top,
    scrollState: ScrollState = rememberScrollState(),
    dialogText: String,
    onDialogDismiss: () -> Unit,
    onNavigateUp: () -> Unit,
    onMarkAsComplete: () -> Unit,
    modifier: Modifier = Modifier,
    body: @Composable () -> Unit,
) {
    val itemBackgroundGradient = Brush.linearGradient(
        colorStops = arrayOf(
            0f to MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            0.4f to MaterialTheme.colorScheme.inversePrimary.copy(alpha = 0.7f),
            1f to MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.7f)
        )
    )
    val lessonItem = uiState.lessonItem
    var showDialog by remember {
        mutableStateOf(true)
    }
    var notificationPermissionGranted by remember {
        mutableStateOf(false)
    }
    if (!uiState.wasWelcomeDialogBoxShown && Build.VERSION.SDK_INT > 32 && lessonItem.category == LessonCategories.MEDITATION) {
        val permissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        notificationPermissionGranted = permissionState.status.isGranted
        if (!showDialog && !notificationPermissionGranted) {
            LaunchedEffect(Unit) {
                permissionState.launchPermissionRequest()
            }
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = contentArrangement,
        modifier = modifier
            .fillMaxSize()
            .background(itemBackgroundGradient)
            .verticalScroll(scrollState)
            .padding(10.dp)
            .blur(if (lessonItem.title.isNotEmpty() && !uiState.wasWelcomeDialogBoxShown && showDialog) 20.dp else 0.dp)
    ) {
        Row(modifier = Modifier.padding(bottom = 30.dp),
            verticalAlignment = Alignment.CenterVertically) {
            BackButton(onNavigateUp)
            Text(lessonItem.title, style = MaterialTheme.typography.titleLarge
                .copy(MaterialTheme.colorScheme.onBackground),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .wrapContentSize(Alignment.Center)
                    .fillMaxWidth()
            )
        }
        body()
        Spacer(modifier = Modifier.height(50.dp))
        ElevatedButton(
            enabled = !lessonItem.isComplete,
            onClick = onMarkAsComplete) {
            Text(stringResource(id = R.string.mark_as_complete),
                fontSize = 23.sp, color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.wrapContentSize(Alignment.Center))
        }
        if (lessonItem.category == LessonCategories.READING) {
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.show_control_bar_button_height) +
                    dimensionResource(R.dimen.show_control_bar_button_padding)))
        }
    }
    if (lessonItem.title.isNotEmpty()
        && !uiState.wasWelcomeDialogBoxShown && showDialog) {
        val title = "${lessonItem.category.name.lowercase().replaceFirstChar { it.titlecase() }} exercise"
        DialogBox(title, dialogText +
        if (lessonItem.category == LessonCategories.MEDITATION &&
            Build.VERSION.SDK_INT > 32 && !notificationPermissionGranted) " Allow notifications in order for you to be able" +
                " to control your progress even when the app is in the background" else "")
        { showDialog = false
        onDialogDismiss()
        }
    }

}

@Composable
fun DialogBox(title: String, text: String,
              onDismiss: () -> Unit) {
    val corner = dimensionResource(R.dimen.common_corner_size)
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
            shape = RoundedCornerShape(corner)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(30.dp),
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxSize()
            ) {
                Text(title,
                    style = MaterialTheme.typography.headlineMedium
                        .copy(color = MaterialTheme.colorScheme.onBackground),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.wrapContentSize(Alignment.Center))
                Text(text,
                    style = MaterialTheme.typography.bodyMedium
                        .copy(color = MaterialTheme.colorScheme.onBackground),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.wrapContentSize(Alignment.Center))
                ElevatedButton(onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Green),
                    modifier = Modifier.defaultMinSize(100.dp)) {
                    Text("OK", fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
    }
}


