package net.neednot.renttracker.feature.tenants.domain

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull

class FakeMoveTenantRepository : MoveTenantRepository {
    val targets = MutableStateFlow<Result<List<MoveTarget>, TenantError>?>(null)
    var moveResult: Result<Unit, TenantError> = Ok(Unit)
    val moves = mutableListOf<Triple<String, String, Long>>()

    override fun observeTargets(tenantId: String) = targets.filterNotNull()

    override suspend fun move(
        tenantId: String,
        toGroupId: String,
        expectedVersion: Long,
    ): Result<Unit, TenantError> {
        moves += Triple(tenantId, toGroupId, expectedVersion)
        return moveResult
    }
}
