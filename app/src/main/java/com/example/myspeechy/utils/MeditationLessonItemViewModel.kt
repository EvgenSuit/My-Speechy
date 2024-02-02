package com.example.myspeechy.utils

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myspeechy.data.LessonItem
import com.example.myspeechy.data.LessonRepository
import com.example.myspeechy.data.MeditationLessonItemState
import com.example.myspeechy.services.MeditationLessonServiceImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class MeditationLessonItemViewModel @Inject constructor(
    private val lessonRepository: LessonRepository,
    private val lessonServiceImpl: MeditationLessonServiceImpl,
    savedStateHandle: SavedStateHandle
): ViewModel() {
    private val id: Int = checkNotNull(savedStateHandle["meditationLessonItemId"])
    private val _uiState = MutableStateFlow(MeditationLessonItemState(LessonItem()))
    val uiState = _uiState.asStateFlow()
    private var job: Job = Job()
    private var breathingJob: Job = Job()
    init {
        viewModelScope.launch {
            lessonRepository.selectLessonItem(id).collect {lesson ->
                val lessonItem = lessonServiceImpl.convertToLessonItem(lesson)
                _uiState.update { MeditationLessonItemState(lessonItem) }
            }
        }
    }

    fun start() {
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
            }
        }
        job.invokeOnCompletion { if (it == null) cancel() }
        return job
    }

    private fun getBreathingJob(): Job {
        val job = viewModelScope.launch {
            while(!_uiState.value.paused && _uiState.value.started) {
                _uiState.update {
                    it.copy(breathingIn = !it.breathingIn)
                }
                delay(2000)
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