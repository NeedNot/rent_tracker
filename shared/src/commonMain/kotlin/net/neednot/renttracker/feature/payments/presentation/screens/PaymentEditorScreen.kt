package net.neednot.renttracker.feature.payments.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.data.PaymentMethod
import net.neednot.renttracker.core.designsystem.components.Banner
import net.neednot.renttracker.core.designsystem.components.BannerKind
import net.neednot.renttracker.core.designsystem.components.ContentLayout
import net.neednot.renttracker.core.designsystem.components.DateField
import net.neednot.renttracker.core.designsystem.components.DialogButton
import net.neednot.renttracker.core.designsystem.components.DialogCardLine
import net.neednot.renttracker.core.designsystem.components.DuplicateWarningDialog
import net.neednot.renttracker.core.designsystem.components.EditorTopBar
import net.neednot.renttracker.core.designsystem.components.FieldGroup
import net.neednot.renttracker.core.designsystem.components.FormTextField
import net.neednot.renttracker.core.designsystem.components.LoadingIndicator
import net.neednot.renttracker.core.designsystem.components.MoneyField
import net.neednot.renttracker.core.designsystem.components.OfflineBanner
import net.neednot.renttracker.core.designsystem.components.RtDialog
import net.neednot.renttracker.core.designsystem.components.RtFilterChip
import net.neednot.renttracker.core.designsystem.components.TenantStrip
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.designsystem.theme.Spacing
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.payments.presentation.components.LateFeeOfferBox
import net.neednot.renttracker.feature.payments.presentation.components.label
import net.neednot.renttracker.feature.payments.presentation.uistate.DuplicateDialogUiState
import net.neednot.renttracker.feature.payments.presentation.uistate.DuplicateMatchUiState
import net.neednot.renttracker.feature.payments.presentation.uistate.LateFeeOfferUiState
import net.neednot.renttracker.feature.payments.presentation.uistate.MonthChangesUiState
import net.neednot.renttracker.feature.payments.presentation.uistate.PaymentEditorField
import net.neednot.renttracker.feature.payments.presentation.uistate.PaymentEditorUiState
import net.neednot.renttracker.feature.payments.presentation.uistate.PaymentHeaderUiState
import net.neednot.renttracker.feature.payments.presentation.viewmodel.PaymentEditorEvent
import net.neednot.renttracker.feature.payments.presentation.viewmodel.PaymentEditorTarget
import net.neednot.renttracker.feature.payments.presentation.viewmodel.PaymentEditorViewModel
import net.neednot.renttracker.feature.payments.presentation.viewmodel.SyncAlertsAnswer
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.entry_duplicate_go_back
import net.neednot.renttracker.resources.entry_error_title
import net.neednot.renttracker.resources.entry_optional
import net.neednot.renttracker.resources.payment_duplicate_save_anyway
import net.neednot.renttracker.resources.payment_editor_amount
import net.neednot.renttracker.resources.payment_editor_conflict_body
import net.neednot.renttracker.resources.payment_editor_date
import net.neednot.renttracker.resources.payment_editor_date_hint
import net.neednot.renttracker.resources.payment_editor_loading
import net.neednot.renttracker.resources.payment_editor_method
import net.neednot.renttracker.resources.payment_editor_method_last_used
import net.neednot.renttracker.resources.payment_editor_note
import net.neednot.renttracker.resources.payment_editor_note_placeholder
import net.neednot.renttracker.resources.payment_editor_offline_note
import net.neednot.renttracker.resources.payment_editor_title_edit
import net.neednot.renttracker.resources.payment_editor_title_log
import net.neednot.renttracker.resources.payment_preview_change_line
import net.neednot.renttracker.resources.payment_preview_check_failed
import net.neednot.renttracker.resources.payment_sync_alerts_allow
import net.neednot.renttracker.resources.payment_sync_alerts_message
import net.neednot.renttracker.resources.payment_sync_alerts_not_now
import net.neednot.renttracker.resources.payment_sync_alerts_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val PART_SEPARATOR = " · "
private const val NOTE_MIN_LINES = 2

@Composable
fun PaymentEditorScreen(
    target: PaymentEditorTarget,
    onClosed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PaymentEditorViewModel = koinViewModel { parametersOf(target) },
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                PaymentEditorEvent.PaymentSaved -> onClosed()
            }
        }
    }
    PaymentEditorContent(
        uiState = uiState,
        actions =
            PaymentEditorActions(
                onFieldChanged = viewModel::onFieldChanged,
                onDateChanged = viewModel::onDateChanged,
                onMethodClicked = viewModel::onMethodClicked,
                onLateFeeToggled = viewModel::onLateFeeToggled,
                onSaveClicked = viewModel::onSaveClicked,
                onDuplicateConfirmed = viewModel::onDuplicateConfirmed,
                onDuplicateDismissed = viewModel::onDuplicateDismissed,
                onSyncAlertsAnswered = viewModel::onSyncAlertsAnswered,
                onCloseClicked = onClosed,
            ),
        modifier = modifier,
    )
}

@Composable
internal fun PaymentEditorContent(
    uiState: PaymentEditorUiState,
    actions: PaymentEditorActions,
    modifier: Modifier = Modifier,
) {
    val title = if (uiState.isNewPayment) Res.string.payment_editor_title_log else Res.string.payment_editor_title_edit
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
        Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            uiState.offlineBanner?.let { banner -> OfflineBanner(banner.asString()) }
            if (uiState.isLoading) {
                LoadingIndicator(text = stringResource(Res.string.payment_editor_loading))
            } else {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .imePadding()
                            .verticalScroll(rememberScrollState())
                            .padding(ContentLayout.Form.padding),
                    verticalArrangement = Arrangement.spacedBy(ContentLayout.Form.spacing),
                ) {
                    PaymentFields(uiState = uiState, actions = actions)
                }
            }
        }
    }
    uiState.duplicate?.let { dialog ->
        PaymentDuplicateDialog(
            dialog = dialog,
            onConfirmed = actions.onDuplicateConfirmed,
            onDismissed = actions.onDuplicateDismissed,
        )
    }
    if (uiState.isSyncAlertsExplainerShown) {
        SyncAlertsDialog(onAnswered = actions.onSyncAlertsAnswered)
    }
}

@Composable
private fun PaymentFields(
    uiState: PaymentEditorUiState,
    actions: PaymentEditorActions,
) {
    val isEnabled = !uiState.isSaving
    PaymentNotices(uiState)
    uiState.header?.let { header ->
        TenantStrip(
            unit = listOfNotNull(header.tenantName, header.groupName).joinToString(PART_SEPARATOR),
            balance = header.balance?.asString(),
        )
    }
    MoneyField(
        label = stringResource(Res.string.payment_editor_amount),
        text = uiState.amount,
        onTextChanged = { actions.onFieldChanged(PaymentEditorField.Amount, it) },
        error = uiState.fieldErrors[PaymentEditorField.Amount],
        enabled = isEnabled,
    )
    DateField(
        label = stringResource(Res.string.payment_editor_date),
        date = uiState.date,
        onDateChanged = actions.onDateChanged,
        error = uiState.fieldErrors[PaymentEditorField.Date],
        hint = stringResource(Res.string.payment_editor_date_hint),
        enabled = isEnabled,
    )
    MethodField(
        selected = uiState.method,
        lastUsed = uiState.lastUsedMethod,
        onMethodClicked = actions.onMethodClicked,
        enabled = isEnabled,
    )
    FormTextField(
        label = stringResource(Res.string.payment_editor_note),
        labelNote = stringResource(Res.string.entry_optional),
        text = uiState.note,
        onTextChanged = { actions.onFieldChanged(PaymentEditorField.Note, it) },
        error = uiState.fieldErrors[PaymentEditorField.Note],
        placeholder = stringResource(Res.string.payment_editor_note_placeholder),
        enabled = isEnabled,
        singleLine = false,
        minLines = NOTE_MIN_LINES,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
    )
    uiState.offeredLateFee?.let { offer ->
        LateFeeOfferBox(
            offer = offer,
            amountError = uiState.fieldErrors[PaymentEditorField.LateFeeAmount],
            onToggled = actions.onLateFeeToggled,
            onAmountChanged = { actions.onFieldChanged(PaymentEditorField.LateFeeAmount, it) },
            enabled = isEnabled,
        )
    }
    uiState.shownMonthChanges?.let { changes -> MonthChangesNotice(changes) }
}

@Composable
private fun PaymentNotices(uiState: PaymentEditorUiState) {
    uiState.error?.let { error ->
        Banner(text = error.asString(), kind = BannerKind.Error, title = stringResource(Res.string.entry_error_title))
    }
    uiState.conflictBanner?.let { conflict ->
        Banner(
            text = stringResource(Res.string.payment_editor_conflict_body),
            kind = BannerKind.Warning,
            title = conflict.asString(),
        )
    }
    if (uiState.isOffline && uiState.isNewPayment) {
        Banner(text = stringResource(Res.string.payment_editor_offline_note), kind = BannerKind.Info)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MethodField(
    selected: PaymentMethod?,
    lastUsed: PaymentMethod?,
    onMethodClicked: (PaymentMethod) -> Unit,
    enabled: Boolean,
) {
    FieldGroup(
        label = stringResource(Res.string.payment_editor_method),
        labelNote =
            if (lastUsed == null) {
                stringResource(Res.string.entry_optional)
            } else {
                stringResource(Res.string.payment_editor_method_last_used, stringResource(lastUsed.label))
            },
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            PaymentMethod.entries.forEach { method ->
                RtFilterChip(
                    text = stringResource(method.label),
                    selected = method == selected,
                    onClick = { onMethodClicked(method) },
                    enabled = enabled,
                )
            }
        }
    }
}

@Composable
private fun MonthChangesNotice(changes: MonthChangesUiState) {
    when (changes) {
        is MonthChangesUiState.Changes -> {
            val lines =
                changes.lines.map {
                    stringResource(
                        Res.string.payment_preview_change_line,
                        it.month,
                        it.before.asString(),
                        it.after.asString(),
                    )
                }
            Banner(text = lines.joinToString("\n"), kind = BannerKind.Info, title = changes.heading.asString())
        }

        MonthChangesUiState.CheckFailed -> {
            Banner(text = stringResource(Res.string.payment_preview_check_failed), kind = BannerKind.Info)
        }
    }
}

@Composable
private fun PaymentDuplicateDialog(
    dialog: DuplicateDialogUiState,
    onConfirmed: () -> Unit,
    onDismissed: () -> Unit,
) {
    DuplicateWarningDialog(
        title = dialog.title.asString(),
        body = dialog.intro.asString(),
        confirmLabel = stringResource(Res.string.payment_duplicate_save_anyway),
        dismissLabel = stringResource(Res.string.entry_duplicate_go_back),
        onConfirmed = onConfirmed,
        onDismissed = onDismissed,
        lines = dialog.matches.map { it.line() },
    )
}

@Composable
private fun DuplicateMatchUiState.line(): DialogCardLine {
    val parts = listOfNotNull(kind?.asString(), date, method?.let { stringResource(it.label) })
    return DialogCardLine(leadingText = parts.joinToString(PART_SEPARATOR), trailingText = amount.asString())
}

@Composable
private fun SyncAlertsDialog(onAnswered: (SyncAlertsAnswer) -> Unit) {
    RtDialog(
        title = stringResource(Res.string.payment_sync_alerts_title),
        onDismissed = { onAnswered(SyncAlertsAnswer.NotNow) },
        confirmButton = {
            DialogButton(
                text = stringResource(Res.string.payment_sync_alerts_allow),
                onClick = { onAnswered(SyncAlertsAnswer.Allow) },
            )
        },
        dismissButton = {
            DialogButton(
                text = stringResource(Res.string.payment_sync_alerts_not_now),
                onClick = { onAnswered(SyncAlertsAnswer.NotNow) },
            )
        },
    ) {
        Text(stringResource(Res.string.payment_sync_alerts_message))
    }
}

@Preview
@Composable
private fun PaymentEditorContentPreview() {
    RentTrackerTheme {
        PaymentEditorContent(
            uiState =
                PaymentEditorUiState(
                    isNewPayment = true,
                    isLoading = false,
                    header =
                        PaymentHeaderUiState(
                            tenantName = "Apt 3B",
                            groupName = "1st Street",
                            balance = UiText.Plain("Owes $550.00"),
                        ),
                    today = LocalDate(2026, 3, 10),
                    date = LocalDate(2026, 3, 10),
                    method = PaymentMethod.Zelle,
                    lastUsedMethod = PaymentMethod.Zelle,
                    lateFeeOffer =
                        LateFeeOfferUiState(
                            month = YearMonth(2026, 3),
                            isChecked = true,
                            amount = "50.00",
                            suggestedAmount = "$50.00",
                        ),
                ),
            actions = PaymentEditorActions(),
        )
    }
}
