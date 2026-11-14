package net.neednot.renttracker.feature.payments.presentation.screens

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
import net.neednot.renttracker.core.data.PaymentMethod
import net.neednot.renttracker.core.designsystem.components.DateField
import net.neednot.renttracker.core.designsystem.components.FormTextField
import net.neednot.renttracker.core.designsystem.components.MoneyField
import net.neednot.renttracker.core.designsystem.components.WarningNotice
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.payments.presentation.components.DuplicateDialog
import net.neednot.renttracker.feature.payments.presentation.components.LateFeeOfferBox
import net.neednot.renttracker.feature.payments.presentation.components.PaymentEditorTopBar
import net.neednot.renttracker.feature.payments.presentation.components.PaymentHeaderCard
import net.neednot.renttracker.feature.payments.presentation.components.PaymentMethodChips
import net.neednot.renttracker.feature.payments.presentation.uistate.LateFeeOfferUiState
import net.neednot.renttracker.feature.payments.presentation.uistate.PaymentEditorField
import net.neednot.renttracker.feature.payments.presentation.uistate.PaymentEditorUiState
import net.neednot.renttracker.feature.payments.presentation.uistate.PaymentHeaderUiState
import net.neednot.renttracker.feature.payments.presentation.viewmodel.PaymentEditorEvent
import net.neednot.renttracker.feature.payments.presentation.viewmodel.PaymentEditorTarget
import net.neednot.renttracker.feature.payments.presentation.viewmodel.PaymentEditorViewModel
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.payment_editor_amount
import net.neednot.renttracker.resources.payment_editor_date
import net.neednot.renttracker.resources.payment_editor_date_hint
import net.neednot.renttracker.resources.payment_editor_loading
import net.neednot.renttracker.resources.payment_editor_note
import net.neednot.renttracker.resources.payment_editor_title_edit
import net.neednot.renttracker.resources.payment_editor_title_log
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

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
        onFieldChanged = viewModel::onFieldChanged,
        onDateChanged = viewModel::onDateChanged,
        onMethodClicked = viewModel::onMethodClicked,
        onLateFeeToggled = viewModel::onLateFeeToggled,
        onSaveClicked = viewModel::onSaveClicked,
        onDuplicateConfirmed = viewModel::onDuplicateConfirmed,
        onDuplicateDismissed = viewModel::onDuplicateDismissed,
        onCloseClicked = onClosed,
        modifier = modifier,
    )
}

@Composable
private fun PaymentEditorContent(
    uiState: PaymentEditorUiState,
    onFieldChanged: (PaymentEditorField, String) -> Unit,
    onDateChanged: (LocalDate) -> Unit,
    onMethodClicked: (PaymentMethod) -> Unit,
    onLateFeeToggled: (Boolean) -> Unit,
    onSaveClicked: () -> Unit,
    onDuplicateConfirmed: () -> Unit,
    onDuplicateDismissed: () -> Unit,
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = if (uiState.isNewPayment) Res.string.payment_editor_title_log else Res.string.payment_editor_title_edit
    Scaffold(
        modifier = modifier,
        topBar = {
            PaymentEditorTopBar(
                title = stringResource(title),
                isSaving = uiState.isSaving,
                isSaveEnabled = !uiState.isSaving && !uiState.isLoading,
                onSaveClicked = onSaveClicked,
                onCloseClicked = onCloseClicked,
            )
        },
    ) { contentPadding ->
        if (uiState.isLoading) {
            val description = stringResource(Res.string.payment_editor_loading)
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
                PaymentFields(
                    uiState = uiState,
                    onFieldChanged = onFieldChanged,
                    onDateChanged = onDateChanged,
                    onMethodClicked = onMethodClicked,
                    onLateFeeToggled = onLateFeeToggled,
                )
            }
        }
    }
    uiState.duplicate?.let { dialog ->
        DuplicateDialog(dialog = dialog, onConfirmed = onDuplicateConfirmed, onDismissed = onDuplicateDismissed)
    }
}

@Composable
private fun PaymentFields(
    uiState: PaymentEditorUiState,
    onFieldChanged: (PaymentEditorField, String) -> Unit,
    onDateChanged: (LocalDate) -> Unit,
    onMethodClicked: (PaymentMethod) -> Unit,
    onLateFeeToggled: (Boolean) -> Unit,
) {
    val isEnabled = !uiState.isSaving
    uiState.conflictBanner?.let { banner -> WarningNotice(banner.asString()) }
    uiState.header?.let { header -> PaymentHeaderCard(header) }
    MoneyField(
        label = stringResource(Res.string.payment_editor_amount),
        text = uiState.amount,
        onTextChanged = { onFieldChanged(PaymentEditorField.Amount, it) },
        error = uiState.fieldErrors[PaymentEditorField.Amount],
        enabled = isEnabled,
    )
    DateField(
        label = stringResource(Res.string.payment_editor_date),
        date = uiState.date,
        onDateChanged = onDateChanged,
        error = uiState.fieldErrors[PaymentEditorField.Date],
        hint = stringResource(Res.string.payment_editor_date_hint),
        enabled = isEnabled,
    )
    PaymentMethodChips(
        selected = uiState.method,
        lastUsed = uiState.lastUsedMethod,
        onMethodClicked = onMethodClicked,
        enabled = isEnabled,
    )
    FormTextField(
        label = stringResource(Res.string.payment_editor_note),
        text = uiState.note,
        onTextChanged = { onFieldChanged(PaymentEditorField.Note, it) },
        enabled = isEnabled,
    )
    val today = uiState.today
    uiState.lateFeeOffer?.let { offer ->
        if (today != null) {
            LateFeeOfferBox(
                offer = offer,
                today = today,
                amountError = uiState.fieldErrors[PaymentEditorField.LateFeeAmount],
                onToggled = onLateFeeToggled,
                onAmountChanged = { onFieldChanged(PaymentEditorField.LateFeeAmount, it) },
                enabled = isEnabled,
            )
        }
    }
    uiState.error?.let { error ->
        Text(
            text = error.asString(),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
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
                    lateFeeOffer = LateFeeOfferUiState(YearMonth(2026, 3), isChecked = true, amount = "50.00"),
                ),
            onFieldChanged = { _, _ -> },
            onDateChanged = {},
            onMethodClicked = {},
            onLateFeeToggled = {},
            onSaveClicked = {},
            onDuplicateConfirmed = {},
            onDuplicateDismissed = {},
            onCloseClicked = {},
        )
    }
}
