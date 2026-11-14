package net.neednot.renttracker.feature.debts.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.data.DebtKind
import net.neednot.renttracker.core.designsystem.components.DateField
import net.neednot.renttracker.core.designsystem.components.FormTextField
import net.neednot.renttracker.core.designsystem.components.InfoNotice
import net.neednot.renttracker.core.designsystem.components.MoneyField
import net.neednot.renttracker.core.designsystem.components.MonthField
import net.neednot.renttracker.core.designsystem.components.WarningNotice
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.debts.presentation.components.ChargeEditorTopBar
import net.neednot.renttracker.feature.debts.presentation.components.ChargeHeaderCard
import net.neednot.renttracker.feature.debts.presentation.components.ChargeKindToggle
import net.neednot.renttracker.feature.debts.presentation.components.FixedChargeKind
import net.neednot.renttracker.feature.debts.presentation.components.LateFeeDuplicateDialog
import net.neednot.renttracker.feature.debts.presentation.uistate.ChargeEditorField
import net.neednot.renttracker.feature.debts.presentation.uistate.ChargeEditorUiState
import net.neednot.renttracker.feature.debts.presentation.uistate.ChargeHeaderUiState
import net.neednot.renttracker.feature.debts.presentation.viewmodel.ChargeEditorEvent
import net.neednot.renttracker.feature.debts.presentation.viewmodel.ChargeEditorTarget
import net.neednot.renttracker.feature.debts.presentation.viewmodel.ChargeEditorViewModel
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.charge_editor_amount
import net.neednot.renttracker.resources.charge_editor_amount_suggested
import net.neednot.renttracker.resources.charge_editor_date
import net.neednot.renttracker.resources.charge_editor_date_hint_add
import net.neednot.renttracker.resources.charge_editor_date_hint_edit
import net.neednot.renttracker.resources.charge_editor_for_month
import net.neednot.renttracker.resources.charge_editor_for_month_hint
import net.neednot.renttracker.resources.charge_editor_loading
import net.neednot.renttracker.resources.charge_editor_note_custom
import net.neednot.renttracker.resources.charge_editor_note_custom_hint
import net.neednot.renttracker.resources.charge_editor_note_optional
import net.neednot.renttracker.resources.charge_editor_suggestion_notice
import net.neednot.renttracker.resources.charge_editor_title_add
import net.neednot.renttracker.resources.charge_editor_title_edit
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val MAX_NOTE_LENGTH = 100

@Composable
fun ChargeEditorScreen(
    target: ChargeEditorTarget,
    onClosed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChargeEditorViewModel = koinViewModel { parametersOf(target) },
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ChargeEditorEvent.ChargeSaved -> onClosed()
            }
        }
    }
    ChargeEditorContent(
        uiState = uiState,
        onKindSelected = viewModel::onKindSelected,
        onFieldChanged = viewModel::onFieldChanged,
        onDateChanged = viewModel::onDateChanged,
        onForMonthChanged = viewModel::onForMonthChanged,
        onSaveClicked = viewModel::onSaveClicked,
        onDuplicateConfirmed = viewModel::onDuplicateConfirmed,
        onDuplicateDismissed = viewModel::onDuplicateDismissed,
        onCloseClicked = onClosed,
        modifier = modifier,
    )
}

@Composable
private fun ChargeEditorContent(
    uiState: ChargeEditorUiState,
    onKindSelected: (DebtKind) -> Unit,
    onFieldChanged: (ChargeEditorField, String) -> Unit,
    onDateChanged: (LocalDate) -> Unit,
    onForMonthChanged: (YearMonth) -> Unit,
    onSaveClicked: () -> Unit,
    onDuplicateConfirmed: () -> Unit,
    onDuplicateDismissed: () -> Unit,
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = if (uiState.isNewCharge) Res.string.charge_editor_title_add else Res.string.charge_editor_title_edit
    Scaffold(
        modifier = modifier,
        topBar = {
            ChargeEditorTopBar(
                title = stringResource(title),
                isSaving = uiState.isSaving,
                isSaveEnabled = !uiState.isSaving && !uiState.isLoading,
                onSaveClicked = onSaveClicked,
                onCloseClicked = onCloseClicked,
            )
        },
    ) { contentPadding ->
        if (uiState.isLoading) {
            val description = stringResource(Res.string.charge_editor_loading)
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ChargeFields(
                    uiState = uiState,
                    onKindSelected = onKindSelected,
                    onFieldChanged = onFieldChanged,
                    onDateChanged = onDateChanged,
                    onForMonthChanged = onForMonthChanged,
                )
            }
        }
    }
    uiState.duplicate?.let { dialog ->
        LateFeeDuplicateDialog(dialog = dialog, onConfirmed = onDuplicateConfirmed, onDismissed = onDuplicateDismissed)
    }
}

@Composable
private fun ChargeFields(
    uiState: ChargeEditorUiState,
    onKindSelected: (DebtKind) -> Unit,
    onFieldChanged: (ChargeEditorField, String) -> Unit,
    onDateChanged: (LocalDate) -> Unit,
    onForMonthChanged: (YearMonth) -> Unit,
) {
    val isEnabled = !uiState.isSaving
    uiState.conflictBanner?.let { banner -> WarningNotice(banner.asString()) }
    uiState.header?.let { header -> ChargeHeaderCard(header) }
    if (uiState.isFromSuggestion) InfoNotice(stringResource(Res.string.charge_editor_suggestion_notice))
    if (uiState.isNewCharge) {
        ChargeKindToggle(selected = uiState.kind, onKindSelected = onKindSelected, enabled = isEnabled)
    } else {
        FixedChargeKind(uiState.kind)
    }
    MoneyField(
        label = stringResource(Res.string.charge_editor_amount),
        text = uiState.amount,
        onTextChanged = { onFieldChanged(ChargeEditorField.Amount, it) },
        error = uiState.fieldErrors[ChargeEditorField.Amount],
        hint = if (uiState.isFromSuggestion) stringResource(Res.string.charge_editor_amount_suggested) else null,
        enabled = isEnabled,
    )
    val dateHint =
        if (uiState.isNewCharge) Res.string.charge_editor_date_hint_add else Res.string.charge_editor_date_hint_edit
    DateField(
        label = stringResource(Res.string.charge_editor_date),
        date = uiState.date,
        onDateChanged = onDateChanged,
        hint = stringResource(dateHint),
        enabled = isEnabled,
    )
    if (uiState.kind == DebtKind.LateFee) {
        uiState.forMonth?.let { month ->
            MonthField(
                label = stringResource(Res.string.charge_editor_for_month),
                month = month,
                onMonthChanged = onForMonthChanged,
                enabled = isEnabled,
            )
            Text(
                text = stringResource(Res.string.charge_editor_for_month_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    NoteField(uiState = uiState, onFieldChanged = onFieldChanged, enabled = isEnabled)
    uiState.error?.let { error ->
        Text(
            text = error.asString(),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun NoteField(
    uiState: ChargeEditorUiState,
    onFieldChanged: (ChargeEditorField, String) -> Unit,
    enabled: Boolean,
) {
    val isCustom = uiState.kind == DebtKind.Custom
    val label = if (isCustom) Res.string.charge_editor_note_custom else Res.string.charge_editor_note_optional
    FormTextField(
        label = stringResource(label),
        text = uiState.note,
        onTextChanged = { onFieldChanged(ChargeEditorField.Note, it) },
        error = uiState.fieldErrors[ChargeEditorField.Note],
        hint =
            if (isCustom) {
                stringResource(Res.string.charge_editor_note_custom_hint, uiState.note.length, MAX_NOTE_LENGTH)
            } else {
                null
            },
        enabled = enabled,
    )
}

@Preview
@Composable
private fun ChargeEditorContentPreview() {
    RentTrackerTheme {
        ChargeEditorContent(
            uiState =
                ChargeEditorUiState(
                    isNewCharge = true,
                    isLoading = false,
                    header =
                        ChargeHeaderUiState(
                            tenantName = "Apt 3B",
                            groupName = "1st Street",
                            balance = UiText.Plain("Owes $550.00"),
                        ),
                    today = LocalDate(2026, 3, 10),
                    amount = "50.00",
                    date = LocalDate(2026, 3, 10),
                    forMonth = YearMonth(2026, 3),
                    isFromSuggestion = true,
                ),
            onKindSelected = {},
            onFieldChanged = { _, _ -> },
            onDateChanged = {},
            onForMonthChanged = {},
            onSaveClicked = {},
            onDuplicateConfirmed = {},
            onDuplicateDismissed = {},
            onCloseClicked = {},
        )
    }
}
