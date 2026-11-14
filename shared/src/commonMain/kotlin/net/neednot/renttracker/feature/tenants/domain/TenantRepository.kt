package net.neednot.renttracker.feature.tenants.domain

import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.sync.Synced

interface TenantRepository {
    fun observeTenant(tenantId: String): Flow<Synced<TenantOverview?, TenantError>>

    suspend fun addTenant(
        groupId: String,
        details: TenantDetails,
        firstRentTerm: FirstRentTerm,
    ): Result<String, TenantError>

    suspend fun editTenant(
        tenantId: String,
        expectedVersion: Long,
        details: TenantDetails,
    ): Result<Unit, TenantError>

    suspend fun archiveTenant(
        tenantId: String,
        expectedVersion: Long,
    ): Result<Unit, TenantError>

    suspend fun restoreTenant(
        tenantId: String,
        expectedVersion: Long,
    ): Result<Unit, TenantError>

    suspend fun addRentTerm(
        tenantId: String,
        draft: RentTermDraft,
    ): Result<String, TenantError>

    suspend fun editRentTerm(
        rentTermId: String,
        expectedVersion: Long,
        draft: RentTermDraft,
    ): Result<Unit, TenantError>

    suspend fun deleteRentTerm(
        rentTermId: String,
        expectedVersion: Long,
        reason: String,
    ): Result<Unit, TenantError>

    suspend fun previewRentTerm(
        tenantId: String,
        rentTermId: String?,
        draft: RentTermDraft,
    ): Result<RentTermPreview, TenantError>

    suspend fun dismissLateFeeSuggestion(
        tenantId: String,
        month: YearMonth,
    )
}
