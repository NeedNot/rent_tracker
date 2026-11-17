package net.neednot.renttracker.feature.debts.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.data.DebtKind
import net.neednot.renttracker.core.designsystem.components.Banner
import net.neednot.renttracker.core.designsystem.components.BannerKind
import net.neednot.renttracker.core.designsystem.components.ContentLayout
import net.neednot.renttracker.core.designsystem.components.DateField
import net.neednot.renttracker.core.designsystem.components.DialogCardLine
import net.neednot.renttracker.core.designsystem.components.DuplicateWarningDialog
import net.neednot.renttracker.core.designsystem.components.EditorTopBar
import net.neednot.renttracker.core.designsystem.components.FormTextField
import net.neednot.renttracker.core.designsystem.components.LoadingIndicator
import net.neednot.renttracker.core.designsystem.components.MoneyField
import net.neednot.renttracker.core.designsystem.components.MonthField
import net.neednot.renttracker.core.designsystem.components.RtSegmentedButton
import net.neednot.renttracker.core.designsystem.components.TenantStrip
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.core.validation.InputLimits
import net.neednot.renttracker.feature.debts.presentation.uistate.ChargeEditorField
import net.neednot.renttracker.feature.debts.presentation.uistate.ChargeEditorUiState
import net.neednot.renttracker.feature.debts.presentation.uistate.ChargeHeaderUiState
import net.neednot.renttracker.feature.debts.presentation.uistate.LateFeeDuplicateUiState
import net.neednot.renttracker.feature.debts.presentation.viewmodel.ChargeEditorEvent
import net.neednot.renttracker.feature.debts.presentation.viewmodel.ChargeEditorTarget
import net.neednot.renttracker.feature.debts.presentation.viewmodel.ChargeEditorViewModel
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.charge_duplicate_add_anyway
import net.neednot.renttracker.resources.charge_duplicate_body_many
import net.neednot.renttracker.resources.charge_duplicate_body_one
import net.neednot.renttracker.resources.charge_duplicate_line
import net.neednot.renttracker.resources.charge_duplicate_title
import net.neednot.renttracker.resources.charge_editor_amount
import net.neednot.renttracker.resources.charge_editor_amount_suggested
import net.neednot.renttracker.resources.charge_editor_conflict_body
import net.neednot.renttracker.resources.charge_editor_date
import net.neednot.renttracker.resources.charge_editor_date_hint_add
import net.neednot.renttracker.resources.charge_editor_date_hint_edit
import net.neednot.renttracker.resources.charge_editor_for_month
import net.neednot.renttracker.resources.charge_editor_for_month_hint
import net.neednot.renttracker.resources.charge_editor_kind_custom
import net.neednot.renttracker.resources.charge_editor_kind_late_fee
import net.neednot.renttracker.resources.charge_editor_loading
import net.neednot.renttracker.resources.charge_editor_note
import net.neednot.renttracker.resources.charge_editor_note_counter
import net.neednot.renttracker.resources.charge_editor_note_custom_hint
import net.neednot.renttracker.resources.charge_editor_note_placeholder_custom
import net.neednot.renttracker.resources.charge_editor_note_placeholder_late_fee
import net.neednot.renttracker.resources.charge_editor_required
import net.neednot.renttracker.resources.charge_editor_suggestion_notice
import net.neednot.renttracker.resources.charge_editor_title_add
import net.neednot.renttracker.resources.charge_editor_title_edit
import net.neednot.renttracker.resources.entry_duplicate_go_back
import net.neednot.renttracker.resources.entry_error_title
import net.neednot.renttracker.resources.entry_optional
import net.neednot.renttracker.resources.ic_arrow_drop_down
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val PART_SEPARATOR = " · "
private const val NOTE_MIN_LINES = 2

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
        actions =
            ChargeEditorActions(
                onKindSelected = viewModel::onKindSelected,
                onFieldChanged = viewModel::onFieldChanged,
                onDateChanged = viewModel::onDateChanged,
                onForMonthChanged = viewModel::onForMonthChanged,
                onSaveClicked = viewModel::onSaveClicked,
                onDuplicateConfirmed = viewModel::onDuplicateConfirmed,
                onDuplicateDismissed = viewModel::onDuplicateDismissed,
                onCloseClicked = onClosed,
            ),
        modifier = modifier,
    )
}

@Composable
internal fun ChargeEditorContent(
    uiState: ChargeEditorUiState,
    actions: ChargeEditorActions,
    modifier: Modifier = Modifier,
) {
    val title = if (uiState.isNewCharge) Res.string.charge_editor_title_add else Res.string.charge_editor_title_edit
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
            LoadingIndicator(
                modifier = Modifier.padding(contentPadding),
                text = stringResource(Res.string.charge_editor_loading),
            )
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
                ChargeFields(uiState = uiState, actions = actions)
            }
        }
    }
    uiState.duplicate?.let { dialog ->
        ChargeDuplicateDialog(
            dialog = dialog,
            onConfirmed = actions.onDuplicateConfirmed,
            onDismissed = actions.onDuplicateDismissed,
        )
    }
}

@Composable
private fun ChargeFields(
    uiState: ChargeEditorUiState,
    actions: ChargeEditorActions,
) {
    val isEnabled = !uiState.isSaving
    ChargeNotices(uiState)
    uiState.header?.let { header ->
        TenantStrip(
            unit = listOf(header.tenantName, header.groupName).joinToString(PART_SEPARATOR),
            balance = header.balance.asString(),
        )
    }
    RtSegmentedButton(
        options = DebtKind.entries.map { stringResource(it.label) },
        selectedIndex = DebtKind.entries.indexOf(uiState.kind),
        onSelected = { index -> actions.onKindSelected(DebtKind.entries[index]) },
        enabled = isEnabled && uiState.isNewCharge,
    )
    MoneyField(
        label = stringResource(Res.string.charge_editor_amount),
        text = uiState.amount,
        onTextChanged = { actions.onFieldChanged(ChargeEditorField.Amount, it) },
        error = uiState.fieldErrors[ChargeEditorField.Amount],
        hint = if (uiState.isFromSuggestion) stringResource(Res.string.charge_editor_amount_suggested) else null,
        enabled = isEnabled,
    )
    val dateHint =
        if (uiState.isNewCharge) Res.string.charge_editor_date_hint_add else Res.string.charge_editor_date_hint_edit
    DateField(
        label = stringResource(Res.string.charge_editor_date),
        date = uiState.date,
        onDateChanged = actions.onDateChanged,
        hint = stringResource(dateHint),
        enabled = isEnabled,
    )
    if (uiState.kind == DebtKind.LateFee) {
        uiState.forMonth?.let { month ->
            MonthField(
                label = stringResource(Res.string.charge_editor_for_month),
                month = month,
                onMonthChanged = actions.onForMonthChanged,
                hint = stringResource(Res.string.charge_editor_for_month_hint),
                enabled = isEnabled,
                icon = Res.drawable.ic_arrow_drop_down,
            )
        }
    }
    NoteField(uiState = uiState, onFieldChanged = actions.onFieldChanged, enabled = isEnabled)
}

@Composable
private fun ChargeNotices(uiState: ChargeEditorUiState) {
    uiState.error?.let { error ->
        Banner(text = error.asString(), kind = BannerKind.Error, title = stringResource(Res.string.entry_error_title))
    }
    uiState.conflictBanner?.let { conflict ->
        Banner(
            text = stringResource(Res.string.charge_editor_conflict_body),
            kind = BannerKind.Warning,
            title = conflict.asString(),
        )
    }
    if (uiState.isFromSuggestion) {
        Banner(text = stringResource(Res.string.charge_editor_suggestion_notice), kind = BannerKind.Info)
    }
}

@Composable
private fun NoteField(
    uiState: ChargeEditorUiState,
    onFieldChanged: (ChargeEditorField, String) -> Unit,
    enabled: Boolean,
) {
    val isCustom = uiState.kind == DebtKind.Custom
    FormTextField(
        label = stringResource(Res.string.charge_editor_note),
        labelNote = stringResource(if (isCustom) Res.string.charge_editor_required else Res.string.entry_optional),
        text = uiState.note,
        onTextChanged = { onFieldChanged(ChargeEditorField.Note, it) },
        error = uiState.fieldErrors[ChargeEditorField.Note],
        hint = if (isCustom) stringResource(Res.string.charge_editor_note_custom_hint) else null,
        counter =
            if (isCustom) {
                stringResource(Res.string.charge_editor_note_counter, uiState.note.length, InputLimits.MAX_NAME_LENGTH)
            } else {
                null
            },
        placeholder =
            stringResource(
                if (isCustom) {
                    Res.string.charge_editor_note_placeholder_custom
                } else {
                    Res.string.charge_editor_note_placeholder_late_fee
                },
            ),
        enabled = enabled,
        singleLine = false,
        minLines = NOTE_MIN_LINES,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
    )
}

@Composable
private fun ChargeDuplicateDialog(
    dialog: LateFeeDuplicateUiState,
    onConfirmed: () -> Unit,
    onDismissed: () -> Unit,
) {
    val single = dialog.matches.singleOrNull()
    DuplicateWarningDialog(
        title = stringResource(Res.string.charge_duplicate_title, dialog.month),
        body =
            if (single != null) {
                stringResource(
                    Res.string.charge_duplicate_body_one,
                    single.amount,
                    dialog.month,
                    single.date,
                    single.by.asString(),
                )
            } else {
                stringResource(Res.string.charge_duplicate_body_many, dialog.month)
            },
        confirmLabel = stringResource(Res.string.charge_duplicate_add_anyway),
        dismissLabel = stringResource(Res.string.entry_duplicate_go_back),
        onConfirmed = onConfirmed,
        onDismissed = onDismissed,
        lines =
            if (single != null) {
                emptyList()
            } else {
                dialog.matches.map { match ->
                    DialogCardLine(
                        leadingText = stringResource(Res.string.charge_duplicate_line, match.date, match.by.asString()),
                        trailingText = match.amount,
                    )
                }
            },
    )
}

private val DebtKind.label
    get() =
        when (this) {
            DebtKind.LateFee -> Res.string.charge_editor_kind_late_fee
            DebtKind.Custom -> Res.string.charge_editor_kind_custom
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
                ),
            actions = ChargeEditorActions(),
        )
    }
}
