package com.example.myspeechy.screens.chat

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
import androidx.compose.runtime.setValue
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
import com.example.myspeechy.R
import com.example.myspeechy.components.BackButton
import com.example.myspeechy.components.CustomAlertDialog
import com.example.myspeechy.components.CommonTextField
import com.example.myspeechy.domain.Result
import com.example.myspeechy.presentation.chat.PictureState
import com.example.myspeechy.presentation.chat.UserProfileViewModel
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
    var name by remember(uiState.user?.name) {
        mutableStateOf(uiState.user?.name)
    }
    var info by remember(uiState.user?.info) {
        mutableStateOf(uiState.user?.info)
    }
    LaunchedEffect(Unit) {
        viewModel.startOrStopListening(false)
    }
    LaunchedEffect(uiState.accountDeletionResult) {
        if (uiState.accountDeletionResult is Result.InProgress) {
            onAccountDelete()
        }
    }
    LaunchedEffect(uiState.authResult) {
        if (uiState.authResult is Result.Error) {
            Toasty.success(context, uiState.authResult.error, Toast.LENGTH_SHORT, true).show()
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage.isNotEmpty()) {
            Toasty.error(context, uiState.errorMessage, Toast.LENGTH_SHORT, true).show()
        }
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
                    if (viewModel.normalQualityPicRef.exists() &&
                        uiState.pictureState != PictureState.DOWNLOADING) {
                        UserProfilePicture(painter = painter) {
                            if (uiState.pictureState != PictureState.UPLOADING && launcher != null) launcher.launch(
                                arrayOf("image/png", "image/jpeg")
                            )
                        }
                    }
                    if((uiState.storageMessage.isNotEmpty() || painter.state is AsyncImagePainter.State.Error)) {
                        UserProfilePicture(painter = painterResource(R.drawable.user)) {
                            launcher?.launch(
                                arrayOf("image/png", "image/jpeg")
                            )
                        }
                    }
                    if (uiState.pictureState == PictureState.DOWNLOADING || painter.state is AsyncImagePainter.State.Loading) {
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
                        CommonTextField(value = name ?: "", onChange = {name = it}, placeholders = placeholders)
                    } else {
                        if (!name.isNullOrEmpty()) {
                            UserInfoTable(value = name ?: "")
                        }
                    }
                }
                AnimatedVisibility(info != null) {
                    if (isCurrentUser) {
                        CommonTextField(value = info ?: "", onChange = {info = it}, last = true, placeholders = placeholders)
                    } else {
                         if (!info.isNullOrEmpty()) {
                             UserInfoTable(value = info ?: "")
                         }
                    }
                }
            }
            AnimatedVisibility(uiState.pictureState == PictureState.UPLOADING) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            Spacer(Modifier.weight(1f))
            if(isCurrentUser && (name != uiState.user?.name || info != uiState.user?.info)) {
                ElevatedButton(onClick = {
                    if (name != null && info != null)
                        coroutineScope.launch {
                            viewModel.changeUserInfo(name!!, info!!)
                            focusManager.clearFocus(true)
                        }
                }, modifier = Modifier.size(200.dp, 50.dp)) {
                    Text("Save")
                }
            }
            if (isCurrentUser) {
                Column(verticalArrangement = Arrangement.spacedBy(40.dp),
                    modifier = Modifier.padding(top = 25.dp)) {
                    if (uiState.pictureState != PictureState.UPLOADING &&
                        uiState.pictureState != PictureState.DOWNLOADING &&
                        uiState.storageMessage.isEmpty()) {
                        ProfileActionButton(icon = Icons.Filled.Face, text = "Delete profile picture", viewModel::removeUserPicture)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProfileActionButton(icon = Icons.AutoMirrored.Filled.ExitToApp, text = "Log out", viewModel::logout)
                        ProfileActionButton(icon = Icons.Filled.Delete, text = "Delete account", viewModel::deleteAccount)
                    }
                }
            }
            if (uiState.chatAlertDialogDataClass.title.isNotEmpty()) {
                CustomAlertDialog(
                    coroutineScope,
                    alertDialogDataClass = uiState.chatAlertDialogDataClass)
            }
        }
        if (uiState.authResult is Result.InProgress) {
            Box(modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.logout_delete_account_padding), Alignment.CenterVertically)) {
                    Text("Logging out...", style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground
                    ))
                    CircularProgressIndicator()
                }
            }
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