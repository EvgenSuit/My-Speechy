package com.example.myspeechy.services

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.util.Log
import com.example.myspeechy.R
import com.example.myspeechy.data.chat.User
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.BeginSignInRequest.GoogleIdTokenRequestOptions
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await

 class AuthService(private val auth: FirebaseAuth, private val usersRef: DatabaseReference) {
     suspend fun createUser(email: String, password: String) {
         auth.createUserWithEmailAndPassword(email, password).await()
         createRealtimeDbUser()
         createFirestoreUser()
    }
    suspend fun logInUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }
     fun logOut() {
         auth.signOut()
     }
     private suspend fun createRealtimeDbUser() {
         val currUser = auth.currentUser
         usersRef.child(currUser!!.uid)
             .setValue(User(currUser.displayName ?: "", "")).await()
     }
     private suspend fun createFirestoreUser() {
         val ref = Firebase.firestore.collection("users").document(auth.currentUser!!.uid)
         listOf("lessons", "meditation").forEach { t ->
             ref.collection(t).document("items").set(mapOf<String, String>()).await()
         }
     }
     suspend fun deleteUser() {
         val userId = auth.currentUser!!.uid
         deleteFirestoreData(userId)
         removeProfilePics(userId)
         usersRef.child(userId).removeValue().await()
         auth.currentUser!!.delete().await()
     }
     private suspend fun removeProfilePics(userId: String) {
         val profilePicsExist = usersRef.child(userId).child("profilePicUpdated").get().await().exists()
         if (profilePicsExist) {
             val picsRef = Firebase.storage.reference.child("profilePics")
                 .child(userId)
             listOf("normalQuality", "lowQuality").forEach { quality ->
                 picsRef.child(quality).child("$userId.jpg").delete().await()
             }
         }
     }
     private suspend fun deleteFirestoreData(userId: String) {
         val userRef = Firebase.firestore.collection("users").document(userId)
             for (doc in listOf("lessons", "meditation")) {
                 val ref1 = userRef.collection(doc).document("items")
                 val items = ref1.get().await().data
                 if (!items.isNullOrEmpty()) {
                     for (doc1 in items.keys) {
                         val ref2 = ref1.collection(doc1)
                         val items1 = ref2.get().await()
                         for (doc2 in items1.documents) {
                             ref2.document(doc2.id).delete().await()
                         }
                     }
                 }
                 ref1.delete().await()
             }
         userRef.delete().await()
     }
}

class GoogleAuthService(
    private val context: Context,
    private val oneTapClient: SignInClient) {
    suspend fun signIn(onError: (String) -> Unit): IntentSender? {
        val res = try {
            oneTapClient.beginSignIn(
                buildSignInRequest()
            ).await()

        } catch(e: Exception) {
            onError(e.message.toString())
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