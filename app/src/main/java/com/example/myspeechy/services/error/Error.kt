package com.example.myspeechy.services.error

sealed interface Error

enum class EmailError: Error {
    IS_EMPTY,
    IS_NOT_VALID
}
enum class PasswordError: Error {
    IS_EMPTY,
    IS_NOT_LONG_ENOUGH,
    NOT_ENOUGH_DIGITS,
    IS_NOT_MIXED_CASE
}
