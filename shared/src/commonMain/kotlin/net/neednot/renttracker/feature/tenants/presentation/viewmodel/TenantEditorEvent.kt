package net.neednot.renttracker.feature.tenants.presentation.viewmodel

sealed interface TenantEditorEvent {
    data class TenantAdded(
        val tenantId: String,
    ) : TenantEditorEvent

    data object TenantSaved : TenantEditorEvent
}
