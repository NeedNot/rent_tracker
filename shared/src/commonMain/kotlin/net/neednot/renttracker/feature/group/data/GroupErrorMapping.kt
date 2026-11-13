package net.neednot.renttracker.feature.group.data

import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.feature.group.domain.GroupError

private const val NOT_FOUND = "NotFound"

internal fun BackendError.toGroupError(): GroupError =
    when (this) {
        is BackendError.Convex -> {
            when (code) {
                NOT_FOUND -> GroupError.NotFound
                else -> GroupError.Unexpected("$code: $message")
            }
        }

        is BackendError.Server -> {
            GroupError.Unexpected(message)
        }

        is BackendError.Internal -> {
            GroupError.Unexpected(message)
        }
    }
