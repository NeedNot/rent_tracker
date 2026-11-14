package net.neednot.renttracker.feature.debts.data

import kotlinx.datetime.LocalDate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.core.backend.ConvexJson
import net.neednot.renttracker.feature.debts.domain.DebtError
import net.neednot.renttracker.feature.debts.domain.DuplicateLateFee

private const val VALIDATION_FAILED = "ValidationFailed"
private const val NEEDS_CONFIRMATION = "NeedsConfirmation"
private const val EDIT_CONFLICT = "EditConflict"
private const val PERMISSION_DENIED = "PermissionDenied"
private const val NOT_FOUND = "NotFound"
private const val TENANT_ARCHIVED = "TenantArchived"

internal fun BackendError.toDebtError(): DebtError =
    when (this) {
        is BackendError.Convex -> toDebtError()
        is BackendError.Server -> DebtError.Unexpected(message)
        is BackendError.Internal -> DebtError.Unexpected(message)
    }

private fun BackendError.Convex.toDebtError(): DebtError =
    when (code) {
        VALIDATION_FAILED -> DebtError.ValidationFailed(message)
        NEEDS_CONFIRMATION -> possibleDuplicate()
        EDIT_CONFLICT -> DebtError.EditConflict(message, editorName())
        PERMISSION_DENIED -> DebtError.PermissionDenied(message)
        NOT_FOUND -> DebtError.NotFound
        TENANT_ARCHIVED -> DebtError.TenantArchived(message)
        else -> DebtError.Unexpected("$code: $message")
    }

private fun BackendError.Convex.possibleDuplicate(): DebtError {
    val details = details?.decodeOrNull(NeedsConfirmationDetails.serializer())
    return if (details == null) {
        DebtError.Unexpected("$code: $message")
    } else {
        DebtError.PossibleDuplicate(details.matches.map { it.toDuplicateLateFee() })
    }
}

private fun BackendError.Convex.editorName(): String? =
    details?.decodeOrNull(EditConflictDetails.serializer())?.lastEditedByName

private fun <T> JsonElement.decodeOrNull(serializer: KSerializer<T>): T? =
    try {
        ConvexJson.decode(serializer, this)
    } catch (_: SerializationException) {
        null
    }

private fun DuplicateLateFeeDetails.toDuplicateLateFee(): DuplicateLateFee =
    DuplicateLateFee(
        date = LocalDate.parse(date),
        amountCents = amountCents,
        byName = byName,
        byAccountId = byAccountId,
    )
