package net.neednot.renttracker.feature.tenants.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.feature.tenants.domain.TenantError

private const val VALIDATION_FAILED = "ValidationFailed"
private const val EDIT_CONFLICT = "EditConflict"
private const val PERMISSION_DENIED = "PermissionDenied"
private const val NOT_FOUND = "NotFound"
private const val TENANT_ARCHIVED = "TenantArchived"

internal fun BackendError.toTenantError(): TenantError =
    when (this) {
        is BackendError.Convex -> toTenantError()
        is BackendError.Server -> TenantError.Unexpected(message)
        is BackendError.Internal -> TenantError.Unexpected(message)
    }

private fun BackendError.Convex.toTenantError(): TenantError =
    when (code) {
        VALIDATION_FAILED -> TenantError.ValidationFailed(message, field())
        EDIT_CONFLICT -> TenantError.EditConflict(message)
        PERMISSION_DENIED -> TenantError.PermissionDenied(message)
        NOT_FOUND -> TenantError.NotFound
        TENANT_ARCHIVED -> TenantError.TenantArchived(message)
        else -> TenantError.Unexpected("$code: $message")
    }

private fun BackendError.Convex.field(): String? = (details as? JsonObject)?.get("field")?.jsonPrimitive?.contentOrNull
