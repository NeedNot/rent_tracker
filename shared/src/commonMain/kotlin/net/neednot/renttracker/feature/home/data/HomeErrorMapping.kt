package net.neednot.renttracker.feature.home.data

import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.feature.home.domain.HomeError

private const val VALIDATION_FAILED = "ValidationFailed"

internal fun BackendError.toHomeError(): HomeError =
    when (this) {
        is BackendError.Convex -> {
            when (code) {
                VALIDATION_FAILED -> HomeError.ValidationFailed(message)
                else -> HomeError.Unexpected("$code: $message")
            }
        }

        is BackendError.Server -> {
            HomeError.Unexpected(message)
        }

        is BackendError.Internal -> {
            HomeError.Unexpected(message)
        }
    }
