package net.neednot.renttracker.feature.members.data

import kotlinx.serialization.SerializationException
import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.core.backend.ConvexJson
import net.neednot.renttracker.core.backend.validationField
import net.neednot.renttracker.feature.members.domain.MembersError

private const val VALIDATION_FAILED = "ValidationFailed"
private const val EDIT_CONFLICT = "EditConflict"
private const val PERMISSION_DENIED = "PermissionDenied"
private const val NOT_FOUND = "NotFound"

internal fun BackendError.toMembersError(): MembersError =
    when (this) {
        is BackendError.Convex -> toMembersError()
        is BackendError.Server -> MembersError.Unexpected(message)
        is BackendError.Internal -> MembersError.Unexpected(message)
    }

private fun BackendError.Convex.toMembersError(): MembersError =
    when (code) {
        VALIDATION_FAILED -> MembersError.ValidationFailed(message, validationField())
        EDIT_CONFLICT -> MembersError.EditConflict(message, editorName())
        PERMISSION_DENIED -> MembersError.PermissionDenied(message)
        NOT_FOUND -> MembersError.NotFound(message)
        else -> MembersError.Unexpected("$code: $message")
    }

private fun BackendError.Convex.editorName(): String? {
    val details = details ?: return null
    return try {
        ConvexJson.decode(EditConflictDetails.serializer(), details).lastEditedByName
    } catch (_: SerializationException) {
        null
    }
}
