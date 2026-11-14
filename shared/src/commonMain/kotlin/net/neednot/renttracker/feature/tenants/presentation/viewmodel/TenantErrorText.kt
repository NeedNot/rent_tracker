package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.domain.TenantError
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.tenant_error_not_found
import net.neednot.renttracker.resources.tenant_error_offline
import net.neednot.renttracker.resources.tenant_error_unexpected

internal fun TenantError.text(): UiText =
    when (this) {
        is TenantError.ValidationFailed -> UiText.Plain(message)
        is TenantError.EditConflict -> UiText.Plain(message)
        is TenantError.PermissionDenied -> UiText.Plain(message)
        is TenantError.TenantArchived -> UiText.Plain(message)
        TenantError.NotFound -> UiText.Resource(Res.string.tenant_error_not_found)
        TenantError.Offline -> UiText.Resource(Res.string.tenant_error_offline)
        is TenantError.Unexpected -> UiText.Resource(Res.string.tenant_error_unexpected)
    }
