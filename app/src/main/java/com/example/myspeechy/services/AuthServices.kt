package com.example.myspeechy.services

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.example.myspeechy.R
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.BeginSignInRequest.GoogleIdTokenRequestOptions
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

 class AuthService(private val auth: FirebaseAuth) {
     suspend fun createUser(email: String, password: String, onTask: (Throwable?) -> Unit) {
                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener{onTask(it.exception)}.await()
    }
    suspend fun logInUser(email: String, password: String, onTask: (Throwable?) -> Unit) {
            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener{onTask(it.exception)}.await()
    }
}

class GoogleAuthService(
    private val context: Context,
    private val oneTapClient: SignInClient) {

    suspend fun signIn(): IntentSender? {
        val res = try {
            oneTapClient.beginSignIn(
                buildSignInRequest()
            ).await()

        } catch(e: Exception) {
            e.printStackTrace()
            null
        }
        return res?.pendingIntent?.intentSender
    }

    suspend fun signInWithIntent(intent: Intent) {
        val credential = oneTapClient.getSignInCredentialFromIntent(intent)
        val googleIdToken = credential.googleIdToken
        val googleCredentials = GoogleAuthProvider.getCredential(googleIdToken, null)
        try {
            Firebase.auth.signInWithCredential(googleCredentials).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildSignInRequest(): BeginSignInRequest {
        return BeginSignInRequest.Builder()
            .setGoogleIdTokenRequestOptions(
                GoogleIdTokenRequestOptions.Builder()
                    .setSupported(true)
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(context.getString(R.string.web_client_id))
                    .build()
            )
            .setAutoSelectEnabled(true)
            .build()
    }
}