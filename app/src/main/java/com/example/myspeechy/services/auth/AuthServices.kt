package com.example.myspeechy.services.auth

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.ApplicationInfo
import android.util.Patterns
import com.example.myspeechy.data.chat.User
import com.example.myspeechy.services.Result
import com.example.myspeechy.services.error.EmailError
import com.example.myspeechy.services.error.PasswordError
import com.example.myspeechy.useCases.CheckIfIsAdminUseCase
import com.example.myspeechy.useCases.DeletePublicChatUseCase
import com.example.myspeechy.useCases.LeavePrivateChatUseCase
import com.example.myspeechy.useCases.LeavePublicChatUseCase
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.BeginSignInRequest.GoogleIdTokenRequestOptions
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DatabaseReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await

class AuthService(private val auth: FirebaseAuth,
                   private val rdbRef: DatabaseReference? = null,
                   private val firestoreRef: FirebaseFirestore? = null,
                   private val storageRef: StorageReference? = null,
    private val leavePublicChatUseCase: LeavePublicChatUseCase,
    private val leavePrivateChatUseCase: LeavePrivateChatUseCase,
    private val checkIfIsAdminUseCase: CheckIfIsAdminUseCase,
    private val deletePublicChatUseCase: DeletePublicChatUseCase) {
    private lateinit var authStateListener: AuthStateListener
    fun listenForAuthState( onLoggedOut: () -> Unit) {
        authStateListener = AuthStateListener { if (it.currentUser == null) {
            it.removeAuthStateListener(authStateListener)
            onLoggedOut()
        } }
        auth.addAuthStateListener(authStateListener)
    }
    fun validatePassword(password: String): Result<String, PasswordError> {
         if (password.isEmpty()) {
                return Result.Error(PasswordError.IS_EMPTY)
         }
         if (password.length < 8) {
             return Result.Error(PasswordError.IS_NOT_LONG_ENOUGH)
         }
         if (password.count(Char::isDigit) == 0) {
             return Result.Error(PasswordError.NOT_ENOUGH_DIGITS)
         }
         if (!password.any(Char::isLowerCase) || !password.any(Char::isUpperCase)) {
             return Result.Error(PasswordError.IS_NOT_MIXED_CASE)
         }
         return Result.Success("")
     }
    fun validateEmail(email: String): Result<String, EmailError> {
        if (email.isEmpty()) {
            return Result.Error(EmailError.IS_EMPTY)
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return Result.Error(EmailError.IS_NOT_VALID)
        }
        return Result.Success("")
    }
    suspend fun signInWithCredential(authCredential: AuthCredential) {
        auth.signInWithCredential(authCredential).await()
    }
     suspend fun createUser(email: String, password: String) {
         auth.createUserWithEmailAndPassword(email, password).await()
    }
    suspend fun logInUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }
     suspend fun checkIfUserExists(): Boolean? = rdbRef?.child("users")?.child(auth.uid!!)?.get()?.await()?.exists()
     fun logOut() = auth.signOut()
     suspend fun createRealtimeDbUser() {
         val currUser = auth.currentUser
         rdbRef?.child("users")?.child(currUser!!.uid)
             ?.setValue(User(currUser.displayName ?: "", ""))?.await()
     }
     suspend fun createFirestoreUser() {
         if (firestoreRef != null) {
             val ref = firestoreRef.collection("users").document(auth.currentUser!!.uid)
             listOf("lessons", "meditation").forEach { t ->
                 ref.collection(t).document("items").set(mapOf<String, String>()).await()
             }
         }
     }
    suspend fun deleteRdbUser() {
        val userId = auth.currentUser!!.uid
        rdbRef?.child("users")?.child(userId)?.removeValue()?.await()
    }
     suspend fun deleteUser() {
         auth.currentUser!!.delete().await()
     }
    suspend fun revokeMembership() {
        val userId = auth.currentUser!!.uid
        if (rdbRef != null) {
            val ref = rdbRef.child("users").child(userId)
            val privateChats = ref.child("private_chats").get().await()
            val publicChats = ref.child("public_chats").get().await()
            for (privateChat in privateChats.children) {
                val id = privateChat.key
                if (id != null) {
                    leavePrivateChatUseCase(id)
                }
            }
            for (publicChat in publicChats.children) {
                val id = publicChat.key
                if (id != null) {
                    if (checkIfIsAdminUseCase(id)) deletePublicChatUseCase(id)
                    leavePublicChatUseCase(id, false)
                }
            }
        }
    }
     suspend fun removeProfilePics(userId: String) {
         if (rdbRef == null || storageRef == null) return
         val profilePicsExist = rdbRef.child("users").child(userId).child("profilePicUpdated").get().await()?.exists()
         if (profilePicsExist == true) {
             val picsRef = storageRef.child("profilePics")
                 .child(userId)
             listOf("normalQuality", "lowQuality").forEach { quality ->
                 picsRef.child(quality).child("$userId.jpg").delete().await()
             }
             }
     }
     suspend fun deleteFirestoreData(userId: String) {
         if (firestoreRef == null) return
         val userRef = firestoreRef.collection("users").document(userId)
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
    private val oneTapClient: SignInClient,
    private val authService: AuthService
) {
    suspend fun signIn(): IntentSender? {
        val res = oneTapClient.beginSignIn(buildSignInRequest()).await()
        return res?.pendingIntent?.intentSender
    }
    suspend fun signInWithIntent(intent: Intent) {
        val googleIdToken = oneTapClient.getSignInCredentialFromIntent(intent).googleIdToken
        val googleCredentials = GoogleAuthProvider.getCredential(googleIdToken, null)
        authService.signInWithCredential(googleCredentials)
        val userExists = authService.checkIfUserExists()
        if (userExists != null && !userExists) {
            authService.createRealtimeDbUser()
            authService.createFirestoreUser()
        }
    }

    private fun buildSignInRequest(): BeginSignInRequest {
        val isInDebug = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val apiKey = context.applicationInfo.metaData
            .getString(if (isInDebug) "DEBUG_GOOGLE_OAUTH_API_KEY" else "RELEASE_GOOGLE_OAUTH_API_KEY")
            ?: throw Exception("Couldn't sign in")
        return BeginSignInRequest.Builder()
            .setGoogleIdTokenRequestOptions(
                GoogleIdTokenRequestOptions.Builder()
                    .setSupported(true)
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(apiKey)
                    .build()
            )
            .setAutoSelectEnabled(true)
            .build()
    }
}