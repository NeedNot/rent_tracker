package net.neednot.renttracker.feature.tenants.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.designsystem.components.ActionTone
import net.neednot.renttracker.core.designsystem.components.Banner
import net.neednot.renttracker.core.designsystem.components.BannerKind
import net.neednot.renttracker.core.designsystem.components.ButtonVariant
import net.neednot.renttracker.core.designsystem.components.ContentLayout
import net.neednot.renttracker.core.designsystem.components.DialogButton
import net.neednot.renttracker.core.designsystem.components.EditorTopBar
import net.neednot.renttracker.core.designsystem.components.FormTextField
import net.neednot.renttracker.core.designsystem.components.LoadingIndicator
import net.neednot.renttracker.core.designsystem.components.MoneyField
import net.neednot.renttracker.core.designsystem.components.MonthField
import net.neednot.renttracker.core.designsystem.components.RtButton
import net.neednot.renttracker.core.designsystem.components.RtDialog
import net.neednot.renttracker.core.designsystem.components.WholeNumberField
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.navigation.RentTermEditorRoute
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.tenants.presentation.uistate.PastMonthWarningUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.RentTermDeletionUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.RentTermEditorField
import net.neednot.renttracker.feature.tenants.presentation.uistate.RentTermEditorUiState
import net.neednot.renttracker.feature.tenants.presentation.viewmodel.RentTermEditorEvent
import net.neednot.renttracker.feature.tenants.presentation.viewmodel.RentTermEditorViewModel
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.common_cancel
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
import net.neednot.renttracker.resources.tenant_editor_due_day
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
        actions =
            RentTermEditorActions(
                onStartMonthChanged = viewModel::onStartMonthChanged,
                onFieldChanged = viewModel::onFieldChanged,
                onSaveClicked = viewModel::onSaveClicked,
                onDeleteClicked = viewModel::onDeleteClicked,
                onDeleteConfirmed = viewModel::onDeleteConfirmed,
                onDeleteDismissed = viewModel::onDeleteDismissed,
                onCloseClicked = onClosed,
            ),
        modifier = modifier,
    )
}

@Composable
internal fun RentTermEditorContent(
    uiState: RentTermEditorUiState,
    actions: RentTermEditorActions,
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
                onSaveClicked = actions.onSaveClicked,
                onCloseClicked = actions.onCloseClicked,
                isSaving = uiState.isSaving,
            )
        },
    ) { contentPadding ->
        if (uiState.isLoading) {
            LoadingIndicator(modifier = Modifier.padding(contentPadding))
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(ContentLayout.Form.padding),
                verticalArrangement = Arrangement.spacedBy(ContentLayout.Form.spacing),
            ) {
                RentTermFields(uiState = uiState, actions = actions)
            }
        }
    }
    uiState.deletion?.let { deletion ->
        DeleteRentTermDialog(
            deletion = deletion,
            onReasonChanged = { actions.onFieldChanged(RentTermEditorField.DeleteReason, it) },
            onConfirmed = actions.onDeleteConfirmed,
            onDismissed = actions.onDeleteDismissed,
        )
    }
}

@Composable
private fun RentTermFields(
    uiState: RentTermEditorUiState,
    actions: RentTermEditorActions,
) {
    val isEnabled = !uiState.isSaving
    uiState.error?.let { error -> Banner(text = error.asString(), kind = BannerKind.Error) }
    uiState.startMonth?.let { startMonth ->
        MonthField(
            label = stringResource(Res.string.rent_term_editor_start_month),
            month = startMonth,
            onMonthChanged = actions.onStartMonthChanged,
            error = uiState.fieldErrors[RentTermEditorField.StartMonth],
            enabled = isEnabled,
        )
    }
    uiState.pastMonthWarning?.let { warning -> Banner(text = warning.text(), kind = BannerKind.Warning) }
    MoneyField(
        label = stringResource(Res.string.tenant_editor_monthly_rent),
        text = uiState.amount,
        onTextChanged = { actions.onFieldChanged(RentTermEditorField.Amount, it) },
        error = uiState.fieldErrors[RentTermEditorField.Amount],
        enabled = isEnabled,
    )
    WholeNumberField(
        label = stringResource(Res.string.tenant_editor_due_day),
        text = uiState.dueDay,
        onTextChanged = { actions.onFieldChanged(RentTermEditorField.DueDay, it) },
        error = uiState.fieldErrors[RentTermEditorField.DueDay],
        enabled = isEnabled,
    )
    if (uiState.showsDueDayReminder) {
        Banner(text = stringResource(Res.string.rent_term_editor_due_day_reminder), kind = BannerKind.Note)
    }
    if (uiState.canDelete) {
        RtButton(
            text = stringResource(Res.string.rent_term_editor_delete),
            onClick = actions.onDeleteClicked,
            modifier = Modifier.fillMaxWidth(),
            variant = ButtonVariant.Outlined,
            tone = ActionTone.Destructive,
            enabled = isEnabled,
        )
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
    RtDialog(
        title = stringResource(Res.string.rent_term_editor_delete_title),
        onDismissed = onDismissed,
        confirmButton = {
            DialogButton(
                text = stringResource(Res.string.rent_term_editor_delete_confirm),
                onClick = onConfirmed,
                tone = ActionTone.Destructive,
                isWorking = deletion.isDeleting,
            )
        },
        dismissButton = { DialogButton(text = stringResource(Res.string.common_cancel), onClick = onDismissed) },
    ) {
        Text(stringResource(Res.string.rent_term_editor_delete_message))
        FormTextField(
            label = stringResource(Res.string.rent_term_editor_delete_reason),
            text = deletion.reason,
            onTextChanged = onReasonChanged,
            error = deletion.error,
            enabled = !deletion.isDeleting,
        )
    }
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
            actions = RentTermEditorActions(),
        )
    }
}
