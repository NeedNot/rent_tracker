package net.neednot.renttracker.feature.group.presentation.viewmodel

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import net.neednot.renttracker.core.designsystem.format.parseDollarsToCents
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.validation.InputLimits
import net.neednot.renttracker.core.validation.amountError
import net.neednot.renttracker.core.validation.lengthError
import net.neednot.renttracker.core.validation.wholeNumberError
import net.neednot.renttracker.feature.group.domain.GroupSettings
import net.neednot.renttracker.feature.group.presentation.uistate.GroupSettingsField
import net.neednot.renttracker.feature.group.presentation.uistate.GroupSettingsForm
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.group_settings_error_grace_days
import net.neednot.renttracker.resources.group_settings_error_late_fee
import net.neednot.renttracker.resources.group_settings_error_name

private val SERVER_FIELDS =
    mapOf(
        "Group name" to GroupSettingsField.Name,
        "Grace period" to GroupSettingsField.GraceDays,
        "Late fee" to GroupSettingsField.LateFee,
    )

internal fun GroupSettingsForm.validate(): Result<GroupSettings, Map<GroupSettingsField, UiText>> {
    val errors = fieldErrors(includesMissing = true)
    return if (errors.isEmpty()) Ok(toSettings()) else Err(errors)
}

internal fun GroupSettingsForm.fieldErrors(includesMissing: Boolean): Map<GroupSettingsField, UiText> =
    GroupSettingsField.entries
        .mapNotNull { field ->
            val error = inputError(field) ?: if (includesMissing) missingError(field) else null
            error?.let { field to it }
        }.toMap()

internal fun groupSettingsFieldOf(serverField: String?): GroupSettingsField? = SERVER_FIELDS[serverField]

private fun GroupSettingsForm.inputError(field: GroupSettingsField): UiText? =
    when (field) {
        GroupSettingsField.Name -> {
            lengthError(name, InputLimits.MAX_NAME_LENGTH)
        }

        GroupSettingsField.GraceDays -> {
            wholeNumberError(graceDays, InputLimits.GRACE_DAYS, Res.string.group_settings_error_grace_days)
        }

        GroupSettingsField.LateFee -> {
            lateFeeError()
        }
    }

private fun GroupSettingsForm.lateFeeError(): UiText? =
    if (parseDollarsToCents(lateFee) == 0L) {
        UiText.Resource(Res.string.group_settings_error_late_fee)
    } else {
        amountError(lateFee, allowsZero = false)
    }

private fun GroupSettingsForm.missingError(field: GroupSettingsField): UiText? =
    when {
        field == GroupSettingsField.Name && name.isBlank() -> {
            UiText.Resource(Res.string.group_settings_error_name)
        }

        field == GroupSettingsField.GraceDays && graceDays.isBlank() -> {
            UiText.Resource(Res.string.group_settings_error_grace_days)
        }

        else -> {
            null
        }
    }
