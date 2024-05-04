package com.myspeechy.myspeechy.screens.thoughtTracker

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavHostController
import com.myspeechy.myspeechy.NavScreens
import com.myspeechy.myspeechy.R
import com.myspeechy.myspeechy.components.DialogBox
import com.myspeechy.myspeechy.data.thoughtTrack.ThoughtTrackItem
import com.myspeechy.myspeechy.domain.Result
import com.myspeechy.myspeechy.presentation.thoughtTracker.ThoughtTrackerViewModel
import es.dmoral.toasty.Toasty

@Composable
fun ThoughtTrackerScreen(navController: NavHostController,
                         viewModel: ThoughtTrackerViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val circularProgressIndicatorDescription = stringResource(R.string.circular_progress_indicator)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.listenForTracks(false)
        viewModel.listenForDateChange(false)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        viewModel.listenForTracks(true)
        viewModel.listenForDateChange(true)
    }
    LaunchedEffect(viewModel) {
        viewModel.tracksFetchResultFlow.collect { result ->
            if (result.error.isNotEmpty()) {
                Toasty.error(context, result.error, Toast.LENGTH_LONG, true).show()
            }
        }
    }
    if (!uiState.wasWelcomeDialogShown) {
        DialogBox(title = stringResource(R.string.thought_tracker), text = stringResource(R.string.thought_tracker_description),
            viewModel::onDialogDismiss)
    }
    BoxWithConstraints(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxSize()
            .blur(if (!uiState.wasWelcomeDialogShown) dimensionResource(R.dimen.blur) else 0.dp)
    ) {
        val maxHeight = this.maxHeight
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .let { if (maxHeight > 400.dp) it.padding(dimensionResource(R.dimen.thought_tracker_page_padding)) else it },
            horizontalAlignment = Alignment.CenterHorizontally) {
            ThoughtTrackerTitle()
            if (maxHeight > 400.dp) Spacer(Modifier.height(100.dp))
            AnimatedContent(uiState.result, label = "") { result ->
                if (result is Result.Success) {
                    TracksColumn(uiState.trackItems,
                        onFormatDate = viewModel::formatDate,
                        onNavigateToItem = {timestamp ->
                            navController.navigate("${NavScreens.ThoughtTracker}/$timestamp") })
                } else if (result is Result.InProgress) {
                    Box(modifier = Modifier.fillMaxHeight(),
                        contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.semantics { contentDescription = circularProgressIndicatorDescription})
                    }
                }
            }
        }
    }
}

@Composable
fun ThoughtTrackerTitle() {
    Text(stringResource(R.string.thought_tracker),
        style = MaterialTheme.typography.titleLarge
            .copy(color = MaterialTheme.colorScheme.onBackground))
}

@Composable
fun ThoughtTrackBox(date: String,
                    onNavigateToItem: () -> Unit) {
    ElevatedButton(onClick = onNavigateToItem,
        shape = RoundedCornerShape(dimensionResource(R.dimen.common_corner_size)),
        modifier = Modifier.width(dimensionResource(R.dimen.thought_tracker_width))
    ) {
        Text(date,
            style = MaterialTheme.typography.labelMedium
                .copy(color = MaterialTheme.colorScheme.onBackground),
            modifier = Modifier.padding(7.dp))
    }
}

@Composable
fun TracksColumn(tracks: List<ThoughtTrackItem>,
                 onFormatDate: (String) -> String,
                 onNavigateToItem: (String) -> Unit) {
    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.fillMaxSize()) {
        itemsIndexed(tracks) {i, item ->
            ThoughtTrackBox(onFormatDate(item.date)) { onNavigateToItem(item.date) }
        }
    }
}