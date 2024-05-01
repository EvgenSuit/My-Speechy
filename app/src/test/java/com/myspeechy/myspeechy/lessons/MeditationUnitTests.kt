package com.myspeechy.myspeechy.lessons

import com.myspeechy.myspeechy.domain.Result
import com.myspeechy.myspeechy.domain.meditation.MeditationStatsServiceImpl
import com.myspeechy.myspeechy.mockTask
import com.myspeechy.myspeechy.presentation.lesson.meditation.MeditationStatsViewModel
import com.myspeechy.myspeechy.userId
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.WriteBatch
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
class MeditationUnitTests {
    private val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    private val stats = buildMap {
        for (i in 1 until 5) {
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
            every { collection("users").document(userId).collection("meditation")
                .document("dummy").delete() } returns mockTask()
        }
        return listenerSlot
    }
    private fun mockViewModel() {
        val meditationStatsServiceImpl = MeditationStatsServiceImpl(mockedFirestore, mockedAuth)
        viewModel = MeditationStatsViewModel(meditationStatsServiceImpl)
    }

    @Test
    fun onInit_statsExtracted() {
        val listenerSlot = mockFirestore()
        mockViewModel()
        viewModel.setUpListener(false)
        listenerSlot.captured.onEvent(mockedSnapshot, null)
        assertEquals(viewModel.uiState.value.statsMap, stats)
        assertTrue(viewModel.uiState.value.loadResult is Result.Success)
    }

    @Test
    fun onInit_dummyDeleted() {
        val listenerSlot = mockFirestore()
        val firstStatsMap = mapOf(stats.keys.first() to stats.values.first())
        mockedSnapshot = mockk<QuerySnapshot> {
            every { isEmpty } returns false
            every { documents.isEmpty() } returns false
            every { documents } returns (mapOf("dummy" to -1) + firstStatsMap).map { mockk<DocumentSnapshot> {
                every { id } returns it.key
                every { data!!.values } returns listOf(it.value.toLong()).toMutableList()
                every { reference } returns mockk<DocumentReference>(relaxed = true)
            } }
        }
        mockViewModel()
        viewModel.setUpListener(false)
        listenerSlot.captured.onEvent(mockedSnapshot, null)
        verify {
            mockedFirestore.collection("users").document(userId).collection("meditation")
                .document("dummy").delete()
        }
        assertTrue(!viewModel.uiState.value.statsMap.containsKey("dummy"))
    }

    @Test
    fun onInitError_statsNotExtracted() {
        val exception = mockk<FirebaseFirestoreException> {
            every { code } returns FirebaseFirestoreException.Code.UNKNOWN
        }
        val listenerSlot = mockFirestore()
        mockViewModel()
        viewModel.setUpListener(false)
        listenerSlot.captured.onEvent(null, exception)
        assertNotSame(viewModel.uiState.value.statsMap, stats)
        assertEquals(viewModel.uiState.value.loadResult.error, exception.code.name)
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
        assertEquals(viewModel.uiState.value.statsMap.size, 4)
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