package com.example.myspeechy

import android.content.Intent
import android.content.pm.ApplicationInfo
import com.example.myspeechy.data.chat.User
import com.example.myspeechy.domain.auth.AccountDeletionService
import com.example.myspeechy.domain.auth.AuthService
import com.example.myspeechy.domain.auth.GoogleAuthService
import com.example.myspeechy.domain.useCases.CheckIfIsAdminUseCase
import com.example.myspeechy.domain.useCases.DecrementMemberCountUseCase
import com.example.myspeechy.domain.useCases.DeletePublicChatUseCase
import com.example.myspeechy.domain.useCases.LeavePrivateChatUseCase
import com.example.myspeechy.domain.useCases.LeavePublicChatUseCase
import com.example.myspeechy.presentation.auth.AccountDeletionViewModel
import com.example.myspeechy.presentation.auth.AuthViewModel
import com.example.myspeechy.presentation.chat.getOtherUserId
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.StorageReference
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FirebaseAuthTests {
    val emailToPass = "someemail@gmail.com"
    val password = "SecurePassword454"
    val googleIdToken = "dfkfjkd"
    val firestoreCollections = listOf("lessons", "meditation")
    val lessons = (1..2).map { mapOf("$it" to mapOf("id" to it)) } + mapOf("dummy" to mapOf("id" to -1))
    val meditations = (1..3).map { mapOf("2024-02-0$it" to mapOf("minutes" to it)) }

    private var mockedAuth = mockk<FirebaseAuth>()
    private var mockedFirestore = mockk<FirebaseFirestore>()
    private var mockedStorage = mockk<StorageReference>()
    private var mockedRdb = mockk<DatabaseReference>()

    @Before
    fun mockDependencies() {
        mockAuth()
        mockRdb()
        mockFirestore()
        mockStorage(mockedStorage)
    }
    fun mockAuth() {
        mockkStatic(GoogleAuthProvider::class)
        mockedAuth = mockk<FirebaseAuth> {
            every { currentUser } returns mockk<FirebaseUser>()
            every { currentUser?.uid } returns userId
            every { createUserWithEmailAndPassword(any(), any()) } returns mockTask()
            every { signInWithEmailAndPassword(any(), any()) } returns mockTask()
            every { signInWithCredential(GoogleAuthProvider.getCredential(googleIdToken, null)) } returns mockTask()
            every { currentUser?.delete() } returns mockTask()
            every { currentUser?.delete() } returns mockTask()
            every { currentUser?.displayName } returns username

        }
    }
    fun mockFirestore() {
        val mockedLessons = mockk<QuerySnapshot> {
            every { documents } returns lessons.map { mockk<DocumentSnapshot>{
                every { id } returns it.keys.first().toString()
            } }
        }
        val mockedMeditations = mockk<QuerySnapshot> {
            every { documents } returns meditations.map { mockk<DocumentSnapshot>{
                every { id } returns it.keys.first().toString()
            } }
        }
        for (coll in firestoreCollections) {
            val currData = if (coll == firestoreCollections[0]) mockedLessons else mockedMeditations
            every { mockedFirestore.collection("users").document(userId).collection(coll).document("dummy").set(mapOf("id" to -1))} returns mockTask()
            every { mockedFirestore.collection("users").document(userId).collection(coll).get() } returns mockTask(currData)
            for (doc in currData.documents) {
                //create a separate variable to hold a value of a doc id
                //since if document(doc.id) is used, at the time of execution this value will be empty because of variable scope
                val id = doc.id
                every { mockedFirestore.collection("users").document(userId).collection(coll).document(id).delete() } returns mockTask()
            }
        }
    }
    fun mockRdb() {
        every { mockedRdb.child("users").child(userId).setValue(User(username, "")) } returns mockTask()
        every { mockedRdb.child("users").child(userId).removeValue() } returns mockTask()
        every { mockedRdb.child(userId).get() } returns mockTask()
        val mockedProfilePicUpdatedSnapshot = mockk<DataSnapshot> {
            every { exists() } returns true
        }
        every { mockedRdb.child("users").child(userId).child("profilePicUpdated").get() } returns mockTask(mockedProfilePicUpdatedSnapshot)
    }

    @Test
    fun authenticate_correctInputFormat_authenticationPerformed() {
        val authService = AuthService(mockedAuth, mockedRdb, mockedFirestore, mockedStorage)
        val authViewModel = AuthViewModel(authService)

        authViewModel.onEmailChanged(emailToPass)
        authViewModel.onPasswordChanged(password)
        runBlocking {
            authViewModel.signUp()
        }
        assertTrue(authViewModel.uiState.value.result.data.isNotEmpty())
        assertTrue(authViewModel.uiState.value.result.error.isEmpty())
        verify { mockedAuth.createUserWithEmailAndPassword(any(), any()) }
        verify { mockedRdb.child("users").child(userId).setValue(User(username, "")) }
        val firestoreRef = mockedFirestore.collection("users").document(userId)
        for (coll in listOf("lessons", "meditation")) {
            verify { firestoreRef.collection(coll).document("dummy").set(mapOf("id" to -1)) }
      }
    }

    @Test
    fun authenticate_wrongInputFormat_authenticationNotPerformed() {
        val authService = AuthService(mockedAuth, mockedRdb , mockedFirestore, mockedStorage)
        val firestoreRef = mockedFirestore.collection("users").document(userId)
        val authViewModel = AuthViewModel(authService)

        //test for different cases of input
        val incorrectEmailCases = listOf("", "some", "34546", "some@")
        val incorrectPasswordCases = listOf("", "dkjfd", "2", "FFFGf3")
        //assume the first try is successful
        authViewModel.onEmailChanged(emailToPass)
        authViewModel.onPasswordChanged(password)
        for (case in incorrectEmailCases) {
            authViewModel.onEmailChanged(case)
            runBlocking {
                authViewModel.signUp()
                authViewModel.logIn()
            }
            //if the input is not of correct format, verify that the authentication wasn't run
            //any() means we don't care about the arguments in this case since uiState's email value was already set
            verify(exactly = 0) { mockedAuth.createUserWithEmailAndPassword(any(), any()) }
            verify(exactly = 0) { mockedAuth.signInWithEmailAndPassword(any(), any())}
            verify(exactly = 0) { mockedRdb.child("users").child(userId).setValue(User(username, "")) }
            for (t in listOf("lessons", "meditation")) {
                verify(exactly = 0) { firestoreRef.collection(t).document("items").set(mapOf<String, String>()) }
            }
            //set to correct email format to correctly test the next case
            authViewModel.onEmailChanged(emailToPass)
        }
        for (case in incorrectPasswordCases) {
            authViewModel.onPasswordChanged(case)
            runBlocking {
                authViewModel.signUp()
                authViewModel.logIn()
            }
            verify(exactly = 0) { mockedAuth.createUserWithEmailAndPassword(any(), any()) }
            verify(exactly = 0) { mockedAuth.signInWithEmailAndPassword(any(), any()) }
            verify(exactly = 0) { mockedRdb.child("users").child(userId).setValue(User(username, "")) }
            for (t in listOf("lessons", "meditation")) {
                verify(exactly = 0) { firestoreRef.collection(t).document("items").set(mapOf<String, String>()) }
            }
            authViewModel.onPasswordChanged(password)
        }
    }
    @Test
    fun signOut_success_signedOut() {
        every { mockedAuth.signOut() } answers {
            every { mockedAuth.currentUser } returns null
        }
        val authService = AuthService(mockedAuth, mockedRdb, mockedFirestore, mockedStorage)
        authService.logOut()
        assertTrue(mockedAuth.currentUser == null)
    }



    @Test
    fun googleAuthenticate_userExists_userNotCreated() {
        val mockedIntent = mockk<Intent>()
        val mockedAppInfo = mockk<ApplicationInfo>()
        val mockedSignInClient = mockk<SignInClient> {
            every { getSignInCredentialFromIntent(mockedIntent).googleIdToken } returns googleIdToken
        }
        val mockedUserExists = mockk<DataSnapshot>{
            every { exists() } returns true
        }
        every { mockedRdb.child("users").child(userId).get() } returns mockTask(mockedUserExists)
        val authService = spyk(AuthService(mockedAuth, mockedRdb, mockedFirestore, mockedStorage)) {
            coEvery { createFirestoreUser() } returns Unit
        }
        val googleAuthService = GoogleAuthService(mockedAppInfo, mockedSignInClient, authService)
        val authViewModel = AuthViewModel(authService, googleAuthService)
        runBlocking {
            authViewModel.googleSignIn()
            authViewModel.googleSignInWithIntent(mockedIntent)
        }
        assertTrue(authViewModel.uiState.value.result.error.isEmpty())
        coVerify(exactly = 0) { authService.createFirestoreUser() }
    }
    @Test
    fun googleAuthenticate_userDoesNotExist_userCreated() {
        val mockedIntent = mockk<Intent>()
        val mockedAppInfo = mockk<ApplicationInfo>()
        val mockedSignInClient = mockk<SignInClient> {
            every { getSignInCredentialFromIntent(mockedIntent).googleIdToken } returns googleIdToken
        }
        val mockedUserExists = mockk<DataSnapshot>{
            every { exists() } returns false
        }
        every { mockedRdb.child("users").child(userId).get() } returns mockTask(mockedUserExists)
        val authService = spyk(AuthService(mockedAuth, mockedRdb, mockedFirestore, mockedStorage))
        coEvery { authService.createFirestoreUser() } returns Unit
        val googleAuthService = GoogleAuthService(mockedAppInfo, mockedSignInClient, authService)
        val authViewModel = AuthViewModel(authService, googleAuthService)
        runBlocking {
            authViewModel.googleSignIn()
            authViewModel.googleSignInWithIntent(mockedIntent)
        }
        assertTrue(authViewModel.uiState.value.result.error.isEmpty())
        coVerify { authService.createFirestoreUser() }
    }
}