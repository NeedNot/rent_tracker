package net.neednot.renttracker.feature.members.presentation.viewmodel

import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.validation.InputLimits
import net.neednot.renttracker.core.validation.lengthError
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.members_invite_error_email_format
import net.neednot.renttracker.resources.members_invite_error_email_required

// Mirrors convex/inviteEmailAddress.ts.
private val EMAIL_SHAPE = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

internal const val EMAIL_FIELD = "email"

internal fun inviteEmailError(
    email: String,
    includesMissing: Boolean,
): UiText? {
    val trimmed = email.trim()
    return when {
        trimmed.isEmpty() -> {
            if (includesMissing) {
                UiText.Resource(
                    Res.string.members_invite_error_email_required,
                )
            } else {
                null
            }
        }

        trimmed.length > InputLimits.MAX_NAME_LENGTH -> {
            lengthError(trimmed, InputLimits.MAX_NAME_LENGTH)
        }

        !EMAIL_SHAPE.matches(trimmed) -> {
            UiText.Resource(Res.string.members_invite_error_email_format)
        }

        else -> {
            null
        }
    }
}
