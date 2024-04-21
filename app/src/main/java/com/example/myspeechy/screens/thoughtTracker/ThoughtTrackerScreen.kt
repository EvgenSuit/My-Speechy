package com.example.myspeechy.screens.thoughtTracker

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavHostController
import com.example.myspeechy.NavScreens
import com.example.myspeechy.R
import com.example.myspeechy.data.thoughtTrack.ThoughtTrackItem
import com.example.myspeechy.domain.Result
import com.example.myspeechy.presentation.thoughtTracker.ThoughtTrackerViewModel
import es.dmoral.toasty.Toasty

@Composable
fun ThoughtTrackerScreen(navController: NavHostController,
                         viewModel: ThoughtTrackerViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.listenForDateChange(false)
        viewModel.listenForTracks(false)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        viewModel.listenForDateChange(true)
        viewModel.listenForTracks(true)
    }
    LaunchedEffect(viewModel) {
        viewModel.tracksFetchResultFlow.collect { result ->
            if (result.error.isNotEmpty()) {
                Toasty.error(context, result.error, Toast.LENGTH_SHORT, true).show()
            }
        }
    }
    Column(Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(dimensionResource(R.dimen.thought_tracker_page_padding)),
        horizontalAlignment = Alignment.CenterHorizontally) {
        ThoughtTrackerTitle()
        Spacer(modifier = Modifier.height(100.dp))
        AnimatedContent(uiState.result, label = "") { result ->
            if (result is Result.Success) {
                TracksColumn(uiState.trackItems,
                    onFormatDate = viewModel::formatDate,
                    onNavigateToItem = {timestamp ->
                        navController.navigate("${NavScreens.ThoughtTracker}/$timestamp") })
            } else if (result is Result.InProgress) {
                Box(modifier = Modifier.fillMaxHeight(),
                    contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
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
        modifier = Modifier.fillMaxWidth(0.9f)
    ) {
        Text(date,
            style = MaterialTheme.typography.bodyMedium
                .copy(color = MaterialTheme.colorScheme.onBackground))
    }
}

@Composable
fun TracksColumn(tracks: List<ThoughtTrackItem>,
                 onFormatDate: (Long) -> String,
                 onNavigateToItem: (Long) -> Unit) {
    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.fillMaxSize()) {
        itemsIndexed(tracks) {i, item ->
            ThoughtTrackBox(onFormatDate(item.timestamp)) { onNavigateToItem(item.timestamp) }
        }
    }
}