package net.neednot.renttracker.feature.debts.domain

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced

class FakeDebtRepository : DebtRepository {
    val entry = MutableStateFlow<Synced<ChargeEntryOverview?, DebtError>>(Synced(null, SyncStatus.Syncing))

    var addResult: Result<String, DebtError> = Ok("d9")
    var editResult: Result<Unit, DebtError> = Ok(Unit)
    var saveGate: CompletableDeferred<Unit>? = null

    val calls = mutableListOf<DebtRepositoryCall>()

    override fun observeEntry(
        tenantId: String,
        debtId: String?,
    ): Flow<Synced<ChargeEntryOverview?, DebtError>> = entry

    override suspend fun addCharge(
        clientId: String,
        tenantId: String,
        draft: ChargeDraft,
        duplicatePolicy: DuplicatePolicy,
    ): Result<String, DebtError> {
        calls += DebtRepositoryCall.Add(clientId, tenantId, draft, duplicatePolicy)
        saveGate?.await()
        return addResult
    }

    override suspend fun editCharge(
        debtId: String,
        expectedVersion: Long,
        draft: ChargeDraft,
    ): Result<Unit, DebtError> {
        calls += DebtRepositoryCall.Edit(debtId, expectedVersion, draft)
        saveGate?.await()
        return editResult
    }
}
