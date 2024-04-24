package com.example.myspeechy.presentation.lesson.meditation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myspeechy.data.DataStoreManager
import com.example.myspeechy.data.lesson.LessonItem
import com.example.myspeechy.data.lesson.LessonRepository
import com.example.myspeechy.data.lesson.MeditationLessonItemState
import com.example.myspeechy.domain.MeditationNotificationServiceImpl
import com.example.myspeechy.domain.Result
import com.example.myspeechy.domain.lesson.MeditationLessonServiceImpl
import com.example.myspeechy.domain.meditation.MeditationStatsServiceImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class MeditationLessonItemViewModel @Inject constructor(
    private val lessonRepository: LessonRepository,
    private val lessonServiceImpl: MeditationLessonServiceImpl,
    private val meditationStatsServiceImpl: MeditationStatsServiceImpl,
    private val meditationNotificationServiceImpl: MeditationNotificationServiceImpl,
    private val dataStoreManager: DataStoreManager,
    savedStateHandle: SavedStateHandle
): ViewModel() {
     val id: Int = checkNotNull(savedStateHandle["meditationLessonItemId"])
    private val _uiState = MutableStateFlow(MeditationLessonItemState(LessonItem()))
    val uiState = _uiState.asStateFlow()
    val saveResultFlow = _uiState.map { it.saveResult }
    private var job: Job = Job()
    private var breathingJob: Job = Job()
    private val breathingInterval: Long = 3000

    init {
        viewModelScope.launch {
            lessonRepository.selectLessonItem(id).collect {lesson ->
                println(lesson)
                val lessonItem = lessonServiceImpl.convertToLessonItem(lesson)
                _uiState.update { it.copy(lessonItem = lessonItem) }
            }
        }
        viewModelScope.launch {
            dataStoreManager.collectMeditationNotificationStatus {isCancelled ->
                if (isCancelled) cancel()
            }
        }
    }

    fun start() {
        viewModelScope.launch {
            dataStoreManager.editMeditationNotificationStatus(false)
            _uiState.update { it.copy(started = true, saveResult = Result.Idle) }
            job = getMeditationJob()
            breathingJob = getBreathingJob()
        }
    }

    fun pause() {
        _uiState.update { it.copy(paused = true) }
        job.cancel()
        breathingJob.cancel()
    }

    fun resume() {
        _uiState.update { it.copy(paused = false) }
        job = getMeditationJob()
        breathingJob = getBreathingJob()
    }

    fun cancel() {
        if (job.isCancelled && _uiState.value.paused) {
            onJobCompletion()
        } else {
            job.cancel()
            breathingJob.cancel()
        }
        _uiState.update { MeditationLessonItemState(lessonItem = it.lessonItem,
            saveResult = it.saveResult) }
    }

    private fun getMeditationJob(): Job {
        val job = viewModelScope.launch {
            while(_uiState.value.passedTime < _uiState.value.setTime) {
                delay(1000)
                _uiState.update {
                    it.copy(passedTime = it.passedTime+1)
                }
                val seconds = _uiState.value.passedTime
                meditationNotificationServiceImpl.sendMeditationNotification(
                    seconds.toString()
                )
            }
            cancel()
        }
        job.invokeOnCompletion {
            if (!_uiState.value.paused) {
                onJobCompletion()
            }
        }
        return job
    }

    private fun onJobCompletion() {
        meditationNotificationServiceImpl.cancelNotification()
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        runBlocking {
            try {
                meditationStatsServiceImpl.updateStats(date, _uiState.value.passedTime)
                updateSaveResult(Result.Success(""))
            } catch (e: Exception) {
                updateSaveResult(Result.Error(e.message!!))
            }
        }
    }

    private fun getBreathingJob(): Job {
        val job = viewModelScope.launch {
            while(!_uiState.value.paused && _uiState.value.started) {
                _uiState.update { it.copy(breathingIn = !it.breathingIn) }
                delay(breathingInterval)
            }
        }
        job.invokeOnCompletion { if (it == null) breathingJob.cancel() }
        return job
    }

    fun setMeditationTime(time: Int) {
        _uiState.update {
            it.copy(setTime = TimeUnit.MINUTES.toSeconds(time.toLong()).toInt())
        }
    }

    fun markAsComplete() {
        viewModelScope.launch {
            lessonServiceImpl.markAsComplete(_uiState.value.lessonItem)
        }
    }

    private fun updateSaveResult(result: Result) {
        _uiState.update { it.copy(saveResult = result) }
    }
}