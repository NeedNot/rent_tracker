package net.neednot.renttracker.feature.tenants.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class TenantEditorUiState(
    val isNewTenant: Boolean,
    val isLoading: Boolean = false,
    val form: TenantEditorForm = TenantEditorForm(),
    val fieldErrors: Map<TenantEditorField, UiText> = emptyMap(),
    val error: UiText? = null,
    val isSaving: Boolean = false,
)
