package net.neednot.renttracker.feature.tenants.domain

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
}
