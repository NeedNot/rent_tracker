package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import kotlinx.coroutines.flow.first
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.feature.tenants.domain.TenantError
import net.neednot.renttracker.feature.tenants.domain.TenantOverview
import net.neednot.renttracker.feature.tenants.domain.TenantRepository

internal suspend fun TenantRepository.cachedOrFirstSyncedTenant(
    tenantId: String,
): Synced<TenantOverview?, TenantError> =
    observeTenant(tenantId).first { it.value != null || it.status != SyncStatus.Syncing }

internal suspend fun TenantRepository.tenantFromServer(tenantId: String): Synced<TenantOverview?, TenantError> =
    observeTenant(tenantId).first { it.status != SyncStatus.Syncing }

internal fun Synced<TenantOverview?, TenantError>.loadError(): TenantError =
    (status as? SyncStatus.Failed)?.error ?: TenantError.NotFound
