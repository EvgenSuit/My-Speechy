package com.example.myspeechy.components

import android.Manifest
import android.os.Build
import android.util.Log
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myspeechy.R
import com.example.myspeechy.data.lesson.LessonItemUiState
import com.example.myspeechy.helpers.LessonCategories
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun <T: LessonItemUiState> LessonItemWrapper(
    uiState: T,
    onNavigateUp: () -> Unit,
    onMarkAsComplete: () -> Unit,
    modifier: Modifier = Modifier,
    body: @Composable () -> Unit,
) {
    val lessonItem = uiState.lessonItem
    var showDialog by remember {
        mutableStateOf(true)
    }
    var notificationPermissionGranted by remember {
        mutableStateOf(false)
    }
    if (Build.VERSION.SDK_INT > 32) {
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
        modifier = modifier
            .fillMaxSize()
            .background(itemBackgroundGradient)
            .verticalScroll(rememberScrollState())
            .padding(10.dp)
            //show blur and dialog box only for item of the first unit
            .blur(if (lessonItem.unit == 1 && lessonItem.title.isNotEmpty() && !lessonItem.isComplete && showDialog) 20.dp else 0.dp)
    ) {
        Row(modifier = Modifier.padding(bottom = 30.dp),
            verticalAlignment = Alignment.CenterVertically) {
            BackButton(onNavigateUp)
            Text(lessonItem.title, style = MaterialTheme.typography.titleLarge,
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
    }
    if (lessonItem.category == LessonCategories.MEDITATION && lessonItem.title.isNotEmpty()
        && !lessonItem.isComplete && showDialog) {
        val title = "${lessonItem.category} exercise"
        DialogBox(title, categoryToDialogText(lessonItem.category) +
        if (Build.VERSION.SDK_INT > 32 && !notificationPermissionGranted) "Allow notifications in order for you to be able" +
                " to control meditation progress even if the app is in the background" else "")
        {
            showDialog = false
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
                    style = MaterialTheme.typography.bodySmall
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


fun categoryToDialogText(category: LessonCategories): String {
    return when(category) {
        LessonCategories.PSYCHOLOGICAL -> "This type of exercise is designed to help you fight your mental battles"
        LessonCategories.READING -> "With this type of exercise you can practice your slow reading skills." +
                "This is a useful way to manage stuttering. Use the slider at the bottom " +
                "to control the speed"
        LessonCategories.MEDITATION -> "This type of exercise targets your ability to focus" +
                " and brings your mind at peace."
        else -> ""
    }
}