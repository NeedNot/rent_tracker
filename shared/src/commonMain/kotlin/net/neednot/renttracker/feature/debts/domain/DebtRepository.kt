package net.neednot.renttracker.feature.debts.domain

import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.Flow
import net.neednot.renttracker.core.sync.Synced

interface DebtRepository {
    fun observeEntry(
        tenantId: String,
        debtId: String?,
    ): Flow<Synced<ChargeEntryOverview?, DebtError>>

    suspend fun addCharge(
        clientId: String,
        tenantId: String,
        draft: ChargeDraft,
        duplicatePolicy: DuplicatePolicy,
    ): Result<String, DebtError>

    suspend fun editCharge(
        debtId: String,
        expectedVersion: Long,
        draft: ChargeDraft,
    ): Result<Unit, DebtError>
}
