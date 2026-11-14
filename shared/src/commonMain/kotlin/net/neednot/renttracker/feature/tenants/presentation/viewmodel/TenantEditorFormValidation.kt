package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.validation.InputLimits
import net.neednot.renttracker.core.validation.amountError
import net.neednot.renttracker.core.validation.lengthError
import net.neednot.renttracker.core.validation.wholeNumberError
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantEditorField
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantEditorForm
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.input_error_amount_format
import net.neednot.renttracker.resources.tenant_editor_error_due_day
import net.neednot.renttracker.resources.tenant_editor_error_grace_days
import net.neednot.renttracker.resources.tenant_editor_error_last_day_before_move_in
import net.neednot.renttracker.resources.tenant_editor_error_move_in_required
import net.neednot.renttracker.resources.tenant_editor_error_name_required

private val FIRST_RENT_TERM_FIELDS = setOf(TenantEditorField.MonthlyRent, TenantEditorField.DueDay)

private val SERVER_FIELDS =
    mapOf(
        "Name" to TenantEditorField.Name,
        "Contact name" to TenantEditorField.ContactName,
        "Phone" to TenantEditorField.Phone,
        "Email" to TenantEditorField.Email,
        "Notes" to TenantEditorField.Notes,
        "Security deposit" to TenantEditorField.SecurityDeposit,
        "Move-in date" to TenantEditorField.MoveIn,
        "Last day in unit" to TenantEditorField.LastDayInUnit,
        "Grace period" to TenantEditorField.GraceDays,
        "Late fee amount" to TenantEditorField.LateFee,
        "Rent" to TenantEditorField.MonthlyRent,
        "Due day" to TenantEditorField.DueDay,
    )

internal fun TenantEditorForm.validate(isNewTenant: Boolean): Result<ValidTenantForm, Map<TenantEditorField, UiText>> {
    val errors = fieldErrors(isNewTenant, includesMissing = true)
    val moveIn = moveIn
    return if (errors.isEmpty() && moveIn != null) {
        Ok(ValidTenantForm(details = details(moveIn), firstRentTerm = if (isNewTenant) firstRentTerm() else null))
    } else {
        Err(errors)
    }
}

internal fun TenantEditorForm.fieldErrors(
    isNewTenant: Boolean,
    includesMissing: Boolean,
): Map<TenantEditorField, UiText> =
    TenantEditorField.entries
        .filter { isNewTenant || it !in FIRST_RENT_TERM_FIELDS }
        .mapNotNull { field ->
            val error = inputError(field) ?: if (includesMissing) missingError(field) else null
            error?.let { field to it }
        }.toMap()

internal fun tenantEditorFieldOf(serverField: String?): TenantEditorField? = SERVER_FIELDS[serverField]

private fun TenantEditorForm.inputError(field: TenantEditorField): UiText? =
    when (field) {
        TenantEditorField.Name -> {
            lengthError(name, InputLimits.MAX_NAME_LENGTH)
        }

        TenantEditorField.ContactName -> {
            lengthError(contactName, InputLimits.MAX_NAME_LENGTH)
        }

        TenantEditorField.Phone -> {
            lengthError(phone, InputLimits.MAX_NAME_LENGTH)
        }

        TenantEditorField.Email -> {
            lengthError(email, InputLimits.MAX_NAME_LENGTH)
        }

        TenantEditorField.Notes -> {
            lengthError(notes, InputLimits.MAX_NOTES_LENGTH)
        }

        TenantEditorField.SecurityDeposit -> {
            amountError(securityDeposit, allowsZero = true)
        }

        TenantEditorField.LateFee -> {
            amountError(lateFee, allowsZero = false)
        }

        TenantEditorField.MonthlyRent -> {
            amountError(monthlyRent, allowsZero = false)
        }

        TenantEditorField.GraceDays -> {
            wholeNumberError(graceDays, InputLimits.GRACE_DAYS, Res.string.tenant_editor_error_grace_days)
        }

        TenantEditorField.DueDay -> {
            wholeNumberError(dueDay, InputLimits.DUE_DAYS, Res.string.tenant_editor_error_due_day)
        }

        TenantEditorField.MoveIn -> {
            null
        }

        TenantEditorField.LastDayInUnit -> {
            lastDayInUnitError()
        }
    }

private fun TenantEditorForm.lastDayInUnitError(): UiText? {
    val moveIn = moveIn
    val endsBeforeMoveIn = moveIn != null && lastDayInUnit != null && lastDayInUnit < moveIn
    return if (endsBeforeMoveIn) UiText.Resource(Res.string.tenant_editor_error_last_day_before_move_in) else null
}

private fun TenantEditorForm.missingError(field: TenantEditorField): UiText? =
    when {
        field == TenantEditorField.Name && name.isBlank() -> {
            UiText.Resource(Res.string.tenant_editor_error_name_required)
        }

        field == TenantEditorField.MoveIn && moveIn == null -> {
            UiText.Resource(Res.string.tenant_editor_error_move_in_required)
        }

        field == TenantEditorField.MonthlyRent && monthlyRent.isBlank() -> {
            UiText.Resource(Res.string.input_error_amount_format)
        }

        field == TenantEditorField.DueDay && dueDay.isBlank() -> {
            UiText.Resource(Res.string.tenant_editor_error_due_day)
        }

        else -> {
            null
        }
    }
