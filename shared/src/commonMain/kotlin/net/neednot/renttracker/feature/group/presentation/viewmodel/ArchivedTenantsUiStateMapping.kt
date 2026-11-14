package net.neednot.renttracker.feature.group.presentation.viewmodel

import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.feature.group.domain.GroupDetails
import net.neednot.renttracker.feature.group.domain.GroupError
import net.neednot.renttracker.feature.group.domain.GroupTenant
import net.neednot.renttracker.feature.group.presentation.uistate.ArchivedTenantRowUiState
import net.neednot.renttracker.feature.group.presentation.uistate.ArchivedTenantsUiState

internal fun Synced<GroupDetails?, GroupError>.toArchivedTenantsUiState(): ArchivedTenantsUiState {
    val syncError = (status as? SyncStatus.Failed)?.error?.text()
    val archivedTenants = value?.archivedTenants.orEmpty()
    return when {
        archivedTenants.isNotEmpty() -> {
            ArchivedTenantsUiState.Content(tenants = archivedTenants.map { it.toRowUiState() }, syncError = syncError)
        }

        syncError != null -> {
            ArchivedTenantsUiState.Error(syncError)
        }

        status == SyncStatus.Syncing -> {
            ArchivedTenantsUiState.Loading
        }

        else -> {
            ArchivedTenantsUiState.Empty
        }
    }
}

private fun GroupTenant.toRowUiState(): ArchivedTenantRowUiState =
    ArchivedTenantRowUiState(
        tenantId = tenantId,
        name = name,
        contactName = contactName,
        balance = balance.text(),
    )
