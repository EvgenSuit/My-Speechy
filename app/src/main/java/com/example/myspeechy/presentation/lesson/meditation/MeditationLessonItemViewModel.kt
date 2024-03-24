package com.example.myspeechy.presentation.lesson.meditation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myspeechy.data.lesson.LessonItem
import com.example.myspeechy.data.lesson.LessonRepository
import com.example.myspeechy.data.lesson.MeditationLessonItemState
import com.example.myspeechy.data.meditation.MeditationStats
import com.example.myspeechy.data.meditation.MeditationStatsRepository
import com.example.myspeechy.services.lesson.MeditationLessonServiceImpl
import com.example.myspeechy.services.MeditationNotificationServiceImpl
import com.example.myspeechy.services.meditation.MeditationStatsServiceImpl
import com.example.myspeechy.services.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class MeditationLessonItemViewModel @Inject constructor(
    private val lessonRepository: LessonRepository,
    private val lessonServiceImpl: MeditationLessonServiceImpl,
    private val meditationStatsRepository: MeditationStatsRepository,
    private val meditationStatsServiceImpl: MeditationStatsServiceImpl,
    private val meditationNotificationServiceImpl: MeditationNotificationServiceImpl,
    savedStateHandle: SavedStateHandle
): ViewModel() {
    private val id: Int = checkNotNull(savedStateHandle["meditationLessonItemId"])
    private val _uiState = MutableStateFlow(MeditationLessonItemState(LessonItem()))
    val uiState = _uiState.asStateFlow()
    private val canceledThroughNotification = NotificationRepository.canceled.asStateFlow()
    private var job: Job = Job()
    private var breathingJob: Job = Job()
    private var currentMeditationStats: MeditationStats? = null

    init {
        viewModelScope.launch {
            lessonRepository.selectLessonItem(id).collect {lesson ->
                val lessonItem = lessonServiceImpl.convertToLessonItem(lesson)
                _uiState.update { MeditationLessonItemState(lessonItem) }
            }
        }
        viewModelScope.launch {
            canceledThroughNotification.collect{isCancelled ->
            if (isCancelled) cancel()}
        }
    }

    fun start() {
        NotificationRepository.canceled.value = false
        _uiState.update {
            it.copy(started = true)
        }
        job = getMeditationJob()
        breathingJob = getBreathingJob()
    }

    fun pause() {
        job.cancel()
        breathingJob.cancel()
        _uiState.update {
            it.copy(paused = true)
        }
    }

    fun resume() {
        _uiState.update {
            it.copy(paused = false)
        }
        job = getMeditationJob()
        breathingJob = getBreathingJob()
    }

    fun cancel() {
        job.cancel()
        breathingJob.cancel()
        meditationNotificationServiceImpl.cancelNotification()
        _uiState.update {
            MeditationLessonItemState(_uiState.value.lessonItem)
        }
    }

    private fun getMeditationJob(): Job {
        val job = viewModelScope.launch {
            while(_uiState.value.passedTime*60*60 < _uiState.value.setTime*60*60) {
                delay(1000)
                _uiState.update {
                    it.copy(passedTime = it.passedTime+1)
                }
                val seconds = _uiState.value.passedTime
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                if (seconds % 60 == 0) {
                    val minutes = seconds / 60
                    var totalMinutes = 1
                    currentMeditationStats =
                        meditationStatsRepository.getCurrentMeditationStats(date).first()
                    if (currentMeditationStats == null) {
                        meditationStatsRepository.insertMeditationStats(MeditationStats(date = date, minutes = minutes))
                    } else {
                        totalMinutes = currentMeditationStats!!.minutes + 1
                        meditationStatsRepository.updateMeditationStats(MeditationStats(date = date, minutes = totalMinutes))
                    }
                    meditationStatsServiceImpl.updateStats(date, totalMinutes)
                }
                meditationNotificationServiceImpl.sendMeditationNotification(
                    seconds.toString()
                )
            }
        }
        job.invokeOnCompletion { if (it == null) cancel()
        else if (it is CancellationException) meditationNotificationServiceImpl.cancelNotification()
        }
        return job
    }

    private fun getBreathingJob(): Job {
        val job = viewModelScope.launch {
            while(!_uiState.value.paused && _uiState.value.started) {
                _uiState.update {
                    it.copy(breathingIn = !it.breathingIn)
                }
                delay(_uiState.value.breathingInterval)
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
        lessonServiceImpl.markAsComplete(_uiState.value.lessonItem)
    }
}