package net.neednot.renttracker.feature.tenants.presentation.viewmodel

sealed interface MoveTenantEvent {
    data object TenantMoved : MoveTenantEvent
}
