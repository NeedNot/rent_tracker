package net.neednot.renttracker.feature.group.data

import kotlinx.serialization.SerializationException
import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.core.backend.ConvexJson
import net.neednot.renttracker.core.backend.validationField
import net.neednot.renttracker.feature.group.domain.GroupError

private const val NOT_FOUND = "NotFound"
private const val PERMISSION_DENIED = "PermissionDenied"
private const val VALIDATION_FAILED = "ValidationFailed"
private const val EDIT_CONFLICT = "EditConflict"

internal fun BackendError.toGroupError(): GroupError =
    when (this) {
        is BackendError.Convex -> {
            when (code) {
                NOT_FOUND -> GroupError.NotFound
                PERMISSION_DENIED -> GroupError.NoAccess
                VALIDATION_FAILED -> GroupError.ValidationFailed(message, validationField())
                EDIT_CONFLICT -> GroupError.EditConflict(message, editorName())
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

internal fun BackendError.isPermissionDenied(): Boolean = this is BackendError.Convex && code == PERMISSION_DENIED

private fun BackendError.Convex.editorName(): String? =
    try {
        details?.let { ConvexJson.decode(EditConflictDetails.serializer(), it).lastEditedByName }
    } catch (_: SerializationException) {
        null
    }
