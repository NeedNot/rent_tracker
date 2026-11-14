package net.neednot.renttracker.feature.tenants.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

sealed interface TenantRepositoryCall {
    data class AddTenant(
        val groupId: String,
        val details: TenantDetails,
        val firstRentTerm: FirstRentTerm,
    ) : TenantRepositoryCall

    data class EditTenant(
        val tenantId: String,
        val expectedVersion: Long,
        val details: TenantDetails,
    ) : TenantRepositoryCall

    data class ArchiveTenant(
        val tenantId: String,
        val expectedVersion: Long,
    ) : TenantRepositoryCall

    data class RestoreTenant(
        val tenantId: String,
        val expectedVersion: Long,
    ) : TenantRepositoryCall

    data class AddRentTerm(
        val tenantId: String,
        val draft: RentTermDraft,
    ) : TenantRepositoryCall

    data class EditRentTerm(
        val rentTermId: String,
        val expectedVersion: Long,
        val draft: RentTermDraft,
    ) : TenantRepositoryCall

    data class DeleteRentTerm(
        val rentTermId: String,
        val expectedVersion: Long,
        val reason: String,
    ) : TenantRepositoryCall

    data class PreviewRentTerm(
        val tenantId: String,
        val rentTermId: String?,
        val draft: RentTermDraft,
    ) : TenantRepositoryCall

    data class CancelPayment(
        val paymentId: String,
        val expectedVersion: Long,
        val reason: String,
    ) : TenantRepositoryCall

    data class RestorePayment(
        val paymentId: String,
        val expectedVersion: Long,
    ) : TenantRepositoryCall

    data class MarkPaymentBounced(
        val paymentId: String,
        val expectedVersion: Long,
    ) : TenantRepositoryCall

    data class CancelDebt(
        val debtId: String,
        val expectedVersion: Long,
        val reason: String,
    ) : TenantRepositoryCall

    data class RestoreDebt(
        val debtId: String,
        val expectedVersion: Long,
    ) : TenantRepositoryCall

    data class AddBouncedCheckFee(
        val tenantId: String,
        val amountCents: Long,
        val date: LocalDate,
    ) : TenantRepositoryCall

    data class DismissLateFeeSuggestion(
        val tenantId: String,
        val month: YearMonth,
    ) : TenantRepositoryCall
}
