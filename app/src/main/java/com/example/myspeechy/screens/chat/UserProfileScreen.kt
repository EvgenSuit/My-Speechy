package com.example.myspeechy.screens.chat

import android.net.Uri
import android.util.Log
import android.widget.Toast
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.myspeechy.R
import com.example.myspeechy.components.ChatAlertDialog
import com.example.myspeechy.components.CommonTextField
import com.example.myspeechy.utils.chat.PictureState
import com.example.myspeechy.utils.chat.UserProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.skydoves.cloudy.Cloudy
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.launch

@Composable
fun UserProfileScreen(
    onOkClick: () -> Unit,
    onLogout: () -> Unit,
    viewModel: UserProfileViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val isCurrentUser = viewModel.userId == viewModel.currUserId
    val picSize = dimensionResource(R.dimen.userProfilePictureSize)
    val placeholders = Pair("Username", "Description")
    val focusManager = LocalFocusManager.current
    var name by remember(uiState.user?.name) {
        mutableStateOf(uiState.user?.name)
    }
    var info by remember(uiState.user?.info) {
        mutableStateOf(uiState.user?.info)
    }
    LaunchedEffect(Unit) {
        viewModel.startOrStopListening(false, onLogout)
    }
    LaunchedEffect(uiState.userManagementError) {
        if (uiState.userManagementError.isNotEmpty()) {
            Toasty.error(context, uiState.userManagementError, Toast.LENGTH_SHORT, true).show()
        }
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
                viewModel.writePicture(imgBytes, false, 40)
                viewModel.writePicture(imgBytes.copyOf(), true, 15)
            }
        }
    }
    Box {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.onPrimaryContainer)
                .padding(10.dp)
                .blur(if (uiState.logginOut || uiState.deletingAccount) 5.dp else 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ElevatedButton(onClick = onOkClick, modifier = Modifier.align(Alignment.Start)) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
            Box(Modifier.padding(top = 20.dp).size(picSize).clip(CircleShape)) {
                    key(uiState.recomposePic) {
                        val painter = rememberAsyncImagePainter(model = ImageRequest.Builder(LocalContext.current)
                            .data(viewModel.normalQualityPicRef.path)
                            .size(coil.size.Size.ORIGINAL)
                            .build())
                        if (viewModel.normalQualityPicRef.exists() &&
                            uiState.pictureState != PictureState.DOWNLOADING) {
                            Image(painter,
                                contentScale = ContentScale.FillBounds,
                                contentDescription = null,
                                modifier = Modifier
                                    .clickable {
                                        if (uiState.pictureState != PictureState.UPLOADING && launcher != null) launcher.launch(
                                            arrayOf("image/png", "image/jpeg")
                                        )
                                    }
                                    .clip(CircleShape)
                                    .fillMaxSize())
                        }
                        if((uiState.pictureState == PictureState.ERROR || painter.state is AsyncImagePainter.State.Error)) {
                            Image(painter = painterResource(R.drawable.user),
                                contentDescription = null,
                                modifier = Modifier
                                    .clickable {
                                        launcher?.launch(
                                            arrayOf("image/png", "image/jpeg")
                                        )
                                    }.fillMaxSize())
                        }
                        if (
                            (uiState.pictureState == PictureState.DOWNLOADING || painter.state is AsyncImagePainter.State.Loading)) {
                            CircularProgressIndicator(Modifier.align(Alignment.Center).size(50.dp))
                        }
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
                        CommonTextField(value = name ?: "", onChange = {name = it}, placeholders = placeholders)
                    } else {
                        UserInfoTable(value = name ?: "")
                    }
                }
                AnimatedVisibility(info != null) {
                    if (isCurrentUser) {
                        CommonTextField(value = info ?: "", onChange = {info = it}, last = true, placeholders = placeholders)
                    }else {
                        UserInfoTable(value = info ?: "")
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 25.dp)) {
                    AnimatedVisibility(
                        uiState.pictureState != PictureState.UPLOADING &&
                                uiState.pictureState != PictureState.IDLE && uiState.storageMessage.isEmpty(),
                        enter = slideInHorizontally(), modifier = Modifier.padding(bottom = 0.dp)) {
                        ProfileActionButton(icon = Icons.Filled.Face, text = "Delete profile picture", viewModel::removeUserPicture)
                    }
                    ProfileActionButton(icon = Icons.AutoMirrored.Filled.ExitToApp, text = "Log out", viewModel::logout)
                    ProfileActionButton(icon = Icons.Filled.Delete, text = "Delete account") {
                        viewModel.deleteAccount()
                    }
                }
            }
            if (uiState.chatAlertDialogDataClass.title.isNotEmpty()) {
                ChatAlertDialog(alertDialogDataClass = uiState.chatAlertDialogDataClass)
            }
        }
        AnimatedVisibility(uiState.logginOut) {
            AccountStatusProgress("Logging out...")
        }
        AnimatedVisibility(uiState.deletingAccount) {
            AccountStatusProgress("Deleting account...")
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.startOrStopListening(true, onLogout)
        }
    }
}

@Composable
fun AccountStatusProgress(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .height(250.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(10.dp)
                ) {
                    Text(text, color = MaterialTheme.colorScheme.onBackground)
                    LinearProgressIndicator()
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
