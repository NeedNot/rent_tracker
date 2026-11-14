package net.neednot.renttracker.feature.tenants.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.navigation.RentTermEditorRoute
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.tenants.presentation.components.EditorTopBar
import net.neednot.renttracker.feature.tenants.presentation.components.FormTextField
import net.neednot.renttracker.feature.tenants.presentation.components.InfoNotice
import net.neednot.renttracker.feature.tenants.presentation.components.MoneyField
import net.neednot.renttracker.feature.tenants.presentation.components.MonthField
import net.neednot.renttracker.feature.tenants.presentation.components.WarningNotice
import net.neednot.renttracker.feature.tenants.presentation.components.WholeNumberField
import net.neednot.renttracker.feature.tenants.presentation.uistate.PastMonthWarningUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.RentTermDeletionUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.RentTermEditorField
import net.neednot.renttracker.feature.tenants.presentation.uistate.RentTermEditorUiState
import net.neednot.renttracker.feature.tenants.presentation.viewmodel.RentTermEditorEvent
import net.neednot.renttracker.feature.tenants.presentation.viewmodel.RentTermEditorViewModel
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.rent_term_editor_delete
import net.neednot.renttracker.resources.rent_term_editor_delete_confirm
import net.neednot.renttracker.resources.rent_term_editor_delete_message
import net.neednot.renttracker.resources.rent_term_editor_delete_reason
import net.neednot.renttracker.resources.rent_term_editor_delete_title
import net.neednot.renttracker.resources.rent_term_editor_due_day_reminder
import net.neednot.renttracker.resources.rent_term_editor_past_month_change
import net.neednot.renttracker.resources.rent_term_editor_past_month_check_failed
import net.neednot.renttracker.resources.rent_term_editor_past_month_checking
import net.neednot.renttracker.resources.rent_term_editor_start_month
import net.neednot.renttracker.resources.rent_term_editor_title_add
import net.neednot.renttracker.resources.rent_term_editor_title_edit
import net.neednot.renttracker.resources.tenant_dialog_cancel
import net.neednot.renttracker.resources.tenant_editor_due_day
import net.neednot.renttracker.resources.tenant_editor_loading
import net.neednot.renttracker.resources.tenant_editor_monthly_rent
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun RentTermEditorScreen(
    route: RentTermEditorRoute,
    onClosed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RentTermEditorViewModel = koinViewModel { parametersOf(route) },
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                RentTermEditorEvent.RentTermSaved, RentTermEditorEvent.RentTermDeleted -> onClosed()
            }
        }
    }
    RentTermEditorContent(
        uiState = uiState,
        onStartMonthChanged = viewModel::onStartMonthChanged,
        onFieldChanged = viewModel::onFieldChanged,
        onSaveClicked = viewModel::onSaveClicked,
        onDeleteClicked = viewModel::onDeleteClicked,
        onDeleteConfirmed = viewModel::onDeleteConfirmed,
        onDeleteDismissed = viewModel::onDeleteDismissed,
        onCloseClicked = onClosed,
        modifier = modifier,
    )
}

@Composable
private fun RentTermEditorContent(
    uiState: RentTermEditorUiState,
    onStartMonthChanged: (YearMonth) -> Unit,
    onFieldChanged: (RentTermEditorField, String) -> Unit,
    onSaveClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    onDeleteDismissed: () -> Unit,
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title =
        if (uiState.isNewTerm) Res.string.rent_term_editor_title_add else Res.string.rent_term_editor_title_edit
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
                RentTermFields(
                    uiState = uiState,
                    onStartMonthChanged = onStartMonthChanged,
                    onFieldChanged = onFieldChanged,
                    onDeleteClicked = onDeleteClicked,
                )
            }
        }
    }
    uiState.deletion?.let { deletion ->
        DeleteRentTermDialog(
            deletion = deletion,
            onReasonChanged = { onFieldChanged(RentTermEditorField.DeleteReason, it) },
            onConfirmed = onDeleteConfirmed,
            onDismissed = onDeleteDismissed,
        )
    }
}

@Composable
private fun RentTermFields(
    uiState: RentTermEditorUiState,
    onStartMonthChanged: (YearMonth) -> Unit,
    onFieldChanged: (RentTermEditorField, String) -> Unit,
    onDeleteClicked: () -> Unit,
) {
    val isEnabled = !uiState.isSaving
    uiState.error?.let { error ->
        Text(
            text = error.asString(),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    uiState.startMonth?.let { startMonth ->
        MonthField(
            label = stringResource(Res.string.rent_term_editor_start_month),
            month = startMonth,
            onMonthChanged = onStartMonthChanged,
            error = uiState.fieldErrors[RentTermEditorField.StartMonth],
            enabled = isEnabled,
        )
    }
    uiState.pastMonthWarning?.let { warning -> WarningNotice(warning.text()) }
    MoneyField(
        label = stringResource(Res.string.tenant_editor_monthly_rent),
        text = uiState.amount,
        onTextChanged = { onFieldChanged(RentTermEditorField.Amount, it) },
        error = uiState.fieldErrors[RentTermEditorField.Amount],
        enabled = isEnabled,
    )
    WholeNumberField(
        label = stringResource(Res.string.tenant_editor_due_day),
        text = uiState.dueDay,
        onTextChanged = { onFieldChanged(RentTermEditorField.DueDay, it) },
        error = uiState.fieldErrors[RentTermEditorField.DueDay],
        enabled = isEnabled,
    )
    if (uiState.showsDueDayReminder) InfoNotice(stringResource(Res.string.rent_term_editor_due_day_reminder))
    if (uiState.canDelete) {
        TextButton(
            onClick = onDeleteClicked,
            modifier = Modifier.heightIn(min = 48.dp),
            enabled = isEnabled,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Text(stringResource(Res.string.rent_term_editor_delete))
        }
    }
}

@Composable
private fun PastMonthWarningUiState.text(): String =
    when (this) {
        PastMonthWarningUiState.CheckingBalance -> {
            stringResource(Res.string.rent_term_editor_past_month_checking)
        }

        is PastMonthWarningUiState.BalanceChange -> {
            stringResource(Res.string.rent_term_editor_past_month_change, before.asString(), after.asString())
        }

        is PastMonthWarningUiState.CheckFailed -> {
            stringResource(Res.string.rent_term_editor_past_month_check_failed, message.asString())
        }
    }

@Composable
private fun DeleteRentTermDialog(
    deletion: RentTermDeletionUiState,
    onReasonChanged: (String) -> Unit,
    onConfirmed: () -> Unit,
    onDismissed: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissed,
        confirmButton = {
            TextButton(onClick = onConfirmed, enabled = !deletion.isDeleting) {
                Text(stringResource(Res.string.rent_term_editor_delete_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissed) { Text(stringResource(Res.string.tenant_dialog_cancel)) }
        },
        title = { Text(stringResource(Res.string.rent_term_editor_delete_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.rent_term_editor_delete_message))
                FormTextField(
                    label = stringResource(Res.string.rent_term_editor_delete_reason),
                    text = deletion.reason,
                    onTextChanged = onReasonChanged,
                    error = deletion.error,
                    enabled = !deletion.isDeleting,
                )
            }
        },
    )
}

@Preview
@Composable
private fun RentTermEditorContentPreview() {
    RentTrackerTheme {
        RentTermEditorContent(
            uiState =
                RentTermEditorUiState(
                    isNewTerm = false,
                    isLoading = false,
                    startMonth = YearMonth(2026, 1),
                    amount = "1,500.00",
                    dueDay = "15",
                    pastMonthWarning =
                        PastMonthWarningUiState.BalanceChange(
                            before = UiText.Plain("Owes $550.00"),
                            after = UiText.Plain("Owes $650.00"),
                        ),
                    showsDueDayReminder = true,
                    canDelete = true,
                ),
            onStartMonthChanged = {},
            onFieldChanged = { _, _ -> },
            onSaveClicked = {},
            onDeleteClicked = {},
            onDeleteConfirmed = {},
            onDeleteDismissed = {},
            onCloseClicked = {},
        )
    }
}
