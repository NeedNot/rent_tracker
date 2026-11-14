package net.neednot.renttracker.feature.tenants.domain

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.MutableStateFlow
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced

class FakeTenantRepository : TenantRepository {
    val tenant = MutableStateFlow<Synced<TenantOverview?, TenantError>>(Synced(null, SyncStatus.Syncing))

    var addTenantResult: Result<String, TenantError> = Ok("t1")
    var addRentTermResult: Result<String, TenantError> = Ok("r9")
    var changeResult: Result<Unit, TenantError> = Ok(Unit)
    var previewResult: Result<RentTermPreview, TenantError> =
        Ok(RentTermPreview(before = BalanceTotal(0, 0), after = BalanceTotal(0, 0)))

    val calls = mutableListOf<TenantRepositoryCall>()

    override fun observeTenant(tenantId: String) = tenant

    override suspend fun addTenant(
        groupId: String,
        details: TenantDetails,
        firstRentTerm: FirstRentTerm,
    ): Result<String, TenantError> {
        calls += TenantRepositoryCall.AddTenant(groupId, details, firstRentTerm)
        return addTenantResult
    }

    override suspend fun editTenant(
        tenantId: String,
        expectedVersion: Long,
        details: TenantDetails,
    ): Result<Unit, TenantError> {
        calls += TenantRepositoryCall.EditTenant(tenantId, expectedVersion, details)
        return changeResult
    }

    override suspend fun archiveTenant(
        tenantId: String,
        expectedVersion: Long,
    ): Result<Unit, TenantError> {
        calls += TenantRepositoryCall.ArchiveTenant(tenantId, expectedVersion)
        return changeResult
    }

    override suspend fun restoreTenant(
        tenantId: String,
        expectedVersion: Long,
    ): Result<Unit, TenantError> {
        calls += TenantRepositoryCall.RestoreTenant(tenantId, expectedVersion)
        return changeResult
    }

    override suspend fun addRentTerm(
        tenantId: String,
        draft: RentTermDraft,
    ): Result<String, TenantError> {
        calls += TenantRepositoryCall.AddRentTerm(tenantId, draft)
        return addRentTermResult
    }

    override suspend fun editRentTerm(
        rentTermId: String,
        expectedVersion: Long,
        draft: RentTermDraft,
    ): Result<Unit, TenantError> {
        calls += TenantRepositoryCall.EditRentTerm(rentTermId, expectedVersion, draft)
        return changeResult
    }

    override suspend fun deleteRentTerm(
        rentTermId: String,
        expectedVersion: Long,
        reason: String,
    ): Result<Unit, TenantError> {
        calls += TenantRepositoryCall.DeleteRentTerm(rentTermId, expectedVersion, reason)
        return changeResult
    }

    override suspend fun previewRentTerm(
        tenantId: String,
        rentTermId: String?,
        draft: RentTermDraft,
    ): Result<RentTermPreview, TenantError> {
        calls += TenantRepositoryCall.PreviewRentTerm(tenantId, rentTermId, draft)
        return previewResult
    }
}
