package com.example.myspeechy.screens

import android.app.Activity.RESULT_OK
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.ImageLoader
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.decode.SvgDecoder
import com.example.myspeechy.R
import com.example.myspeechy.components.advancedShadow
import com.example.myspeechy.ui.theme.itimFamily
import com.example.myspeechy.ui.theme.kalamFamily
import com.example.myspeechy.ui.theme.lalezarFamily
import com.example.myspeechy.utils.AuthViewModel
import com.example.myspeechy.utils.isValidEmail
import com.example.myspeechy.utils.meetsPasswordRequirements
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AuthScreen(
    onNavigateToMain: () -> Unit) {
    val imageLoader = ImageLoader.Builder(LocalContext.current)
        .components {
            add(SvgDecoder.Factory())
        }.build()
    val painter = rememberAsyncImagePainter(R.raw.auth_page_background, imageLoader)
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { contentPadding ->
            Box(
                modifier = Modifier.padding(contentPadding)
            ) {
                Image(
                    painter = painter,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize(),
                    contentDescription = null
                )
                Column(modifier = Modifier
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
                    .align(Alignment.Center),/*
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center*/) {
                    if (painter.state is AsyncImagePainter.State.Loading) {
                        Box(
                            modifier = Modifier
                                .background(Color.White)
                                .fillMaxSize()
                        ) {
                            CircularProgressIndicator(
                                color = Color.Black,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                    if (painter.state is AsyncImagePainter.State.Success) {
                        MainBox(
                            onNavigateToMain = onNavigateToMain,
                            imageLoader = imageLoader,
                            snackbarHostState,
                            modifier = Modifier
                                //.align(Alignment.Center)
                        )
                    }
                }
                }
            }
    }

@Composable
fun MainBox(onNavigateToMain: () -> Unit,
            imageLoader: ImageLoader,
            snackbarHostState: SnackbarHostState,
            modifier: Modifier = Modifier) {
    val viewModel: AuthViewModel = hiltViewModel()
    val focusManager = LocalFocusManager.current
    val coroutine = rememberCoroutineScope()
    val exceptionState by viewModel.exceptionState.collectAsState()
    val uiState = viewModel.uiState
    var enabled by rememberSaveable {
        mutableStateOf(false)
    }
    enabled = exceptionState.exceptionMessage.isEmpty()
            && uiState.email.isValidEmail() && uiState.password.meetsPasswordRequirements()
    val padding = dimensionResource(id = R.dimen.padding_auth_fields)
    Box(modifier = modifier
        .border(
            width = 1.dp,
            color = Color.White,
            shape = RoundedCornerShape(30.dp)
        )
        .clip(RoundedCornerShape(30.dp))
        .background(Color.White.copy(0.4f))
        .defaultMinSize(minHeight = dimensionResource(id = R.dimen.auth_components_height))
        .width(dimensionResource(id = R.dimen.auth_components_width)))
    {
        Column (
            modifier = Modifier
                .align(Alignment.Center)
        ) {
            Text(text = stringResource(id = R.string.app_name),
                fontFamily = itimFamily,
                fontSize = 48.sp,
                color = Color.Black,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 15.dp))
           Column(modifier = Modifier.padding(start = padding, end = padding, bottom = 34.dp)) {
               AuthTextField(value = uiState.email,
                   label = "Email",
                   onValueChange = {viewModel.onEmailChanged(it)})
               AuthTextField(value = uiState.password,
                   label = "Password",
                   onValueChange = {viewModel.onPasswordChanged(it)})
               if (exceptionState.exceptionMessage.isNotEmpty()) {
                    ErrorMessage(exceptionState.exceptionMessage)
               }
               Row(horizontalArrangement = Arrangement.SpaceBetween,
                   modifier = Modifier
                       .padding(top = dimensionResource(id = R.dimen.padding_auth_button_row))
                       .fillMaxWidth()
               ) {
                   AuthButton(label = "Log In", enabled) {
                              coroutine.launch {
                                  viewModel.logIn()
                                  delay(1000)
                                  withContext(Dispatchers.Main) {
                                      if (enabled) {
                                          focusManager.clearFocus()
                                          onNavigateToMain()
                                      }
                                  }
                              }
                   }
                   AuthButton(label = "Sign Up", enabled) {
                       coroutine.launch {
                           viewModel.signUp()
                           delay(1000)
                           withContext(Dispatchers.Main) {
                               if (enabled) {
                                   focusManager.clearFocus()
                                   snackbarHostState.showSnackbar("Signed Up")
                               }
                           }
                       }
                   }
               }
               GoogleAuthButton(viewModel, imageLoader, snackbarHostState) {
                   onNavigateToMain()
               }
           }
        }
    }
}

@Composable
fun GoogleAuthButton(viewModel: AuthViewModel,
                     imageLoader: ImageLoader,
                     snackbarHostState: SnackbarHostState,
                     onClick: () -> Unit) {
    val painter = rememberAsyncImagePainter(R.raw.google_icon, imageLoader)
    val coroutine = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { result ->
            if (result.resultCode == RESULT_OK) {
                coroutine.launch {
                    viewModel.googleSignInWithIntent(
                        result.data ?: return@launch)
                    onClick()
                }
            }
        }
    )
    Button(onClick = {
        coroutine.launch {
            val signInIntentSender = viewModel.googleSignIn()
            if (signInIntentSender == null) {
                snackbarHostState.showSnackbar("Error signing in with google, make sure to" +
                        " add your account to the current device")
            } else {
                launcher.launch(
                    IntentSenderRequest.Builder(
                        intentSender = signInIntentSender
                    ).build()
                )
            }
        }},
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Blue.copy(alpha = 0.7f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(85.dp)
            .padding(top = 25.dp)
            .imePadding()
    ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Image(painter = painter,
                        contentDescription = null,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .weight(0.15f)
                    )
                    Text(stringResource(id = R.string.continue_with_google),
                        textAlign = TextAlign.Center,
                        fontSize = 17.sp,
                        color = Color.White,
                        modifier = Modifier.weight(1f))
                }
    }
}

@Composable
fun AuthTextField(value:String, label: String, onValueChange: (String) -> Unit) {
    var exceptionMessage by rememberSaveable() {
        mutableStateOf("")
    }
    val focusManager = LocalFocusManager.current
     Column {
         OutlinedTextField(value = value,
             placeholder = {Text(label,
                 fontFamily = kalamFamily,
                 color = Color.Black,
                 fontSize = 24.sp)},
             keyboardActions = KeyboardActions(
                 onDone = {if (label == "Email") {
                     focusManager.moveFocus(FocusDirection.Next)
                 }else {
                     focusManager.clearFocus()
                 }
                 }
             ),
             onValueChange = {
                 onValueChange(it)
                 if (label == "Email" && !it.isValidEmail()) {
                     exceptionMessage = "Wrong email format"
                 }
                 else if (label == "Password" && !it.meetsPasswordRequirements()) {
                     exceptionMessage = "Wrong password format"
                 }
                 else {
                     exceptionMessage = ""
                 }
             },
             singleLine = true,
             isError = exceptionMessage.isNotEmpty(),
             colors = OutlinedTextFieldDefaults.colors(
                 unfocusedContainerColor = Color.White,
                 focusedContainerColor = Color.White,
                 focusedTextColor = Color.Black,
                 unfocusedTextColor = Color.Black,
                 unfocusedBorderColor  = Color.White,
                 focusedBorderColor = Color.White,
                 errorContainerColor = Color.White,
                 errorBorderColor = Color.Red,
                 errorTextColor = Color.Black
             ),
             shape = RoundedCornerShape(10.dp),
             modifier = Modifier
                 .padding(top = dimensionResource(id = R.dimen.padding_auth_text_fields))
                 .advancedShadow(
                     alpha = 0.5f,
                     cornersRadius = 10.dp,
                     shadowBlurRadius = 10.dp,
                     offsetY = 4.dp
                 )
                 .height(60.dp)
         )
         if (exceptionMessage.isNotEmpty()) {
             ErrorMessage(exceptionMessage)
         }
     }
}

@Composable
fun AuthButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(5.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFA37EFB),
            disabledContainerColor = Color.White.copy(alpha = 0.4f),
            disabledContentColor = Color.White.copy(alpha = 1f)
        ),
        modifier = Modifier
            .width(120.dp)
            .height(50.dp)
    ) {
        Text(label, fontFamily = lalezarFamily, fontSize = 20.sp)
    }
}

@Composable
fun ErrorMessage(value: String, color: Color = Color.Red) {
    Text(value,
        color = color,
        modifier = Modifier.padding(5.dp))
}
