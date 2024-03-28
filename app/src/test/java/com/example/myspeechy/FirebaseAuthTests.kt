package com.example.myspeechy

import android.content.Context
import android.content.Intent
import com.example.myspeechy.data.chat.User
import com.example.myspeechy.domain.auth.AuthService
import com.example.myspeechy.domain.auth.GoogleAuthService
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.StorageReference
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test

class FirebaseAuthTests {
    val emailToPass = "some3dfdfm"
    val userId = "userId"
    val username = "SomeName"
    val password = "SomeRandomPassword"

    //Can take any possible kind of result, such as AuthResult, DocumentSnapshot, QuerySnapshot
    inline fun <reified T> mockTask(result: T? = null, exception: Exception? = null): Task<T> {
        val task: Task<T> = mockk(relaxed = true)
        every { task.isComplete } returns true
        every { task.result } returns result
        every { task.exception } returns exception
        every { task.isCanceled } returns false
        return task
    }
    fun mockAuth(): FirebaseAuth {
        val mockedUser = mockk<FirebaseUser>(relaxed = true)
        return mockk<FirebaseAuth> { every { currentUser } returns mockedUser
            every { currentUser?.uid } returns userId
            every { currentUser?.displayName } returns username
            every { uid } returns userId
        }
    }
    fun mockFirestore(): FirebaseFirestore {
        val mockedFirestore = mockk<FirebaseFirestore>()
        for (collection in listOf("lessons", "meditation")) {
            every {
                mockedFirestore.collection("users")
                    .document(userId).collection(collection).document("items").set(mapOf<String, String>())
            } returns mockTask()
        }
        return mockedFirestore
    }
    fun mockRdb() = mockk<DatabaseReference> {
        every {child("users").child(userId).setValue(User(username, "")) } returns mockTask()
        every {child("users").child(userId).child("profilePicUpdated").get() } returns mockTask()
    }

    @Test
    fun basicAuthTest() {
        val mockedAuth = mockAuth()
        every { mockedAuth.createUserWithEmailAndPassword(emailToPass, password) } returns mockTask()
        every { mockedAuth.signOut() } returns Unit
        every { mockedAuth.signInWithEmailAndPassword(emailToPass, password) } returns mockTask()

        val mockRef = mockk<DatabaseReference>()
        val authService = spyk(AuthService(mockedAuth, mockRef), recordPrivateCalls = true)
        runBlocking {
            authService.createUser(emailToPass, password)
            authService.logInUser(emailToPass, password)
        }
        authService.logOut()
        coVerify { authService.createUser(emailToPass, password) }
        coVerify { authService.logInUser(emailToPass, password) }
        verify { authService.logOut() }
        confirmVerified(authService)

        verify { mockedAuth.createUserWithEmailAndPassword(emailToPass, password) }
        verify { mockedAuth.signInWithEmailAndPassword(emailToPass, password) }
        verify { mockedAuth.signOut() }
        confirmVerified(mockedAuth)
    }

    @Test
    fun rdbTest() {
        val mockedAuth = mockAuth()
        val mockedRef = mockRdb()
        val authService = spyk(AuthService(mockedAuth, rdbRef = mockedRef), recordPrivateCalls = true)
        runBlocking {
            authService.createRealtimeDbUser()
        }
        verify { mockedRef.child(userId).setValue(User(username, "")) }
        coVerify { authService.createRealtimeDbUser() }
    }

    @Test
    fun firestoreTest() {
        val mockedAuth = mockAuth()
        val mockedFirestore = mockFirestore()
        val authService = spyk(AuthService(mockedAuth, firestoreRef = mockedFirestore), recordPrivateCalls = true)
        runBlocking {
            authService.createFirestoreUser()
        }
        verify {
            listOf("lessons", "meditation").forEach { t ->
             mockedFirestore.collection("users")
                .document(userId).collection(t).document("items").set(mapOf<String, String>())
        } }
        coVerify { authService.createFirestoreUser() }
    }

    @Test
    fun deleteUserTest() {
        val mockedAuth = mockAuth()
        val mockedRdb = mockRdb()
        val mockedFirestore = mockFirestore()
        val mockedStorage = mockk<StorageReference>()
        /*val mockedDocumentRef = mockk<DocumentReference> {
            //coEvery { collection("5").get().await().documents } returns mapOf("id" to 5)
        }
        val mockedDocumentSnapshot = mockk<DocumentSnapshot> {
            every { data } returns *//*if (currDoc == "lesson") mapOf("5" to mapOf("id" to 5))
            else mapOf("2024-10-23" to mapOf("minutes" to 20))*//* mapOf("df" to "df")
        }
        val mockedQuerySnapshot = mockk<QuerySnapshot> {
            every { documents } returns listOf()
        }
        val userRef = mockedFirestore.collection("users").document(userId)
        every { userRef.delete() } returns mockTask()
        runBlocking {
            for (doc in listOf("meditation")) {
                val ref1 = userRef.collection(doc).document("items")
                every { ref1.get() } returns mockTask(mockedDocumentSnapshot)
                every { ref1.delete() } returns mockTask()
                val items = ref1.get().await().data
                if (!items.isNullOrEmpty()) {
                    for (doc1 in items.keys) {
                        every { ref1.collection(doc1).get() } returns mockTask(mockedQuerySnapshot)
                        val items1 = ref1.collection(doc1).get().await()
                        for (doc2 in items1.documents) {
                            ref1.collection(doc1).document(doc2.id).delete().await()
                        }
                    }
                }
                ref1.delete().await()
                println(ref1.get().await().data)
            }
            userRef.delete().await()
        }*/
        every { mockedAuth.currentUser?.delete() } returns mockTask()
        every { mockedRdb.child("users").child(userId).removeValue() } returns mockTask()
        listOf("normalQuality", "lowQuality").forEach { quality ->
            every { mockedStorage.child("profilePics")
                .child(userId).child(quality).child("$userId.jpg").delete() } returns mockTask()
        }
        val authService = spyk(AuthService(mockedAuth, mockedRdb, mockedFirestore, mockedStorage), recordPrivateCalls = true)
        runBlocking {
            authService.deleteUser()
            authService.removeProfilePics(userId)
            //authService.deleteFirestoreData(userId)
        }
        coVerify { authService.deleteUser() }
        coVerify { authService.removeProfilePics(userId) }
        //coVerify { authService.deleteFirestoreData(userId) }
        confirmVerified(authService)
    }

    @Test
    fun googleAuthTest() {
        val googleIdToken = "dfkfjkd"
        val mockedIntent = mockk<Intent>()
        val mockedContext = mockk<Context>()
        val mockedAuth = mockAuth()
        val mockedRdb = mockRdb()
        every { mockedRdb.child(userId).get() } returns mockTask()
        val mockedSignInClient = mockk<SignInClient> {
            every { getSignInCredentialFromIntent(mockedIntent).googleIdToken } returns googleIdToken
        }
        every { mockedAuth.signInWithCredential(any()) } returns mockTask()
        val authService = spyk(AuthService(mockedAuth, mockedRdb, mockFirestore(), mockk<StorageReference>()), recordPrivateCalls = true) {
            coEvery { checkIfUserExists() } returns false
        }
        val googleAuthService = GoogleAuthService(mockedContext, mockedSignInClient, authService)
        runBlocking {
            googleAuthService.signInWithIntent(mockedIntent)
        }
        coVerify { authService.createFirestoreUser() }
        coVerify { authService.createRealtimeDbUser() }
    }
}