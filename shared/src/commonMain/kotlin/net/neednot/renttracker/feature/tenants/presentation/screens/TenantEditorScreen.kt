package net.neednot.renttracker.feature.tenants.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.designsystem.components.DateField
import net.neednot.renttracker.core.designsystem.components.EditorTopBar
import net.neednot.renttracker.core.designsystem.components.FormTextField
import net.neednot.renttracker.core.designsystem.components.MoneyField
import net.neednot.renttracker.core.designsystem.components.WholeNumberField
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.navigation.TenantEditorRoute
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantEditorField
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantEditorForm
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantEditorUiState
import net.neednot.renttracker.feature.tenants.presentation.viewmodel.TenantEditorEvent
import net.neednot.renttracker.feature.tenants.presentation.viewmodel.TenantEditorViewModel
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.tenant_editor_contact_name
import net.neednot.renttracker.resources.tenant_editor_deposit
import net.neednot.renttracker.resources.tenant_editor_due_day
import net.neednot.renttracker.resources.tenant_editor_email
import net.neednot.renttracker.resources.tenant_editor_grace_days
import net.neednot.renttracker.resources.tenant_editor_grace_days_hint
import net.neednot.renttracker.resources.tenant_editor_last_day
import net.neednot.renttracker.resources.tenant_editor_last_day_hint
import net.neednot.renttracker.resources.tenant_editor_late_fee
import net.neednot.renttracker.resources.tenant_editor_late_fee_hint
import net.neednot.renttracker.resources.tenant_editor_loading
import net.neednot.renttracker.resources.tenant_editor_monthly_rent
import net.neednot.renttracker.resources.tenant_editor_move_in
import net.neednot.renttracker.resources.tenant_editor_name
import net.neednot.renttracker.resources.tenant_editor_notes
import net.neednot.renttracker.resources.tenant_editor_phone
import net.neednot.renttracker.resources.tenant_editor_title_add
import net.neednot.renttracker.resources.tenant_editor_title_edit
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun TenantEditorScreen(
    route: TenantEditorRoute,
    onTenantAdded: (tenantId: String) -> Unit,
    onClosed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TenantEditorViewModel = koinViewModel { parametersOf(route) },
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TenantEditorEvent.TenantAdded -> onTenantAdded(event.tenantId)
                TenantEditorEvent.TenantSaved -> onClosed()
            }
        }
    }
    TenantEditorContent(
        uiState = uiState,
        onFieldChanged = viewModel::onFieldChanged,
        onMoveInChanged = viewModel::onMoveInChanged,
        onLastDayInUnitChanged = viewModel::onLastDayInUnitChanged,
        onSaveClicked = viewModel::onSaveClicked,
        onCloseClicked = onClosed,
        modifier = modifier,
    )
}

@Composable
private fun TenantEditorContent(
    uiState: TenantEditorUiState,
    onFieldChanged: (TenantEditorField, String) -> Unit,
    onMoveInChanged: (LocalDate) -> Unit,
    onLastDayInUnitChanged: (LocalDate?) -> Unit,
    onSaveClicked: () -> Unit,
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = if (uiState.isNewTenant) Res.string.tenant_editor_title_add else Res.string.tenant_editor_title_edit
    Scaffold(
        modifier = modifier,
        topBar = {
            EditorTopBar(
                title = stringResource(title),
                isSaveEnabled = !uiState.isSaving && !uiState.isLoading,
                onSaveClicked = onSaveClicked,
                onCloseClicked = onCloseClicked,
            )
        },
    ) { contentPadding ->
        if (uiState.isLoading) {
            val description = stringResource(Res.string.tenant_editor_loading)
            Box(modifier = Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = description })
            }
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                uiState.error?.let { error ->
                    Text(
                        text = error.asString(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                TenantFields(
                    uiState = uiState,
                    onFieldChanged = onFieldChanged,
                    onMoveInChanged = onMoveInChanged,
                    onLastDayInUnitChanged = onLastDayInUnitChanged,
                )
            }
        }
    }
}

@Composable
private fun TenantFields(
    uiState: TenantEditorUiState,
    onFieldChanged: (TenantEditorField, String) -> Unit,
    onMoveInChanged: (LocalDate) -> Unit,
    onLastDayInUnitChanged: (LocalDate?) -> Unit,
) {
    val form = uiState.form
    val isEnabled = !uiState.isSaving
    val words = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
    FormTextField(
        label = stringResource(Res.string.tenant_editor_name),
        text = form.name,
        onTextChanged = { onFieldChanged(TenantEditorField.Name, it) },
        error = uiState.fieldErrors[TenantEditorField.Name],
        enabled = isEnabled,
        keyboardOptions = words,
    )
    if (uiState.isNewTenant) {
        MoneyField(
            label = stringResource(Res.string.tenant_editor_monthly_rent),
            text = form.monthlyRent,
            onTextChanged = { onFieldChanged(TenantEditorField.MonthlyRent, it) },
            error = uiState.fieldErrors[TenantEditorField.MonthlyRent],
            enabled = isEnabled,
        )
        WholeNumberField(
            label = stringResource(Res.string.tenant_editor_due_day),
            text = form.dueDay,
            onTextChanged = { onFieldChanged(TenantEditorField.DueDay, it) },
            error = uiState.fieldErrors[TenantEditorField.DueDay],
            enabled = isEnabled,
        )
    }
    DateField(
        label = stringResource(Res.string.tenant_editor_move_in),
        date = form.moveIn,
        onDateChanged = onMoveInChanged,
        error = uiState.fieldErrors[TenantEditorField.MoveIn],
        enabled = isEnabled,
    )
    DateField(
        label = stringResource(Res.string.tenant_editor_last_day),
        date = form.lastDayInUnit,
        onDateChanged = onLastDayInUnitChanged,
        error = uiState.fieldErrors[TenantEditorField.LastDayInUnit],
        hint = stringResource(Res.string.tenant_editor_last_day_hint),
        enabled = isEnabled,
        onCleared = { onLastDayInUnitChanged(null) },
    )
    FormTextField(
        label = stringResource(Res.string.tenant_editor_contact_name),
        text = form.contactName,
        onTextChanged = { onFieldChanged(TenantEditorField.ContactName, it) },
        error = uiState.fieldErrors[TenantEditorField.ContactName],
        enabled = isEnabled,
        keyboardOptions = words,
    )
    FormTextField(
        label = stringResource(Res.string.tenant_editor_phone),
        text = form.phone,
        onTextChanged = { onFieldChanged(TenantEditorField.Phone, it) },
        error = uiState.fieldErrors[TenantEditorField.Phone],
        enabled = isEnabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
    )
    FormTextField(
        label = stringResource(Res.string.tenant_editor_email),
        text = form.email,
        onTextChanged = { onFieldChanged(TenantEditorField.Email, it) },
        error = uiState.fieldErrors[TenantEditorField.Email],
        enabled = isEnabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
    )
    MoneyField(
        label = stringResource(Res.string.tenant_editor_deposit),
        text = form.securityDeposit,
        onTextChanged = { onFieldChanged(TenantEditorField.SecurityDeposit, it) },
        error = uiState.fieldErrors[TenantEditorField.SecurityDeposit],
        enabled = isEnabled,
    )
    WholeNumberField(
        label = stringResource(Res.string.tenant_editor_grace_days),
        text = form.graceDays,
        onTextChanged = { onFieldChanged(TenantEditorField.GraceDays, it) },
        error = uiState.fieldErrors[TenantEditorField.GraceDays],
        hint = stringResource(Res.string.tenant_editor_grace_days_hint),
        enabled = isEnabled,
    )
    MoneyField(
        label = stringResource(Res.string.tenant_editor_late_fee),
        text = form.lateFee,
        onTextChanged = { onFieldChanged(TenantEditorField.LateFee, it) },
        error = uiState.fieldErrors[TenantEditorField.LateFee],
        hint = stringResource(Res.string.tenant_editor_late_fee_hint),
        enabled = isEnabled,
    )
    FormTextField(
        label = stringResource(Res.string.tenant_editor_notes),
        text = form.notes,
        onTextChanged = { onFieldChanged(TenantEditorField.Notes, it) },
        error = uiState.fieldErrors[TenantEditorField.Notes],
        enabled = isEnabled,
        singleLine = false,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
    )
}

@Preview
@Composable
private fun TenantEditorContentPreview() {
    RentTrackerTheme {
        TenantEditorContent(
            uiState =
                TenantEditorUiState(
                    isNewTenant = true,
                    form = TenantEditorForm(name = "Apt 3B", monthlyRent = "1,500.00", dueDay = "1"),
                ),
            onFieldChanged = { _, _ -> },
            onMoveInChanged = {},
            onLastDayInUnitChanged = {},
            onSaveClicked = {},
            onCloseClicked = {},
        )
    }
}
