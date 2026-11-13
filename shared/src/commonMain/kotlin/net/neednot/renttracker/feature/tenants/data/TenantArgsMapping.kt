package net.neednot.renttracker.feature.tenants.data

import net.neednot.renttracker.feature.tenants.domain.BalanceTotal
import net.neednot.renttracker.feature.tenants.domain.FirstRentTerm
import net.neednot.renttracker.feature.tenants.domain.RentTermDraft
import net.neednot.renttracker.feature.tenants.domain.RentTermPreview
import net.neednot.renttracker.feature.tenants.domain.TenantDetails
import net.neednot.renttracker.model.rentterms.RentTermsAddArgs
import net.neednot.renttracker.model.rentterms.RentTermsEditArgs
import net.neednot.renttracker.model.rentterms.RentTermsPreviewProposed
import net.neednot.renttracker.model.rentterms.RentTermsPreviewResult
import net.neednot.renttracker.model.tenants.TenantsAddArgs
import net.neednot.renttracker.model.tenants.TenantsEditArgs

internal fun TenantDetails.toAddArgs(
    groupId: String,
    firstRentTerm: FirstRentTerm,
): TenantsAddArgs =
    TenantsAddArgs(
        groupId = groupId,
        name = name,
        contactName = contactName,
        contactPhone = contactPhone,
        contactEmail = contactEmail,
        notes = notes,
        depositCents = depositCents,
        moveIn = moveIn.toString(),
        moveOut = moveOut?.toString(),
        graceDaysOverride = graceDaysOverride?.toLong(),
        lateFeeFlatOverrideCents = lateFeeFlatOverrideCents,
        amountCents = firstRentTerm.amountCents,
        dueDay = firstRentTerm.dueDay.toLong(),
    )

internal fun TenantDetails.toEditArgs(
    tenantId: String,
    expectedVersion: Long,
): TenantsEditArgs =
    TenantsEditArgs(
        tenantId = tenantId,
        expectedVersion = expectedVersion,
        name = name,
        contactName = contactName,
        contactPhone = contactPhone,
        contactEmail = contactEmail,
        notes = notes,
        depositCents = depositCents,
        moveIn = moveIn.toString(),
        moveOut = moveOut?.toString(),
        graceDaysOverride = graceDaysOverride?.toLong(),
        lateFeeFlatOverrideCents = lateFeeFlatOverrideCents,
    )

internal fun RentTermDraft.toAddArgs(tenantId: String): RentTermsAddArgs =
    RentTermsAddArgs(
        tenantId = tenantId,
        startMonth = startMonth.toString(),
        amountCents = amountCents,
        dueDay = dueDay.toLong(),
    )

internal fun RentTermDraft.toEditArgs(
    rentTermId: String,
    expectedVersion: Long,
): RentTermsEditArgs =
    RentTermsEditArgs(
        rentTermId = rentTermId,
        expectedVersion = expectedVersion,
        startMonth = startMonth.toString(),
        amountCents = amountCents,
        dueDay = dueDay.toLong(),
    )

internal fun RentTermDraft.toProposed(rentTermId: String?): RentTermsPreviewProposed =
    RentTermsPreviewProposed(
        rentTermId = rentTermId,
        startMonth = startMonth.toString(),
        amountCents = amountCents,
        dueDay = dueDay.toLong(),
    )

internal fun RentTermsPreviewResult.toRentTermPreview(): RentTermPreview =
    RentTermPreview(
        before =
            BalanceTotal(
                owedCents = before.rentOwedCents + before.lateFeesOwedCents + before.otherOwedCents,
                creditCents = before.creditCents,
            ),
        after =
            BalanceTotal(
                owedCents = after.rentOwedCents + after.lateFeesOwedCents + after.otherOwedCents,
                creditCents = after.creditCents,
            ),
    )
