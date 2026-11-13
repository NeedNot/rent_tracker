package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.designsystem.format.formatDollarsInput
import net.neednot.renttracker.core.designsystem.format.parseDollarsToCents
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.domain.FirstRentTerm
import net.neednot.renttracker.feature.tenants.domain.TenantDetails
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantEditorField
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantEditorForm
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.tenant_editor_error_due_day_format
import net.neednot.renttracker.resources.tenant_editor_error_move_in_required
import net.neednot.renttracker.resources.tenant_editor_error_name_required
import net.neednot.renttracker.resources.tenant_editor_error_whole_days

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
        TenantEditorField.MoveIn -> this
    }

internal fun TenantEditorForm.validate(isNewTenant: Boolean): Result<ValidTenantForm, Map<TenantEditorField, UiText>> {
    val errors = if (isNewTenant) detailErrors() + firstRentTermErrors() else detailErrors()
    val moveIn = moveIn
    return if (errors.isEmpty() && moveIn != null) {
        Ok(ValidTenantForm(details = details(moveIn), firstRentTerm = if (isNewTenant) firstRentTerm() else null))
    } else {
        Err(errors)
    }
}

private fun TenantEditorForm.detailErrors(): Map<TenantEditorField, UiText> =
    buildMap {
        if (name.isBlank()) put(TenantEditorField.Name, UiText.Resource(Res.string.tenant_editor_error_name_required))
        if (moveIn == null) {
            put(TenantEditorField.MoveIn, UiText.Resource(Res.string.tenant_editor_error_move_in_required))
        }
        if (securityDeposit.isNotBlank()) {
            amountFormatError(securityDeposit)?.let { put(TenantEditorField.SecurityDeposit, it) }
        }
        if (lateFee.isNotBlank()) positiveAmountFormatError(lateFee)?.let { put(TenantEditorField.LateFee, it) }
        if (graceDays.isNotBlank() && graceDays.trim().toIntOrNull() == null) {
            put(TenantEditorField.GraceDays, UiText.Resource(Res.string.tenant_editor_error_whole_days))
        }
    }

private fun TenantEditorForm.firstRentTermErrors(): Map<TenantEditorField, UiText> =
    buildMap {
        positiveAmountFormatError(monthlyRent)?.let { put(TenantEditorField.MonthlyRent, it) }
        if (dueDay.trim().toIntOrNull() == null) {
            put(TenantEditorField.DueDay, UiText.Resource(Res.string.tenant_editor_error_due_day_format))
        }
    }

private fun TenantEditorForm.details(moveIn: LocalDate): TenantDetails =
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

private fun TenantEditorForm.firstRentTerm(): FirstRentTerm? {
    val amountCents = parseDollarsToCents(monthlyRent)
    val dueDay = dueDay.trim().toIntOrNull()
    return if (amountCents != null && dueDay != null) FirstRentTerm(amountCents, dueDay) else null
}

private fun String.trimmedOrNull(): String? = trim().ifEmpty { null }
