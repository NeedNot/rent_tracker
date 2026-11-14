package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.designsystem.format.formatDollarsInput
import net.neednot.renttracker.core.designsystem.format.parseDollarsToCents
import net.neednot.renttracker.feature.tenants.domain.FirstRentTerm
import net.neednot.renttracker.feature.tenants.domain.TenantDetails
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantEditorField
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantEditorForm

internal fun TenantDetails.toForm(): TenantEditorForm =
    TenantEditorForm(
        name = name,
        contactName = contactName.orEmpty(),
        phone = contactPhone.orEmpty(),
        email = contactEmail.orEmpty(),
        notes = notes.orEmpty(),
        securityDeposit = depositCents?.let(::formatDollarsInput).orEmpty(),
        moveIn = moveIn,
        lastDayInUnit = moveOut,
        graceDays = graceDaysOverride?.toString().orEmpty(),
        lateFee = lateFeeFlatOverrideCents?.let(::formatDollarsInput).orEmpty(),
    )

internal fun TenantEditorForm.with(
    field: TenantEditorField,
    text: String,
): TenantEditorForm =
    when (field) {
        TenantEditorField.Name -> copy(name = text)
        TenantEditorField.ContactName -> copy(contactName = text)
        TenantEditorField.Phone -> copy(phone = text)
        TenantEditorField.Email -> copy(email = text)
        TenantEditorField.Notes -> copy(notes = text)
        TenantEditorField.SecurityDeposit -> copy(securityDeposit = text)
        TenantEditorField.GraceDays -> copy(graceDays = text)
        TenantEditorField.LateFee -> copy(lateFee = text)
        TenantEditorField.MonthlyRent -> copy(monthlyRent = text)
        TenantEditorField.DueDay -> copy(dueDay = text)
        TenantEditorField.MoveIn, TenantEditorField.LastDayInUnit -> this
    }

internal fun TenantEditorForm.details(moveIn: LocalDate): TenantDetails =
    TenantDetails(
        name = name.trim(),
        contactName = contactName.trimmedOrNull(),
        contactPhone = phone.trimmedOrNull(),
        contactEmail = email.trimmedOrNull(),
        notes = notes.trimmedOrNull(),
        depositCents = parseDollarsToCents(securityDeposit),
        moveIn = moveIn,
        moveOut = lastDayInUnit,
        graceDaysOverride = graceDays.trim().toIntOrNull(),
        lateFeeFlatOverrideCents = parseDollarsToCents(lateFee),
    )

internal fun TenantEditorForm.firstRentTerm(): FirstRentTerm? {
    val amountCents = parseDollarsToCents(monthlyRent)
    val dueDay = dueDay.trim().toIntOrNull()
    return if (amountCents != null && dueDay != null) FirstRentTerm(amountCents, dueDay) else null
}

private fun String.trimmedOrNull(): String? = trim().ifEmpty { null }
