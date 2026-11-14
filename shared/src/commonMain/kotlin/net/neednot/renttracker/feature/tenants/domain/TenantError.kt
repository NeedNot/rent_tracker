package net.neednot.renttracker.feature.tenants.domain

sealed interface TenantError {
    data class ValidationFailed(
        val message: String,
        val field: String? = null,
    ) : TenantError

    data class EditConflict(
        val message: String,
    ) : TenantError

    data object NotFound : TenantError

    data class TenantArchived(
        val message: String,
    ) : TenantError

    data object Offline : TenantError

    data class Unexpected(
        val message: String,
    ) : TenantError
}
