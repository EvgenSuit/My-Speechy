package com.myspeechy.myspeechy.domain.auth

import android.content.Intent
import android.content.IntentSender
import android.content.pm.ApplicationInfo
import androidx.core.util.PatternsCompat
import com.myspeechy.myspeechy.data.chat.User
import com.myspeechy.myspeechy.domain.InputFormatCheckResult
import com.myspeechy.myspeechy.domain.error.EmailError
import com.myspeechy.myspeechy.domain.error.PasswordError
import com.myspeechy.myspeechy.domain.useCases.CheckIfIsAdminUseCase
import com.myspeechy.myspeechy.domain.useCases.DeletePublicChatUseCase
import com.myspeechy.myspeechy.domain.useCases.LeavePrivateChatUseCase
import com.myspeechy.myspeechy.domain.useCases.LeavePublicChatUseCase
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
                  private val leavePublicChatUseCase: LeavePublicChatUseCase? = null,
                  private val leavePrivateChatUseCase: LeavePrivateChatUseCase? = null,
                  private val checkIfIsAdminUseCase: CheckIfIsAdminUseCase? = null,
                  private val deletePublicChatUseCase: DeletePublicChatUseCase? = null) {
    val userId = auth.currentUser?.uid
    private lateinit var authStateListener: AuthStateListener
    fun listenForAuthState(onGetState: (Boolean) -> Unit) {
        authStateListener = AuthStateListener {
            onGetState(it.currentUser == null)
        }
        auth.addAuthStateListener(authStateListener)
    }

    fun validatePassword(password: String): InputFormatCheckResult<String, PasswordError> {
        if (password.isEmpty()) {
            return InputFormatCheckResult.Error(PasswordError.IS_EMPTY)
        }
        if (password.length < 8) {
            return InputFormatCheckResult.Error(PasswordError.IS_NOT_LONG_ENOUGH)
        }
        if (password.count(Char::isDigit) == 0) {
            return InputFormatCheckResult.Error(PasswordError.NOT_ENOUGH_DIGITS)
        }
        if (!password.any(Char::isLowerCase) || !password.any(Char::isUpperCase)) {
            return InputFormatCheckResult.Error(PasswordError.IS_NOT_MIXED_CASE)
        }
        return InputFormatCheckResult.Success("")
    }

    fun validateEmail(email: String): InputFormatCheckResult<String, EmailError> {
        if (email.isEmpty()) {
            return InputFormatCheckResult.Error(EmailError.IS_EMPTY)
        }
        if (!PatternsCompat.EMAIL_ADDRESS.matcher(email).matches()) {
            return InputFormatCheckResult.Error(EmailError.IS_NOT_VALID)
        }
        return InputFormatCheckResult.Success("")
    }

    suspend fun signInWithCredential(authCredential: AuthCredential) {
        auth.signInWithCredential(authCredential).await()
    }

    suspend fun logInUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun checkIfUserExists(): Boolean? =
        auth.currentUser?.uid?.let { rdbRef?.child("users")?.child(it)?.get()?.await()?.exists() }
    fun logOut() = auth.signOut()
    suspend fun createUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password).await()
    }

    suspend fun createRealtimeDbUser() {
        val currUser = auth.currentUser
        rdbRef?.child("users")?.child(currUser!!.uid)?.setValue(User(currUser.displayName ?: "", ""))
            ?.await()
    }

    suspend fun createFirestoreUser() {
        if (firestoreRef != null) {
            val ref = firestoreRef.collection("users").document(auth.currentUser!!.uid)
            for (coll in listOf("lessons", "meditation"))
                ref.collection(coll).document("dummy").set(mapOf("id" to -1)).await()
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
        val userId = auth.currentUser?.uid
        if (rdbRef != null && userId != null) {
            val ref = rdbRef.child("users").child(userId)
            val privateChats = ref.child("private_chats").get().await()
            val publicChats = ref.child("public_chats").get().await()
            for (privateChat in privateChats.children) {
                val id = privateChat.key
                if (id != null) {
                    leavePrivateChatUseCase?.let { it(id) }
                }
            }
            for (publicChat in publicChats.children) {
                val id = publicChat.key
                if (id != null) {
                    val isAdmin = checkIfIsAdminUseCase?.let { it(id) }
                    if (isAdmin == true) deletePublicChatUseCase?.let { it(id) }
                    leavePublicChatUseCase?.let { it(id, isAdmin == false) }
                }
            }
        }
    }

    suspend fun removeProfilePics(userId: String) {
        if (rdbRef == null || storageRef == null) return
        val profilePicsExist =
            rdbRef.child("users").child(userId).child("profilePicUpdated").get().await()?.exists()
        if (profilePicsExist == true) {
            val picsRef = storageRef.child("profilePics").child(userId)
            for (quality in listOf("normalQuality", "lowQuality"))
                picsRef.child(quality).child("$userId.jpg").delete().await()
        }
    }

    suspend fun deleteFirestoreData(userId: String) {
        //TODO implement batched deletion in the future
        if (firestoreRef == null) return
        val userRef = firestoreRef.collection("users").document(userId)
        for (coll in listOf("lessons", "meditation", "thoughtTracks")) {
            val itemsRef = userRef.collection(coll)
            val docs = itemsRef.get().await().documents
            if (docs.isNotEmpty()) {
                for (doc in docs) {
                    itemsRef.document(doc.id).delete().await()
                }
            }
        }
    }
}

class GoogleAuthService(
    private val appInfo: ApplicationInfo,
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
            authService.createFirestoreUser()
            authService.createRealtimeDbUser()
        }
    }
     private fun getApiKey(): String {
        val isInDebug = appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val apiKey = appInfo.metaData
            .getString(if (isInDebug) "DEBUG_GOOGLE_OAUTH_API_KEY" else "RELEASE_GOOGLE_OAUTH_API_KEY")
            ?: throw Exception("Couldn't sign in")
        return apiKey
    }

    private fun buildSignInRequest(): BeginSignInRequest {
        val apiKey = getApiKey()
        return BeginSignInRequest.Builder()
            .setGoogleIdTokenRequestOptions(
                GoogleIdTokenRequestOptions.Builder()
                    .setSupported(true)
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(apiKey)
                    .build())
            .setAutoSelectEnabled(true)
            .build()
    }
}