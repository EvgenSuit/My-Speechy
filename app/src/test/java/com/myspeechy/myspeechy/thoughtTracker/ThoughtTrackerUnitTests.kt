package com.myspeechy.myspeechy.thoughtTracker

import com.myspeechy.myspeechy.domain.thoughtTracker.ThoughtTrackerService
import com.myspeechy.myspeechy.domain.useCases.GetCurrentDateUseCase
import com.myspeechy.myspeechy.domain.useCases.IsDateEqualToCurrentUseCase
import com.myspeechy.myspeechy.presentation.thoughtTracker.ThoughtTrackerViewModel
import com.myspeechy.myspeechy.userId
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner


@RunWith(RobolectricTestRunner::class)
class ThoughtTrackerUnitTests {
    private lateinit var viewModel: ThoughtTrackerViewModel
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
    fun listenForData_success_tracksReceived() {
        val currDate = GetCurrentDateUseCase().invoke()
        val mockedDocument = mockk<DocumentSnapshot> {
            every { id } returns currDate
        }
        val mockedSnapshot = mockk<QuerySnapshot> {
            every { documents } returns listOf(mockedDocument)
        }
        val listenerSlot = slot<EventListener<QuerySnapshot>>()
        mockedFirestore = mockk<FirebaseFirestore> {
            every { collection("users").document(userId).collection("thoughtTracks")
                .addSnapshotListener(capture(listenerSlot)) } returns mockk<ListenerRegistration>()
        }
        viewModel = ThoughtTrackerViewModel(ThoughtTrackerService(mockedFirestore!!.collection("users"), mockedAuth),
            IsDateEqualToCurrentUseCase(),
            GetCurrentDateUseCase())
        viewModel.listenForTracks(false)
        listenerSlot.captured.onEvent(mockedSnapshot, null)
        assertTrue(viewModel.uiState.value.trackItems.map { it.date }.any {it == currDate})
    }

    @Test
    fun listenForData_failure_tracksNotReceived() {
        val mockedException = mockk<FirebaseFirestoreException> {
            every { code } returns FirebaseFirestoreException.Code.UNKNOWN
        }
        val listenerSlot = slot<EventListener<QuerySnapshot>>()
        mockedFirestore = mockk<FirebaseFirestore> {
            every { collection("users").document(userId).collection("thoughtTracks")
                .addSnapshotListener(capture(listenerSlot)) } returns mockk<ListenerRegistration>()
        }
        viewModel = ThoughtTrackerViewModel(ThoughtTrackerService(mockedFirestore!!.collection("users"), mockedAuth),
            IsDateEqualToCurrentUseCase(),
            GetCurrentDateUseCase())
        viewModel.listenForTracks(false)
        listenerSlot.captured.onEvent(null, mockedException)
        assertTrue(viewModel.uiState.value.trackItems.isEmpty())
        assertTrue(viewModel.uiState.value.result.error == "Couldn't fetch tracks: ${FirebaseFirestoreException.Code.UNKNOWN.name}")
    }


}