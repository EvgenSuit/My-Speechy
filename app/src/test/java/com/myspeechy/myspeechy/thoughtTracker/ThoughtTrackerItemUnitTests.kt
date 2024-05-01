package com.myspeechy.myspeechy.thoughtTracker

import androidx.lifecycle.SavedStateHandle
import com.myspeechy.myspeechy.data.thoughtTrack.ThoughtTrack
import com.myspeechy.myspeechy.domain.Result
import com.myspeechy.myspeechy.domain.thoughtTracker.ThoughtTrackerItemService
import com.myspeechy.myspeechy.domain.useCases.GetCurrentDateUseCase
import com.myspeechy.myspeechy.mockTask
import com.myspeechy.myspeechy.presentation.thoughtTracker.ThoughtTrackerItemViewModel
import com.myspeechy.myspeechy.userId
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThoughtTrackerItemUnitTests {
    private lateinit var viewModel: ThoughtTrackerItemViewModel
    private var mockedFirestore: FirebaseFirestore? = null
    private lateinit var mockedAuth: FirebaseAuth

    @Before
    fun init() {
        mockAuth()
    }
    private fun mockAuth() {
        mockedAuth = mockk<FirebaseAuth> {
            every { uid } returns userId }
    }

    @Test
    fun listenForData_success_trackReceived() {
        val currDate = GetCurrentDateUseCase().invoke()
        val thoughtTrack = ThoughtTrack(thoughts = "Some thoughts")
        val mockedDocument = mockk<DocumentSnapshot> {
            every { toObject(ThoughtTrack::class.java) } returns thoughtTrack
        }
        val mockedSavedStateHandle = mockk<SavedStateHandle> {
            every { get<String>("date") } returns currDate
        }
        val listenerSlot = slot<EventListener<DocumentSnapshot>>()
        mockedFirestore = mockk<FirebaseFirestore> {
            every { collection("users").document(userId).collection("thoughtTracks")
                .document(currDate).addSnapshotListener(capture(listenerSlot))} returns mockk<ListenerRegistration>()
        }
        viewModel = ThoughtTrackerItemViewModel(
            ThoughtTrackerItemService(mockedFirestore!!.collection("users"), mockedAuth),
            mockedSavedStateHandle)
        listenerSlot.captured.onEvent(mockedDocument, null)
        assertTrue(viewModel.uiState.value.trackFetchResult is Result.Success)
        assertEquals(viewModel.uiState.value.track, thoughtTrack)
        assertEquals(viewModel.uiState.value.currentDate, "Today")
    }

    @Test
    fun listenForData_failure_trackNotReceived() {
        val currDate = GetCurrentDateUseCase().invoke()
        val mockedException = mockk<FirebaseFirestoreException> {
            every { code } returns FirebaseFirestoreException.Code.UNKNOWN
        }
        val mockedDocument = mockk<DocumentSnapshot> {
            every { toObject(ThoughtTrack::class.java) } returns ThoughtTrack(thoughts = "")
        }
        val mockedSavedStateHandle = mockk<SavedStateHandle> {
            every { get<String>("date") } returns currDate
        }
        val listenerSlot = slot<EventListener<DocumentSnapshot>>()
        mockedFirestore = mockk<FirebaseFirestore> {
            every { collection("users").document(userId).collection("thoughtTracks")
                .document(currDate).addSnapshotListener(capture(listenerSlot))} returns mockk<ListenerRegistration>()
        }
        viewModel = ThoughtTrackerItemViewModel(
            ThoughtTrackerItemService(mockedFirestore!!.collection("users"), mockedAuth),
            mockedSavedStateHandle)
        listenerSlot.captured.onEvent(mockedDocument, mockedException)
        assertEquals(viewModel.uiState.value.trackFetchResult.error, "Couldn't fetch track: ${FirebaseFirestoreException.Code.UNKNOWN}")
        assertEquals(viewModel.uiState.value.track, ThoughtTrack())
        assertEquals(viewModel.uiState.value.currentDate, "")
    }

    @Test
    fun saveTrack_success_trackSaved() {
        val currDate = GetCurrentDateUseCase().invoke()
        val trackToSave = ThoughtTrack(thoughts = "Some thoughts")
        val mockedSavedStateHandle = mockk<SavedStateHandle> {
            every { get<String>("date") } returns currDate
        }
        val mockedDocument = mockk<DocumentSnapshot> {
            every { toObject(ThoughtTrack::class.java) } returns ThoughtTrack(thoughts = "")
        }
        val newDocument = mockk<DocumentSnapshot> {
            every { toObject(ThoughtTrack::class.java) } returns trackToSave
        }
        val listenerSlot = slot<EventListener<DocumentSnapshot>>()
        mockedFirestore = mockk<FirebaseFirestore> {
            every { collection("users").document(userId).collection("thoughtTracks")
                .document(currDate).addSnapshotListener(capture(listenerSlot))} returns mockk<ListenerRegistration>()
            every { collection("users").document(userId).collection("thoughtTracks")
                .document(currDate).set(trackToSave)} returns mockTask()
        }
        viewModel = ThoughtTrackerItemViewModel(
            ThoughtTrackerItemService(mockedFirestore!!.collection("users"), mockedAuth),
            mockedSavedStateHandle)
        listenerSlot.captured.onEvent(mockedDocument, null)
        assertEquals(viewModel.uiState.value.track, ThoughtTrack(thoughts = ""))
        runBlocking {
            viewModel.saveData(trackToSave.questions, trackToSave.thoughts!!)
        }
        if (viewModel.uiState.value.saveResult is Result.Success) {
            listenerSlot.captured.onEvent(newDocument, null)
        }
        assertEquals(viewModel.uiState.value.track, trackToSave)
    }
}