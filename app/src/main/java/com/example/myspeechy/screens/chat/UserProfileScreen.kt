package com.example.myspeechy.screens.chat

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.myspeechy.R
import com.example.myspeechy.components.CommonTextField
import com.example.myspeechy.utils.chat.UserProfileViewModel

@Composable
fun UserProfileScreen(viewModel: UserProfileViewModel = hiltViewModel(),
                      onOkClick: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isCurrentUser = viewModel.userId == viewModel.currUserId
    val picSize = dimensionResource(R.dimen.userProfilePictureSize)
    var retryHash by remember(uiState.recomposePic) { mutableStateOf(0) }
    val placeholders = Pair("Username", "Description")
    var name by remember(uiState.name) {
        mutableStateOf(uiState.name)
    }
    var info by remember(uiState.info) {
        mutableStateOf(uiState.info)
    }
    LaunchedEffect(Unit) {
        viewModel.startOrStopListening(false)
    }
    var launcher: ManagedActivityResultLauncher<Array<String>, Uri?>? = null
    if (isCurrentUser)
        launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()) {result ->
        if (result != null) {
            val img = context.contentResolver.openInputStream(result)
            val imgBytes = img?.readBytes()
            img?.close()
            if (imgBytes != null) {
                viewModel.writePicture(imgBytes, false, 50)
                viewModel.writePicture(imgBytes.copyOf(), true, 20)
            }
        }
    }
    Box {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.onPrimaryContainer)
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ElevatedButton(onClick = onOkClick, modifier = Modifier.align(Alignment.Start)) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
            key(uiState.recomposePic) {
                Box(Modifier.padding(top = 20.dp)) {
                    val painter = rememberAsyncImagePainter(model = ImageRequest.Builder(LocalContext.current)
                        .data(viewModel.normalQualityPicRef.path)
                        .size(coil.size.Size.ORIGINAL)
                        .setParameter("retry_hash", retryHash)
                        .build())
                    if (painter.state is AsyncImagePainter.State.Error) {retryHash++}
                    if (viewModel.normalQualityPicRef.exists()) {
                        Image(painter,
                            contentScale = ContentScale.FillBounds,
                            contentDescription = null,
                            modifier = Modifier
                                .clickable {
                                    if (!uiState.uploadingPicture && launcher != null) launcher.launch(
                                        arrayOf("image/png", "image/jpeg")
                                    )
                                }
                                .size(picSize))
                    }
                    this@Column.AnimatedVisibility(!uiState.uploadingPicture
                            && (uiState.storageMessage.isNotEmpty() || (painter.state is AsyncImagePainter.State.Error)),
                        enter = slideInHorizontally(),
                        exit = shrinkHorizontally()
                    ) {
                        Image(painter = painterResource(R.drawable.user),
                            contentDescription = null,
                            modifier = Modifier
                                .size(picSize)
                                .clickable {
                                    if (!uiState.uploadingPicture && launcher != null) launcher.launch(
                                        arrayOf("image/png", "image/jpeg")
                                    )
                                })
                    }
                }
            }
            AnimatedVisibility(
                !uiState.uploadingPicture &&
                        uiState.storageMessage.isEmpty() && isCurrentUser,
                enter = slideInHorizontally()) {
                ElevatedButton(onClick = viewModel::removeUserPicture,
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 10.dp
                    ),
                    modifier = Modifier.width(200.dp)) {
                    Icon(imageVector = Icons.Filled.Delete,
                        tint = MaterialTheme.colorScheme.error,
                        contentDescription = null)
                }
            }

            Column(
                Modifier
                    .width(300.dp)
                    .padding(top = 50.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AnimatedVisibility(name != null) {
                    if (isCurrentUser) {
                        CommonTextField(value = name!!, onChange = {name = it}, placeholders = placeholders)
                    } else {
                        UserInfoTable(value = name!!)
                    }
                }
                AnimatedVisibility(info != null) {
                    if (isCurrentUser) {
                        CommonTextField(value = info!!, onChange = {info = it}, true, placeholders = placeholders)
                    }else {
                        UserInfoTable(value = info!!)
                    }
                }
            }

            AnimatedVisibility(uiState.uploadingPicture) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            Spacer(Modifier.weight(1f))
            if(isCurrentUser && (name != uiState.name || info != uiState.info)) {
                ElevatedButton(onClick = {
                    if (name != null && info != null)
                        viewModel.changeUserInfo(name!!, info!!){ onOkClick() }
                    else onOkClick()
                }, modifier = Modifier.size(200.dp, 50.dp)) {
                    Text("Save")
                }
            }
        }
        DisposableEffect(Unit) {
            onDispose {
                viewModel.startOrStopListening(true)
            }
        }
    }
}


@Composable
fun UserInfoTable(value: String) {
    Box(contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primary)
            .fillMaxWidth()) {
        Text(value, color = MaterialTheme.colorScheme.onPrimary,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(20.dp))
    }
}