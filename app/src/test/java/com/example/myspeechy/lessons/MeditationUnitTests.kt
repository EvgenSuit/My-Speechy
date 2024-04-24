package com.example.myspeechy.lessons

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.example.myspeechy.data.DataStoreManager
import com.example.myspeechy.data.authDataStore
import com.example.myspeechy.data.lesson.LessonDao
import com.example.myspeechy.data.lesson.LessonDb
import com.example.myspeechy.data.lesson.LessonRepository
import com.example.myspeechy.data.loadData
import com.example.myspeechy.data.navBarDataStore
import com.example.myspeechy.data.notificationsDataStore
import com.example.myspeechy.data.themeDataStore
import com.example.myspeechy.domain.MeditationNotificationServiceImpl
import com.example.myspeechy.domain.Result
import com.example.myspeechy.domain.lesson.MeditationLessonServiceImpl
import com.example.myspeechy.domain.meditation.MeditationStatsServiceImpl
import com.example.myspeechy.mockTask
import com.example.myspeechy.presentation.lesson.meditation.MeditationLessonItemViewModel
import com.example.myspeechy.presentation.lesson.meditation.MeditationStatsViewModel
import com.example.myspeechy.userId
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.WriteBatch
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.time.delay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
class MeditationUnitTests {
    private val date = "2024-04-23"
    private val stats = buildMap {
        for (i in 0 until 4) {
            put("2024-04-2$i", i*Random.nextInt(1, 30))
        }
    }
    private lateinit var viewModel: MeditationStatsViewModel
    private lateinit var mockedAuth: FirebaseAuth
    private lateinit var mockedFirestore: FirebaseFirestore
    private lateinit var mockedSnapshot: QuerySnapshot
    @Before
    fun init() {
        mockAuth()
    }

    private fun mockBatch(): WriteBatch {
        val batch = mockk<WriteBatch>()
        for (doc in mockedSnapshot.documents) {
            every { batch.delete(any()) } returns batch
        }
        every { batch.commit() } returns mockTask()
        return batch
    }
    private fun mockAuth() {
        mockedAuth = mockk<FirebaseAuth> {
            every { currentUser?.uid  } returns userId
        }
    }
    private fun mockFirestore(): CapturingSlot<EventListener<QuerySnapshot>> {
        mockedSnapshot = mockk<QuerySnapshot> {
            every { isEmpty } returns false
            every { documents.isEmpty() } returns false
            every { documents } returns stats.map { mockk<DocumentSnapshot> {
                every { id } returns it.key
                every { data!!.values } returns listOf(it.value.toLong()).toMutableList()
            } }
        }
        val listenerSlot = slot<EventListener<QuerySnapshot>>()
        mockedFirestore = mockk<FirebaseFirestore> {
            every { collection("users").document(userId).collection("meditation")
                .addSnapshotListener(capture(listenerSlot))} returns mockk<ListenerRegistration>()
            every { batch() } returns mockBatch()
        }
        return listenerSlot
    }
    private fun mockViewModel() {
        viewModel = MeditationStatsViewModel(MeditationStatsServiceImpl(mockedFirestore, mockedAuth))
    }

    @Test
    fun onInit_statsExtracted() {
        val listenerSlot = mockFirestore()
        mockViewModel()
        viewModel.setUpListener(false)
        listenerSlot.captured.onEvent(mockedSnapshot, null)
        assertEquals(viewModel.uiState.value.statsMap, stats)
    }

    @Test
    fun onInit_moreThanFourItemsExtracted() {
        val listenerSlot = mockFirestore()
        every { mockedSnapshot.documents } returns (stats + mapOf("2024-04-26" to 78)).map {
            mockk<DocumentSnapshot> {
                every { id } returns it.key
                every { data!!.values } returns listOf(it.value.toLong()).toMutableList()
                every { reference } returns mockk<DocumentReference> {
                    every { id } returns it.key
                    every { path } returns it.key
                }
            }}
        mockViewModel()
        viewModel.setUpListener(false)
        listenerSlot.captured.onEvent(mockedSnapshot, null)
        assertEquals(viewModel.uiState.value.statsMap.entries.first(), stats.entries.toList()[1])
    }

    @Test
    fun onStatsReceived_calculatedInSeconds() {
        val listenerSlot = mockFirestore()
        mockViewModel()
        viewModel.setUpListener(false)
        listenerSlot.captured.onEvent(mockedSnapshot, null)
        val map = viewModel.uiState.value.statsMap
        val maxValue = if (map.values.isNotEmpty()) map.values.max() else 0
        val labelList = mutableListOf<Float>()
        for (i in 0..maxValue step 5) {
            labelList.add(i.toFloat())
        }
        assertEquals(viewModel.uiState.value.labelList, labelList)
    }

    @Test
    fun onStatsReceived_calculatedInMinutes() {
        val listenerSlot = mockFirestore()
        mockViewModel()
        viewModel.setUpListener(false)
        every { mockedSnapshot.documents } returns listOf(mockk<DocumentSnapshot> {
            every { id } returns date
            every { data!!.values } returns listOf(67L).toMutableList()
        })
        listenerSlot.captured.onEvent(mockedSnapshot, null)
        val map = viewModel.uiState.value.statsMap
        val maxValue = if (map.values.isNotEmpty()) map.values.max() else 0
        val labelList = mutableListOf<Float>()
        var currValue = 0f
        val max = maxValue.toFloat() / 60f
        while (currValue <= max) {
            labelList.add(currValue)
            currValue += if (max <= 10f) 1f else 10f
        }
        assertEquals(viewModel.uiState.value.labelList, labelList)
    }
}