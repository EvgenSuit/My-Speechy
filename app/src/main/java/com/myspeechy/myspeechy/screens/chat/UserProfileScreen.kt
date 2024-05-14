package com.myspeechy.myspeechy.screens.chat

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.myspeechy.myspeechy.R
import com.myspeechy.myspeechy.components.BackButton
import com.myspeechy.myspeechy.components.CommonTextField
import com.myspeechy.myspeechy.components.CustomAlertDialog
import com.myspeechy.myspeechy.components.CustomSplashScreen
import com.myspeechy.myspeechy.domain.Result
import com.myspeechy.myspeechy.presentation.chat.PictureState
import com.myspeechy.myspeechy.presentation.chat.UserProfileViewModel
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.launch

@Composable
fun UserProfileScreen(
    onOkClick: () -> Unit,
    onAccountDelete: () -> Unit,
    viewModel: UserProfileViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val isCurrentUser = viewModel.userId == viewModel.currUserId
    val placeholders = Pair("Username", "About me")
    val focusManager = LocalFocusManager.current
    val name by remember(uiState.user?.name) {
        mutableStateOf(uiState.user?.name)
    }
    val info by remember(uiState.user?.info) {
        mutableStateOf(uiState.user?.info)
    }
    LaunchedEffect(Unit) {
        viewModel.startOrStopListening(false)
    }
    LaunchedEffect(viewModel) {
        viewModel.accountDeletionResultFlow.collect {res ->
            if (res is Result.InProgress) {
                viewModel.setDeletionResultToIdle()
                onAccountDelete()
            }
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.authResultFlow.collect {res ->
            if (res is Result.Error) {
                Toasty.error(context, res.error, Toast.LENGTH_LONG, true).show()
            }
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.errorMessageFlow.collect {e ->
            if (e.isNotEmpty()) {
                Toasty.error(context, e, Toast.LENGTH_LONG, true).show()
            }
        }
    }
    if (uiState.chatAlertDialogDataClass.title.isNotEmpty()) {
        CustomAlertDialog(
            coroutineScope,
            alertDialogDataClass = uiState.chatAlertDialogDataClass)
    }
    val picSize = dimensionResource(R.dimen.userProfilePictureSize)
    var launcher: ManagedActivityResultLauncher<Array<String>, Uri?>? = null
    if (isCurrentUser)
        launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()) {result ->
        if (result != null) {
            val img = context.contentResolver.openInputStream(result)
            val imgBytes = img?.readBytes()
            img?.close()
            if (imgBytes != null) {
                viewModel.writePicture(imgBytes)
            }
        }
    }
    Box {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.fillMaxWidth()) {
                BackButton(onOkClick)
            }
            key(uiState.recomposePic) {
                Box(contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(top = 5.dp)
                        .size(picSize)
                        .clip(CircleShape)) {
                    val painter = rememberAsyncImagePainter(model = ImageRequest.Builder(context)
                        .data(viewModel.normalQualityPicRef.path)
                        .size(coil.size.Size.ORIGINAL)
                        .build())
                    UserProfilePicture(painter = if (uiState.pictureState == PictureState.SUCCESS) painter
                        else painterResource(R.drawable.user)) {
                            launcher?.launch(
                                arrayOf("image/png", "image/jpeg")
                            )
                        }
                    if (uiState.pictureState == PictureState.DOWNLOADING || painter.state is AsyncImagePainter.State.Loading
                        || uiState.pictureState == PictureState.UPLOADING) {
                        CircularProgressIndicator(Modifier.size(50.dp))
                    }
                }
            }
            Column(
                Modifier
                    .width(300.dp)
                    .padding(top = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AnimatedVisibility(name != null) {
                    if (isCurrentUser) {
                        CommonTextField(value = name ?: "", onChange = viewModel::onUsernameChange, placeholders = placeholders)
                    } else {
                        if (!name.isNullOrEmpty()) {
                            UserInfoTable(value = name ?: "")
                        }
                    }
                }
                AnimatedVisibility(info != null) {
                    if (isCurrentUser) {
                        CommonTextField(value = info ?: "", onChange = viewModel::onInfoChange, last = true, placeholders = placeholders)
                    } else {
                         if (info != null && info!!.isNotBlank()) {
                             UserInfoTable(value = info!!)
                         }
                    }
                }
            }
            AnimatedVisibility(uiState.pictureState == PictureState.UPLOADING) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            Spacer(Modifier.weight(1f))
            if (isCurrentUser) {
                if((name != null && name!!.isNotBlank())
                    && (info != null) &&
                    (name != uiState.initUser?.name || info != uiState.initUser?.info)) {
                    ElevatedButton(onClick = {
                        coroutineScope.launch {
                            viewModel.changeUserInfo()
                            focusManager.clearFocus(true)
                        }
                    }, modifier = Modifier.size(200.dp, 50.dp)) {
                        Text("Save")
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(40.dp),
                    modifier = Modifier.padding(top = 25.dp)) {
                    if (uiState.pictureState == PictureState.SUCCESS) {
                        ProfileActionButton(icon = Icons.Filled.Face, text = "Delete profile picture", viewModel::removeUserPicture)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProfileActionButton(icon = Icons.AutoMirrored.Filled.ExitToApp, text = "Log out", viewModel::logout)
                        ProfileActionButton(icon = Icons.Filled.Delete, text = "Delete account", viewModel::deleteAccount)
                    }
                }
            }
        }
        if (uiState.authResult is Result.InProgress) {
            CustomSplashScreen()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            focusManager.clearFocus(true)
            viewModel.startOrStopListening(true)
        }
    }
}

@Composable
fun UserProfilePicture(painter: Painter, onClick: () -> Unit) {
    Image(painter,
        contentScale = ContentScale.FillBounds,
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .clickable { onClick() }
    )
}

@Composable
fun UserInfoTable(value: String) {
    val corner = dimensionResource(R.dimen.common_corner_size)
    Box(contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(corner))
            .background(MaterialTheme.colorScheme.primary)
            .fillMaxWidth()) {
        Text(value, color = MaterialTheme.colorScheme.onPrimary,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(20.dp))
    }
}

@Composable
fun ProfileActionButton(icon: ImageVector, text: String, onClick: () -> Unit) {
    ElevatedButton(onClick = onClick, Modifier.size(250.dp, 50.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()) {
            Text(text, color = MaterialTheme.colorScheme.error)
            Icon(icon, contentDescription = null)
        }
    }
}
