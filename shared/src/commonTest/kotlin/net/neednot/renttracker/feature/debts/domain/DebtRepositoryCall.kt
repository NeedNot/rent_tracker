package net.neednot.renttracker.feature.debts.domain

sealed interface DebtRepositoryCall {
    data class Add(
        val clientId: String,
        val tenantId: String,
        val draft: ChargeDraft,
        val duplicatePolicy: DuplicatePolicy,
    ) : DebtRepositoryCall

    data class Edit(
        val debtId: String,
        val expectedVersion: Long,
        val draft: ChargeDraft,
    ) : DebtRepositoryCall
}
