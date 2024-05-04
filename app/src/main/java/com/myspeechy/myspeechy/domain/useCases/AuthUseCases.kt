package com.myspeechy.myspeechy.domain.useCases

import com.myspeechy.myspeechy.R
import com.myspeechy.myspeechy.domain.InputFormatCheckResult
import com.myspeechy.myspeechy.domain.auth.AuthService
import com.myspeechy.myspeechy.domain.error.EmailError
import com.myspeechy.myspeechy.domain.error.PasswordError
import com.myspeechy.myspeechy.presentation.UiText

class ValidatePasswordUseCase(
    private val authService: AuthService) {
    operator fun invoke(password: String): UiText =
        when(val res = authService.validatePassword(password)) {
            is InputFormatCheckResult.Error -> {
                when (res.error) {
                    PasswordError.IS_EMPTY -> UiText.StringResource(R.string.password_is_empty)
                    PasswordError.IS_NOT_MIXED_CASE -> UiText.StringResource(R.string.password_is_not_mixed_case)
                    PasswordError.IS_NOT_LONG_ENOUGH -> UiText.StringResource(R.string.password_is_not_long_enough)
                    PasswordError.NOT_ENOUGH_DIGITS -> UiText.StringResource(R.string.password_not_enough_digits)
                }
            }
            is InputFormatCheckResult.Success -> { UiText.StringResource.DynamicString("") }
        }
}

class ValidateEmailUseCase(
    private val authService: AuthService) {
    operator fun invoke(email: String): UiText =
        when(val res = authService.validateEmail(email)) {
            is InputFormatCheckResult.Error -> {
                when (res.error) {
                    EmailError.IS_NOT_VALID -> UiText.StringResource(R.string.email_not_valid)
                    EmailError.IS_EMPTY -> UiText.StringResource(R.string.email_is_empty)
                }
            }

            is InputFormatCheckResult.Success -> {
               UiText.StringResource.DynamicString("")
            }
        }
}