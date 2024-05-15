package com.myspeechy.myspeechy.screens.auth

import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.ImageLoader
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.decode.SvgDecoder
import com.myspeechy.myspeechy.R
import com.myspeechy.myspeechy.components.CustomSplashScreen
import com.myspeechy.myspeechy.components.advancedShadow
import com.myspeechy.myspeechy.domain.Result
import com.myspeechy.myspeechy.presentation.UiText
import com.myspeechy.myspeechy.presentation.auth.AuthViewModel
import com.myspeechy.myspeechy.ui.theme.itimFamily
import com.myspeechy.myspeechy.ui.theme.kalamFamily
import com.myspeechy.myspeechy.ui.theme.lalezarFamily
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(onNavigateToMain: () -> Unit) {
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
            CustomSplashScreen()
        }
        if (painter.state is AsyncImagePainter.State.Success) {
            MainBox(imageLoader = imageLoader,
                onNavigateToMain = onNavigateToMain)
            PrivacyPolicyText(Modifier.align(Alignment.BottomCenter))
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            focusManger.clearFocus(true)
        }
    }
}


@Composable
fun MainBox(imageLoader: ImageLoader,
            onNavigateToMain: () -> Unit,
            modifier: Modifier = Modifier,
            viewModel: AuthViewModel = hiltViewModel()) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val exceptionState by viewModel.exceptionState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val padding = dimensionResource(id = R.dimen.padding_auth_fields)
    val usernameLabel = stringResource(R.string.username)
    val emailLabel = stringResource(R.string.email_auth_field_click_label)
    val passwordLabel = stringResource(R.string.password_auth_field_click_label)
    val linearProgressBarDescription = stringResource(R.string.waiting_for_auth)
    val maxUsernameLength = integerResource(R.integer.max_username_or_title_length)
    val paddingAuthTextFields = dimensionResource(R.dimen.padding_auth_text_fields)
    val usernameError by remember(exceptionState.usernameErrorMessage) {
        mutableStateOf(exceptionState.usernameErrorMessage?.let { e ->
            when (e) {
                is UiText.StringResource -> e.asString(context)
                is UiText.StringResource.DynamicString -> e.s
            }
        })
    }
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
        .padding(30.dp)
        .border(
            width = 1.dp,
            color = Color.White,
            shape = RoundedCornerShape(30.dp)
        )
        .clip(RoundedCornerShape(30.dp))
        .background(Color.White.copy(0.4f))
        .defaultMinSize(minHeight = dimensionResource(id = R.dimen.auth_components_height))
        .width(IntrinsicSize.Min),
        contentAlignment = Alignment.Center)
    {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            Text(text = stringResource(id = R.string.app_name),
                fontFamily = itimFamily,
                fontSize = 48.sp,
                color = Color.Black,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 15.dp, bottom = 25.dp))
           Column(
               horizontalAlignment = Alignment.CenterHorizontally,
               verticalArrangement = Arrangement.spacedBy(paddingAuthTextFields),
               modifier = Modifier.padding(start = padding, end = padding, bottom = 34.dp)) {
               AnimatedVisibility(!uiState.logIn,
                   enter = slideInHorizontally { it },
                   exit = slideOutHorizontally { it }) {
                   Column(horizontalAlignment = Alignment.CenterHorizontally) {
                       AuthTextField(value = uiState.username,
                           exceptionMessage = usernameError ?: "",
                           label = usernameLabel,
                           modifier = Modifier.clickable(onClickLabel = usernameLabel,
                               onClick = {}),
                           onValueChange = {
                               if (it.length <= maxUsernameLength) viewModel.onUsernameChanged(it)
                           })
                       if (!usernameError.isNullOrEmpty()) {
                           ErrorMessage(usernameError!!)
                       }
                   }
               }
               Column(horizontalAlignment = Alignment.CenterHorizontally) {
                   AuthTextField(value = uiState.email,
                       exceptionMessage = emailError ?: "",
                       label = emailLabel,
                       modifier = Modifier.clickable(onClickLabel = emailLabel,
                           onClick = {}),
                       onValueChange = viewModel::onEmailChanged)
                   if (!emailError.isNullOrEmpty()) {
                       ErrorMessage(emailError!!)
                   }
               }
               Column(horizontalAlignment = Alignment.CenterHorizontally) {
                   AuthTextField(value = uiState.password,
                       exceptionMessage = passwordError ?: "",
                       label = passwordLabel,
                       modifier = Modifier.clickable(onClickLabel = passwordLabel,
                           onClick = {}),
                       onValueChange = viewModel::onPasswordChanged)
                   if (!passwordError.isNullOrEmpty()) {
                       ErrorMessage(passwordError!!)
                   }
               }
               AnimatedVisibility(uiState.result !is Result.InProgress && uiState.result !is Result.Success) {
                   AuthButtons(
                       logIn = uiState.logIn,
                       enabled =
                       (usernameError?.isEmpty() == true || uiState.logIn) &&
                       emailError?.isEmpty() == true &&
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
               AnimatedVisibility(uiState.result !is Result.InProgress && uiState.result !is Result.Success) {
                   val text = "Go to ${if (!uiState.logIn) stringResource(R.string.log_in) else stringResource(R.string.sign_up)}"
                   val label = stringResource(R.string.update_auth_option)
                   Text(text,
                       style = MaterialTheme.typography.labelSmall.copy(
                           color = Color.Black,
                           textDecoration = TextDecoration.Underline
                       ),
                       modifier = Modifier
                           .clickable(onClickLabel = label) { viewModel.updateAuthOption() })
               }
               AnimatedVisibility(uiState.result is Result.InProgress) {
                   LinearProgressIndicator(modifier = Modifier
                       .padding(top = dimensionResource(R.dimen.padding_auth_button_row))
                       .semantics { stateDescription = linearProgressBarDescription })
               }
                GoogleAuthButton(imageLoader = imageLoader,
                       enabled = uiState.result !is Result.InProgress && uiState.result !is Result.Success,
                       onGoogleSignIn = viewModel::googleSignIn,
                       onGoogleSignInWithIntent = viewModel::googleSignInWithIntent)
           }
        }
    }
}

@Composable
fun PrivacyPolicyText(modifier: Modifier) {
    val privacyPolicyLink = stringResource(R.string.privacy_policy_link)
    val context = LocalContext.current
    Text(
        stringResource(R.string.privacy_policy),
        style = MaterialTheme.typography.labelSmall.copy(
            color = Color.Black,
            textDecoration = TextDecoration.Underline
        ),
        modifier = modifier
            .padding(10.dp)
            .clickable {
                val urlIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(privacyPolicyLink)
                )
                context.startActivity(urlIntent)
            })
}

@Composable
fun AuthButtons(
    enabled: Boolean,
    logIn: Boolean,
    onLogIn: () -> Unit,
    onSignUp: () -> Unit) {
    val signUpLabel = stringResource(R.string.sign_up)
    val logInLabel = stringResource(R.string.log_in)
    AnimatedContent(logIn) { state ->
        AuthButton(label = if (state) logInLabel else signUpLabel, enabled,
            Modifier
                .clickable(onClickLabel = if (state) logInLabel else signUpLabel, onClick = {})
                .fillMaxWidth(),
            if (state) onLogIn else onSignUp)
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
            .width(dimensionResource(R.dimen.google_auth_button_width))
            .height(IntrinsicSize.Min)
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
        keyboardOptions = KeyboardOptions(imeAction = if (label != stringResource(R.string.password_auth_field_click_label)) ImeAction.Next
        else ImeAction.Done),
         keyboardActions = KeyboardActions(
             onDone = {if (label == "Email") {
                 focusManager.moveFocus(FocusDirection.Next)
             } else {
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
             .advancedShadow(
                 alpha = 0.5f,
                 cornersRadius = 10.dp,
                 shadowBlurRadius = 10.dp,
                 offsetY = 4.dp
             )
             .height(60.dp)
             .width(dimensionResource(R.dimen.auth_text_field_width))
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
            .height(IntrinsicSize.Min)
    ) {
        Text(label, fontFamily = lalezarFamily, fontSize = 20.sp)
    }
}

@Composable
fun ErrorMessage(value: String, color: Color = Color.Red) {
    Text(value,
        color = color,
        textAlign = TextAlign.Center,
        modifier = Modifier)
}
