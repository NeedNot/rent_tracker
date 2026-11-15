package net.neednot.renttracker.feature.tenants.data

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import net.neednot.renttracker.core.backend.Backend
import net.neednot.renttracker.core.data.AsOfClock
import net.neednot.renttracker.core.telemetry.Telemetry
import net.neednot.renttracker.feature.tenants.domain.MoveTarget
import net.neednot.renttracker.feature.tenants.domain.MoveTenantRepository
import net.neednot.renttracker.feature.tenants.domain.TenantError
import net.neednot.renttracker.model.tenants.TenantsMoveArgs
import net.neednot.renttracker.model.tenants.TenantsMoveTargetsArgs
import net.neednot.renttracker.model.tenants.TenantsMoveTargetsResult

class MoveTenantRepositoryImpl(
    backend: Backend,
    private val asOfClock: AsOfClock,
    telemetry: Telemetry,
) : MoveTenantRepository {
    private val calls = TenantBackendCalls(backend, telemetry)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeTargets(tenantId: String): Flow<Result<List<MoveTarget>, TenantError>> =
        asOfClock.asOf
            .flatMapLatest { asOf ->
                calls.subscribe(
                    name = MOVE_TARGETS,
                    args = TenantsMoveTargetsArgs(asOf = asOf, tenantId = tenantId),
                    argsSerializer = TenantsMoveTargetsArgs.serializer(),
                    resultSerializer = TenantsMoveTargetsResult.serializer(),
                )
            }.map { result -> result.map { it.toMoveTargets() } }

    override suspend fun move(
        tenantId: String,
        toGroupId: String,
        expectedVersion: Long,
    ): Result<Unit, TenantError> =
        calls.mutateWithoutAnswer(
            name = MOVE_TENANT,
            args = TenantsMoveArgs(expectedVersion = expectedVersion, tenantId = tenantId, toGroupId = toGroupId),
            argsSerializer = TenantsMoveArgs.serializer(),
        )

    private companion object {
        const val MOVE_TARGETS = "tenants:moveTargets"
        const val MOVE_TENANT = "tenants:move"
    }
}
