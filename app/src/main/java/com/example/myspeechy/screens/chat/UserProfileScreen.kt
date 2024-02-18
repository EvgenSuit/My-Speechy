package com.example.myspeechy.screens.chat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
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
import java.io.ByteArrayOutputStream

@Composable
fun UserProfileScreen(viewModel: UserProfileViewModel = hiltViewModel(),
                      onOkClick: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
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
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()) {result ->
        if (result != null) {
            val img = context.contentResolver.openInputStream(result)
            val imgBytes = img?.readBytes()
            img?.close()
            if (imgBytes != null) {
                val baos = ByteArrayOutputStream()
                val bmp = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size)
                bmp.compress(Bitmap.CompressFormat.JPEG, 50, baos)
                viewModel.createPicDir()
                val file = viewModel.picRef
                file.writeBytes(imgBytes)
                viewModel.uploadUserPicture(file)
            }
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(start = 10.dp, end = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.padding(top = 50.dp)) {
            val decodedPic = BitmapFactory.decodeFile(uiState.pic!!.path)
                if (
                    //uiState.pic != null && uiState.pic!!.exists() && uiState.pic!!.readLines().isNotEmpty()
                    decodedPic != null
                    ) {

                    if (decodedPic != null) {
                        Image(decodedPic.asImageBitmap(), contentDescription = null,
                            modifier = Modifier
                                .clickable {
                                    if (!uiState.uploadingPicture) launcher.launch("image/*")
                                }
                                .size(picSize))
                    }
                }
            androidx.compose.animation.AnimatedVisibility(!uiState.uploadingPicture && uiState.pic == null
                    || uiState.storageErrorMessage == PictureStorageError.USING_DEFAULT_PROFILE_PICTURE.name,
                exit = shrinkHorizontally()
            ) {
                ElevatedButton(onClick = { launcher.launch("image/*") },
                    shape = RectangleShape,
                    modifier = Modifier.size(picSize)) {
                    Image(painter = painterResource(R.drawable.user),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize())
                }
            }
        }
        AnimatedVisibility(
            !uiState.uploadingPicture &&
                    uiState.storageErrorMessage != PictureStorageError.USING_DEFAULT_PROFILE_PICTURE.name
                    && uiState.storageErrorMessage != PictureStorageError.OBJECT_DOES_NOT_EXIST_AT_LOCATION.name,
            enter = slideInHorizontally()) {
            ElevatedButton(onClick = viewModel::removeUserPicture) {
                Text("Remove", color = Color.Red)
            }
        }
        Column(
            Modifier
                .width(300.dp)
                .padding(top = 50.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            UserInfoTextField(value = name, onChange = {name = it})
            UserInfoTextField(value = info, onChange = {info = it}, true)
        }
            AnimatedVisibility(uiState.uploadingPicture) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        Spacer(Modifier.weight(1f))
        ElevatedButton(onClick = {
            viewModel.changeUserDescription(name, info)
            onOkClick()
        }) {
            Text("OK")
        }
    }
}

@Composable
fun UserInfoTextField(value: String, onChange: (String) -> Unit, last: Boolean=false) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(value = value,
        singleLine = true,
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
        modifier = Modifier.fillMaxWidth())
}