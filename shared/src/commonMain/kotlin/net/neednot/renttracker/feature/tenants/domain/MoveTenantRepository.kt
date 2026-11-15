package net.neednot.renttracker.feature.tenants.domain

import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.Flow

interface MoveTenantRepository {
    fun observeTargets(tenantId: String): Flow<Result<List<MoveTarget>, TenantError>>

    suspend fun move(
        tenantId: String,
        toGroupId: String,
        expectedVersion: Long,
    ): Result<Unit, TenantError>
}
