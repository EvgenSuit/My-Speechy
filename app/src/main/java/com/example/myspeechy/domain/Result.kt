package com.example.myspeechy.domain

import com.example.myspeechy.presentation.RootError

sealed interface InputFormatCheckResult<out D, out E: RootError> {
    data class Success<out D, out E: RootError>(val data: D): InputFormatCheckResult<D, E>
    data class Error<out D, out E: RootError>(val error: E): InputFormatCheckResult<D, E>
}
sealed class Result(val data: String = "", val error: String = "") {
    data object Idle: Result()
    data object InProgress: Result()
    class Success(data: String): Result(data)
    class Error(error: String): Result(error = error)
}
