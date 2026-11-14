package net.neednot.renttracker.feature.payments.data

import kotlinx.datetime.LocalDate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.core.backend.ConvexJson
import net.neednot.renttracker.core.backend.validationField
import net.neednot.renttracker.core.data.PaymentMethod
import net.neednot.renttracker.feature.payments.domain.DuplicateKind
import net.neednot.renttracker.feature.payments.domain.DuplicateMatch
import net.neednot.renttracker.feature.payments.domain.PaymentError

private const val VALIDATION_FAILED = "ValidationFailed"
private const val NEEDS_CONFIRMATION = "NeedsConfirmation"
private const val EDIT_CONFLICT = "EditConflict"
private const val PERMISSION_DENIED = "PermissionDenied"
private const val NOT_FOUND = "NotFound"
private const val TENANT_ARCHIVED = "TenantArchived"

internal fun BackendError.toPaymentError(): PaymentError =
    when (this) {
        is BackendError.Convex -> toPaymentError()
        is BackendError.Server -> PaymentError.Unexpected(message)
        is BackendError.Internal -> PaymentError.Unexpected(message)
    }

private fun BackendError.Convex.toPaymentError(): PaymentError =
    when (code) {
        VALIDATION_FAILED -> PaymentError.ValidationFailed(message, validationField())
        NEEDS_CONFIRMATION -> possibleDuplicate()
        EDIT_CONFLICT -> PaymentError.EditConflict(message, editorName())
        PERMISSION_DENIED -> PaymentError.PermissionDenied(message)
        NOT_FOUND -> PaymentError.NotFound
        TENANT_ARCHIVED -> PaymentError.TenantArchived(message)
        else -> PaymentError.Unexpected("$code: $message")
    }

private fun BackendError.Convex.possibleDuplicate(): PaymentError {
    val details = details?.decodeOrNull(NeedsConfirmationDetails.serializer())
    return if (details == null) {
        PaymentError.Unexpected("$code: $message")
    } else {
        PaymentError.PossibleDuplicate(details.matches.map { it.toDuplicateMatch() })
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

private fun DuplicateMatchDetails.toDuplicateMatch(): DuplicateMatch =
    DuplicateMatch(
        kind = if (kind == DuplicateKindDetails.Payment) DuplicateKind.Payment else DuplicateKind.LateFee,
        date = LocalDate.parse(date),
        amountCents = amountCents,
        method = method?.let { PaymentMethod.valueOf(it.name) },
        byName = byName,
        byAccountId = byAccountId,
    )
