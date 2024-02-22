package com.example.myspeechy.screens.chat

import android.net.Uri
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myspeechy.R
import com.example.myspeechy.services.chat.PictureStorageError
import com.example.myspeechy.utils.chat.UserProfileViewModel

@Composable
fun UserProfileScreen(viewModel: UserProfileViewModel = hiltViewModel(),
                      onOkClick: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isCurrentUser = viewModel.userId == viewModel.currUserId
    val picSize = dimensionResource(R.dimen.userProfilePictureSize)
    var name by rememberSaveable {
        mutableStateOf(uiState.name)
    }
    var info by rememberSaveable {
        mutableStateOf(uiState.info)
    }
    LaunchedEffect(uiState.name) {
        name = uiState.name
    }
    LaunchedEffect(uiState.info) {
        info = uiState.info
    }
    LaunchedEffect(Unit) {
        viewModel.startOrStopListening(false)
    }
    val decodedPic by remember(uiState.pic) {
        mutableStateOf(uiState.pic)
    }
    if (uiState.storageErrorMessage == PictureStorageError.PICTURE_MUST_BE_LESS_THAN_2_MB_IN_SIZE.name) {
        Toast.makeText(context, "Picture must be less than 2 mb in size", Toast.LENGTH_SHORT).show()
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
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.onPrimaryContainer)
            .padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.padding(top = 50.dp)) {
            if (decodedPic != null) {
                        Image(decodedPic!!.asImageBitmap(),
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

            androidx.compose.animation.AnimatedVisibility(
                decodedPic == null && !uiState.uploadingPicture
                    || uiState.storageErrorMessage.isNotEmpty(),
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
        AnimatedVisibility(
            decodedPic != null &&
            !uiState.uploadingPicture &&
                    uiState.storageErrorMessage.isEmpty() && isCurrentUser,
            enter = slideInHorizontally()) {
            ElevatedButton(onClick = viewModel::removeUserPicture) {
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
                        UserInfoTextField(value = name!!, onChange = {name = it})
                    } else {
                        UserInfoTable(value = name!!)
                    }
                }
                AnimatedVisibility(info != null) {
                    if (isCurrentUser) {
                        UserInfoTextField(value = info!!, onChange = {info = it}, true)
                    }else {
                        UserInfoTable(value = info!!)
                    }
                }
            }

            AnimatedVisibility(uiState.uploadingPicture) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        Spacer(Modifier.weight(1f))
        ElevatedButton(onClick = {
            if (name != null && info != null && viewModel.userId == viewModel.currUserId)
                viewModel.changeUserInfo(name!!, info!!){ onOkClick() }
            else onOkClick()
        }, modifier = Modifier.size(200.dp, 50.dp)) {
            Text("OK")
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.startOrStopListening(true)
        }
    }
}

@Composable
fun UserInfoTextField(value: String, onChange: (String) -> Unit, last: Boolean=false) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(value = value,
        singleLine = !last,
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = {
                if (last) focusManager.clearFocus()
                else focusManager.moveFocus(FocusDirection.Next)
            }
        ),
        onValueChange = onChange,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onPrimary,
            unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
            focusedBorderColor = MaterialTheme.colorScheme.inversePrimary,
            unfocusedContainerColor = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.fillMaxWidth())
}

@Composable
fun UserInfoTable(value: String) {
    Box(contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primary)
            .fillMaxWidth()) {
        Text(value, color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(20.dp))
    }
}