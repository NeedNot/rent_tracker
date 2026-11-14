package net.neednot.renttracker.feature.debts.presentation.viewmodel

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import net.neednot.renttracker.feature.debts.domain.ChargeDraft
import net.neednot.renttracker.feature.debts.domain.DebtError
import net.neednot.renttracker.feature.debts.domain.DebtRepository
import net.neednot.renttracker.feature.debts.domain.DuplicatePolicy

internal suspend fun DebtRepository.save(
    request: ChargeSaveRequest,
    draft: ChargeDraft,
    duplicatePolicy: DuplicatePolicy,
): Result<Unit, DebtError> {
    val debtId = request.debtId
    val version = request.expectedVersion
    return when {
        debtId == null -> {
            addCharge(request.clientId, request.tenantId, draft, duplicatePolicy).map { }
        }

        version == null -> {
            Err(DebtError.NotFound)
        }

        else -> {
            editCharge(debtId, version, draft)
        }
    }
}
