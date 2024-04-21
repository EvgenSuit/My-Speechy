package com.example.myspeechy.screens.auth

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import coil.decode.SvgDecoder
import com.example.myspeechy.R
import com.example.myspeechy.components.BackButton
import com.example.myspeechy.components.TryAgainOrLogoutButton
import com.example.myspeechy.domain.Result
import com.example.myspeechy.presentation.UiText
import com.example.myspeechy.presentation.auth.AccountDeletionViewModel
import com.example.myspeechy.presentation.auth.AuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.launch

@Composable
fun AccountDeletionScreen(
    onGoBack: () -> Unit,
    viewModel: AccountDeletionViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val imageLoader = ImageLoader.Builder(LocalContext.current)
        .components {
            add(SvgDecoder.Factory())
        }.build()
    val label = stringResource(R.string.log_in)
    val colors = TextFieldDefaults.colors(
        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
        focusedTextColor = MaterialTheme.colorScheme.onBackground
    )
    val emailError by remember(uiState.emailErrorMessage) {
        mutableStateOf(uiState.emailErrorMessage?.let { e ->
            when (e) {
                is UiText.StringResource -> e.asString(context)
                is UiText.StringResource.DynamicString -> e.s
            }
        })
    }
    val passwordError by remember(uiState.passwordErrorMessage) {
        mutableStateOf(uiState.passwordErrorMessage?.let { e ->
            when (e) {
                is UiText.StringResource -> e.asString(context)
                is UiText.StringResource.DynamicString -> e.s
            }
        })
    }
    LaunchedEffect(viewModel.authResultFlow) {
        viewModel.authResultFlow.collect { result ->
            if (result is Result.Success) {
                focusManager.clearFocus(true)
                Toasty.success(context, result.data, Toast.LENGTH_SHORT, true).show()
            } else if (result is Result.Error) {
                Toasty.error(context, result.error, Toast.LENGTH_LONG, true).show()
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(10.dp)) {
        if (Firebase.auth.currentUser != null && uiState.deletionResult.error.isNotEmpty()) {
            BackButton { onGoBack() }
        }
        Box(
            Modifier
                .weight(1f)
                .align(Alignment.CenterHorizontally)
                .defaultMinSize(dimensionResource(R.dimen.auth_components_height))
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.logout_delete_account_padding), Alignment.CenterVertically)) {
                when(val result = uiState.deletionResult) {
                    is Result.InProgress -> {
                        Text("Deleting account...", style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onBackground
                        ))
                        CircularProgressIndicator()
                    }
                    is Result.Success -> {
                        Toasty.success(context, result.data, Toast.LENGTH_SHORT, true).show()
                    }
                    else -> {
                        Text(result.error,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground))
                        when(uiState.authProvider) {
                            AuthProvider.EMAIL_AND_PASSWORD -> Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(13.dp)
                            ) {
                                AuthTextField(
                                    value = uiState.email,
                                    label = "Email",
                                    exceptionMessage = "",
                                    modifier = Modifier,
                                    colors = colors
                                ) { viewModel.onEmailChanged(it) }
                                if (!emailError.isNullOrEmpty()) {
                                    ErrorMessage(emailError!!)
                                }
                                AuthTextField(
                                    value = uiState.password,
                                    label = "Password",
                                    exceptionMessage = "",
                                    modifier = Modifier,
                                    colors = colors
                                ) { viewModel.onPasswordChanged(it) }
                                if (!passwordError.isNullOrEmpty()) {
                                    ErrorMessage(passwordError!!)
                                }
                                AuthButton(
                                    label = label,
                                    enabled = uiState.authResult !is Result.InProgress && emailError?.isEmpty() == true &&
                                            passwordError?.isEmpty() == true,
                                    Modifier.clickable(onClickLabel = label, onClick = {}),
                                    onClick = {coroutineScope.launch {
                                        viewModel.logIn()
                                    }}
                                )
                            }
                            AuthProvider.GOOGLE -> GoogleAuthButton(
                                imageLoader = imageLoader,
                                enabled = uiState.authResult !is Result.InProgress,
                                onGoogleSignIn = viewModel::googleSignIn,
                                onGoogleSignInWithIntent = viewModel::googleSignInWithIntent
                            )
                            else -> {}
                        }
                        TryAgainOrLogoutButton(true) {
                            coroutineScope.launch {
                                viewModel.deleteUser()
                            }
                        }
                    }
                }
            }
        }
    }
}
