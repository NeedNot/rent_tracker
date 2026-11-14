package net.neednot.renttracker.feature.debts.data

import com.github.michaelbull.result.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.serialization.builtins.serializer
import net.neednot.renttracker.core.backend.Backend
import net.neednot.renttracker.core.data.AsOfClock
import net.neednot.renttracker.core.data.RentTrackerDatabase
import net.neednot.renttracker.core.data.ScreenResultStore
import net.neednot.renttracker.core.data.replaceTenant
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.sync.syncedFlow
import net.neednot.renttracker.core.telemetry.Telemetry
import net.neednot.renttracker.feature.debts.domain.ChargeDraft
import net.neednot.renttracker.feature.debts.domain.ChargeEntryOverview
import net.neednot.renttracker.feature.debts.domain.DebtError
import net.neednot.renttracker.feature.debts.domain.DebtRepository
import net.neednot.renttracker.feature.debts.domain.DuplicatePolicy
import net.neednot.renttracker.model.debts.DebtsAddCustomArgs
import net.neednot.renttracker.model.debts.DebtsAddLateFeeArgs
import net.neednot.renttracker.model.debts.DebtsEditArgs
import net.neednot.renttracker.model.tenants.TenantsScreenArgs
import net.neednot.renttracker.model.tenants.TenantsScreenResult

class DebtRepositoryImpl(
    backend: Backend,
    private val asOfClock: AsOfClock,
    private val database: RentTrackerDatabase,
    private val screenResultStore: ScreenResultStore,
    telemetry: Telemetry,
) : DebtRepository {
    private val calls = DebtBackendCalls(backend, telemetry)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeEntry(
        tenantId: String,
        debtId: String?,
    ): Flow<Synced<ChargeEntryOverview?, DebtError>> =
        syncedFlow(
            cached = database.observeChargeEntry(tenantId, debtId, asOfClock.asOf),
            serverResults =
                asOfClock.asOf.flatMapLatest { asOf ->
                    calls.subscribe(
                        name = TENANT_SCREEN,
                        args = TenantsScreenArgs(asOf = asOf, tenantId = tenantId),
                        argsSerializer = TenantsScreenArgs.serializer(),
                        resultSerializer = TenantsScreenResult.serializer(),
                    )
                },
            store = { result -> screenResultStore.replaceTenant(tenantId, result) },
        )

    override suspend fun addCharge(
        clientId: String,
        tenantId: String,
        draft: ChargeDraft,
        duplicatePolicy: DuplicatePolicy,
    ): Result<String, DebtError> =
        when (draft) {
            is ChargeDraft.LateFee -> {
                calls.mutate(
                    name = ADD_LATE_FEE,
                    args = draft.toAddArgs(clientId, tenantId, duplicatePolicy),
                    argsSerializer = DebtsAddLateFeeArgs.serializer(),
                    resultSerializer = String.serializer(),
                )
            }

            is ChargeDraft.Custom -> {
                calls.mutate(
                    name = ADD_CUSTOM,
                    args = draft.toAddArgs(clientId, tenantId),
                    argsSerializer = DebtsAddCustomArgs.serializer(),
                    resultSerializer = String.serializer(),
                )
            }
        }

    override suspend fun editCharge(
        debtId: String,
        expectedVersion: Long,
        draft: ChargeDraft,
    ): Result<Unit, DebtError> =
        calls.mutateWithoutAnswer(
            name = EDIT_DEBT,
            args = draft.toEditArgs(debtId, expectedVersion),
            argsSerializer = DebtsEditArgs.serializer(),
        )

    private companion object {
        const val TENANT_SCREEN = "tenants:screen"
        const val ADD_LATE_FEE = "debts:addLateFee"
        const val ADD_CUSTOM = "debts:addCustom"
        const val EDIT_DEBT = "debts:edit"
    }
}
