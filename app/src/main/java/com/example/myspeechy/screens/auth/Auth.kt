package com.example.myspeechy.screens.auth

import android.content.Intent
import android.content.IntentSender
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
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
import com.example.myspeechy.domain.Result
import com.example.myspeechy.presentation.UiText
import com.example.myspeechy.presentation.auth.AuthViewModel
import com.example.myspeechy.ui.theme.itimFamily
import com.example.myspeechy.ui.theme.kalamFamily
import com.example.myspeechy.ui.theme.lalezarFamily
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onNavigateToMain: () -> Unit) {
    val focusManger = LocalFocusManager.current
    val imageLoader = ImageLoader.Builder(LocalContext.current)
        .components {
            add(SvgDecoder.Factory())
        }.build()
    val painter = rememberAsyncImagePainter(R.raw.auth_page_background, imageLoader)
    Box(contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(Color.White)){
            Image(
                painter = painter,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
                contentDescription = null
            )
        if (painter.state is AsyncImagePainter.State.Loading) {
            CircularProgressIndicator(
                color = Color.Black,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        if (painter.state is AsyncImagePainter.State.Success) {
            MainBox(
                onNavigateToMain = onNavigateToMain,
                imageLoader = imageLoader)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            focusManger.clearFocus(true)
        }
    }
}


@Composable
fun MainBox(onNavigateToMain: () -> Unit,
            imageLoader: ImageLoader,
            modifier: Modifier = Modifier) {
    val viewModel: AuthViewModel = hiltViewModel()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val exceptionState by viewModel.exceptionState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val padding = dimensionResource(id = R.dimen.padding_auth_fields)
    val linearProgressBarDescription = stringResource(R.string.waiting_for_auth)
    val emailError by remember(exceptionState.emailErrorMessage) {
        mutableStateOf(exceptionState.emailErrorMessage?.let { e ->
            when (e) {
                is UiText.StringResource -> e.asString(context)
                is UiText.StringResource.DynamicString -> e.s
            }
        })
    }
    val passwordError by remember(exceptionState.passwordErrorMessage) {
        mutableStateOf(exceptionState.passwordErrorMessage?.let { e ->
            when (e) {
                is UiText.StringResource -> e.asString(context)
                is UiText.StringResource.DynamicString -> e.s
            }
        })
    }

    LaunchedEffect(uiState.result) {
        if (uiState.result is Result.Success) {
            focusManager.clearFocus(true)
            Toasty.success(context, uiState.result.data, Toast.LENGTH_SHORT, true).show()
            onNavigateToMain()
        } else if (uiState.result is Result.Error) {
            Toasty.error(context, uiState.result.error, Toast.LENGTH_LONG, true).show()
        }
    }
    Box(modifier = modifier
        .border(
            width = 1.dp,
            color = Color.White,
            shape = RoundedCornerShape(30.dp)
        )
        .clip(RoundedCornerShape(30.dp))
        .background(Color.White.copy(0.4f))
        .defaultMinSize(minHeight = dimensionResource(id = R.dimen.auth_components_height))
        .width(dimensionResource(id = R.dimen.auth_components_width))
        .verticalScroll(rememberScrollState()))
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
                    .padding(top = 15.dp, bottom = 15.dp))
           Column(modifier = Modifier.padding(start = padding, end = padding, bottom = 34.dp)) {
               AuthTextField(value = uiState.email,
                   exceptionMessage = emailError ?: "",
                   label = "Email",
                   modifier = Modifier.clickable(onClickLabel = stringResource(R.string.email_auth_field_click_label),
                       onClick = {}),
                   onValueChange = {viewModel.onEmailChanged(it)})
               if (!emailError.isNullOrEmpty()) {
                   ErrorMessage(emailError!!)
               }
               AuthTextField(value = uiState.password,
                   exceptionMessage = passwordError ?: "",
                   label = "Password",
                   modifier = Modifier.clickable(onClickLabel = stringResource(R.string.password_auth_field_click_label),
                       onClick = {}),
                   onValueChange = {viewModel.onPasswordChanged(it)})
               if (!passwordError.isNullOrEmpty()) {
                   ErrorMessage(passwordError!!)
               }
               AnimatedVisibility(uiState.result !is Result.InProgress) {
                   AuthButtons(
                       enabled = emailError?.isEmpty() == true &&
                       passwordError?.isEmpty() == true && uiState.result !is Result.Error,
                       onLogIn = {
                           coroutineScope.launch {
                               viewModel.logIn()
                           }
                       },
                       onSignUp = {
                           coroutineScope.launch {
                               viewModel.signUp()
                           }
                       })
               }
               AnimatedVisibility(uiState.result is Result.InProgress) {
                   LinearProgressIndicator(modifier = Modifier
                       .padding(top = dimensionResource(R.dimen.padding_auth_button_row))
                       .semantics { stateDescription = linearProgressBarDescription })
               }
               GoogleAuthButton(imageLoader = imageLoader,
                        enabled = uiState.result !is Result.InProgress,
                       onGoogleSignIn = viewModel::googleSignIn,
                       onGoogleSignInWithIntent = viewModel::googleSignInWithIntent)
           }
        }
    }
}

@Composable
fun AuthButtons(
    enabled: Boolean,
    onLogIn: () -> Unit,
    onSignUp: () -> Unit) {
    val signUp = stringResource(R.string.sign_up)
    val logIn = stringResource(R.string.log_in)
    Row(horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .padding(top = dimensionResource(id = R.dimen.padding_auth_button_row))
            .fillMaxWidth()
    ) {
        AuthButton(label = logIn, enabled,
            Modifier.clickable(onClickLabel = logIn, onClick = {}), onLogIn)
        AuthButton(label = signUp, enabled,
            Modifier.clickable(onClickLabel = signUp, onClick = {}), onSignUp)
    }
}

@Composable
fun GoogleAuthButton(imageLoader: ImageLoader,
                     enabled: Boolean,
                     onGoogleSignIn: suspend () -> IntentSender?,
                     onGoogleSignInWithIntent: suspend (Intent) -> Unit) {
    val painter = rememberAsyncImagePainter(R.raw.google_icon, imageLoader)
    val coroutine = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val res = result.data
        if (res != null) {
            coroutine.launch {
                onGoogleSignInWithIntent(res)
            }
        }
    }
    Button(onClick = {
        coroutine.launch {
            val signInIntentSender = onGoogleSignIn()
            if (signInIntentSender != null) {
                launcher.launch(
                    IntentSenderRequest.Builder(
                        intentSender = signInIntentSender
                    ).build()
                )
            }
        }
        },
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Blue.copy(alpha = 0.7f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .size(105.dp, 85.dp)
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
fun AuthTextField(value:String, label: String,
                  exceptionMessage: String,
                  modifier: Modifier,
                  colors: TextFieldColors? = null,
                  onValueChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(value = value,
         placeholder = {Text(label,
             fontFamily = kalamFamily,
             color = Color.Black,
             fontSize = 24.sp)},
        keyboardOptions = KeyboardOptions(imeAction = if (label == "Email") ImeAction.Next
        else ImeAction.Done),
         keyboardActions = KeyboardActions(
             onDone = {if (label == "Email") {
                 focusManager.moveFocus(FocusDirection.Next)
             }else {
                 focusManager.clearFocus()
             }
             }
         ),
         onValueChange = {
             if (it.isNotBlank() || value.isNotBlank()) onValueChange(it)
         },
         singleLine = true,
         isError = exceptionMessage.isNotEmpty(),
         colors = colors ?: OutlinedTextFieldDefaults.colors(
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
         modifier = modifier
             .padding(top = dimensionResource(id = R.dimen.padding_auth_text_fields))
             .advancedShadow(
                 alpha = 0.5f,
                 cornersRadius = 10.dp,
                 shadowBlurRadius = 10.dp,
                 offsetY = 4.dp
             )
             .height(60.dp)
     )
}

@Composable
fun AuthButton(label: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Button(onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(5.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFA37EFB),
            disabledContainerColor = Color.White.copy(alpha = 0.4f),
            disabledContentColor = Color.White.copy(alpha = 1f)
        ),
        modifier = modifier
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
