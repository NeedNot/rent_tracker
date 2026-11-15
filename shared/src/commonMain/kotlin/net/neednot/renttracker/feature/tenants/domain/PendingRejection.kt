package net.neednot.renttracker.feature.tenants.domain

sealed interface PendingRejection {
    data object NoPermission : PendingRejection

    data object TenantArchived : PendingRejection

    data object TenantMoved : PendingRejection

    data class Other(
        val message: String,
    ) : PendingRejection
}
