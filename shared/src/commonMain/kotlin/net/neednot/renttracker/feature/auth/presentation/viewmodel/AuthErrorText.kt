package net.neednot.renttracker.feature.auth.presentation.viewmodel

import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.auth.domain.AuthError
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.auth_error_code_expired
import net.neednot.renttracker.resources.auth_error_code_rate_limited
import net.neednot.renttracker.resources.auth_error_code_rate_limited_minutes
import net.neednot.renttracker.resources.auth_error_code_rate_limited_seconds
import net.neednot.renttracker.resources.auth_error_display_name_length
import net.neednot.renttracker.resources.auth_error_google_failed
import net.neednot.renttracker.resources.auth_error_invalid_code
import net.neednot.renttracker.resources.auth_error_offline
import net.neednot.renttracker.resources.auth_error_send_rate_limited
import net.neednot.renttracker.resources.auth_error_send_rate_limited_minutes
import net.neednot.renttracker.resources.auth_error_send_rate_limited_seconds
import net.neednot.renttracker.resources.auth_error_session_expired
import net.neednot.renttracker.resources.auth_error_too_many_attempts
import net.neednot.renttracker.resources.auth_error_unexpected
import org.jetbrains.compose.resources.StringResource

private const val SECONDS_PER_MINUTE = 60

internal fun AuthError.whileSendingCode(): UiText? =
    when (this) {
        is AuthError.RateLimited -> {
            waitText(
                retryAfterSeconds = retryAfterSeconds,
                unknown = Res.string.auth_error_send_rate_limited,
                seconds = Res.string.auth_error_send_rate_limited_seconds,
                minutes = Res.string.auth_error_send_rate_limited_minutes,
            )
        }

        else -> {
            commonText()
        }
    }

internal fun AuthError.whileCheckingCode(): UiText? =
    when (this) {
        is AuthError.RateLimited -> {
            waitText(
                retryAfterSeconds = retryAfterSeconds,
                unknown = Res.string.auth_error_code_rate_limited,
                seconds = Res.string.auth_error_code_rate_limited_seconds,
                minutes = Res.string.auth_error_code_rate_limited_minutes,
            )
        }

        else -> {
            commonText()
        }
    }

internal fun AuthError.commonText(): UiText? =
    when (this) {
        AuthError.GoogleSignInCancelled -> null
        AuthError.InvalidCode -> UiText.Resource(Res.string.auth_error_invalid_code)
        AuthError.CodeExpired -> UiText.Resource(Res.string.auth_error_code_expired)
        AuthError.TooManyAttempts -> UiText.Resource(Res.string.auth_error_too_many_attempts)
        AuthError.Offline -> UiText.Resource(Res.string.auth_error_offline)
        AuthError.SessionExpired -> UiText.Resource(Res.string.auth_error_session_expired)
        AuthError.GoogleSignInFailed -> UiText.Resource(Res.string.auth_error_google_failed)
        AuthError.InvalidDisplayName -> UiText.Resource(Res.string.auth_error_display_name_length)
        is AuthError.RateLimited -> UiText.Resource(Res.string.auth_error_send_rate_limited)
        is AuthError.Unexpected -> UiText.Resource(Res.string.auth_error_unexpected)
    }

private fun waitText(
    retryAfterSeconds: Int?,
    unknown: StringResource,
    seconds: StringResource,
    minutes: StringResource,
): UiText =
    when {
        retryAfterSeconds == null || retryAfterSeconds <= 0 -> UiText.Resource(unknown)
        retryAfterSeconds < SECONDS_PER_MINUTE -> UiText.Resource(seconds, listOf(retryAfterSeconds))
        else -> UiText.Resource(minutes, listOf((retryAfterSeconds + SECONDS_PER_MINUTE - 1) / SECONDS_PER_MINUTE))
    }
