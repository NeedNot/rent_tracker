package net.neednot.renttracker.feature.group.presentation.screens

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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import net.neednot.renttracker.core.data.PaymentOrder
import net.neednot.renttracker.core.designsystem.components.Banner
import net.neednot.renttracker.core.designsystem.components.BannerKind
import net.neednot.renttracker.core.designsystem.components.ContentLayout
import net.neednot.renttracker.core.designsystem.components.EditorTopBar
import net.neednot.renttracker.core.designsystem.components.ErrorState
import net.neednot.renttracker.core.designsystem.components.FieldGroup
import net.neednot.renttracker.core.designsystem.components.FormTextField
import net.neednot.renttracker.core.designsystem.components.LoadingIndicator
import net.neednot.renttracker.core.designsystem.components.MoneyField
import net.neednot.renttracker.core.designsystem.components.RtSegmentedButton
import net.neednot.renttracker.core.designsystem.components.RtTopAppBar
import net.neednot.renttracker.core.designsystem.components.TopBarNavigation
import net.neednot.renttracker.core.designsystem.components.UnimplementedAction
import net.neednot.renttracker.core.designsystem.components.WholeNumberField
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.group.presentation.components.TimeZoneField
import net.neednot.renttracker.feature.group.presentation.uistate.GroupSettingsField
import net.neednot.renttracker.feature.group.presentation.uistate.GroupSettingsForm
import net.neednot.renttracker.feature.group.presentation.uistate.GroupSettingsUiState
import net.neednot.renttracker.feature.group.presentation.uistate.TimeZoneOptionUiState
import net.neednot.renttracker.feature.group.presentation.viewmodel.GroupSettingsEvent
import net.neednot.renttracker.feature.group.presentation.viewmodel.GroupSettingsViewModel
import net.neednot.renttracker.feature.group.presentation.viewmodel.label
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.group_error_title
import net.neednot.renttracker.resources.group_settings_conflict_body
import net.neednot.renttracker.resources.group_settings_grace_days
import net.neednot.renttracker.resources.group_settings_grace_days_hint
import net.neednot.renttracker.resources.group_settings_late_fee
import net.neednot.renttracker.resources.group_settings_late_fee_hint
import net.neednot.renttracker.resources.group_settings_late_fee_optional
import net.neednot.renttracker.resources.group_settings_name
import net.neednot.renttracker.resources.group_settings_payment_order
import net.neednot.renttracker.resources.group_settings_payment_order_hint
import net.neednot.renttracker.resources.group_settings_read_only
import net.neednot.renttracker.resources.group_settings_title
import net.neednot.renttracker.resources.ic_visibility
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun GroupSettingsScreen(
    groupId: String,
    onClosed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupSettingsViewModel = koinViewModel { parametersOf(groupId) },
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                GroupSettingsEvent.Saved -> onClosed()
            }
        }
    }
    GroupSettingsContent(
        uiState = uiState,
        actions =
            GroupSettingsActions(
                onFieldChanged = viewModel::onFieldChanged,
                onTimeZoneSelected = viewModel::onTimeZoneSelected,
                onPaymentOrderSelected = viewModel::onPaymentOrderSelected,
                onSaveClicked = viewModel::onSaveClicked,
                onClosed = onClosed,
            ),
        modifier = modifier,
    )
}

@Composable
internal fun GroupSettingsContent(
    uiState: GroupSettingsUiState,
    actions: GroupSettingsActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { GroupSettingsTopBar(uiState = uiState, actions = actions) },
    ) { contentPadding ->
        val bodyModifier = Modifier.padding(contentPadding)
        when (uiState) {
            GroupSettingsUiState.Loading -> {
                LoadingIndicator(modifier = bodyModifier)
            }

            is GroupSettingsUiState.Error -> {
                ErrorState(
                    title = stringResource(Res.string.group_error_title),
                    text = uiState.message.asString(),
                    onRetryClicked = UnimplementedAction,
                    modifier = bodyModifier,
                )
            }

            is GroupSettingsUiState.ReadOnly -> {
                SettingsForm(modifier = bodyModifier) {
                    Banner(
                        text = stringResource(Res.string.group_settings_read_only),
                        kind = BannerKind.Info,
                        icon = Res.drawable.ic_visibility,
                    )
                    SettingsFields(
                        form = uiState.form,
                        timeZoneOptions = uiState.timeZoneOptions,
                        fieldErrors = emptyMap(),
                        enabled = false,
                        actions = actions,
                    )
                }
            }

            is GroupSettingsUiState.Editing -> {
                SettingsForm(modifier = bodyModifier) {
                    uiState.conflictTitle?.let { title ->
                        Banner(
                            title = title.asString(),
                            text = stringResource(Res.string.group_settings_conflict_body),
                            kind = BannerKind.Warning,
                        )
                    }
                    uiState.error?.let { Banner(text = it.asString(), kind = BannerKind.Error) }
                    SettingsFields(
                        form = uiState.form,
                        timeZoneOptions = uiState.timeZoneOptions,
                        fieldErrors = uiState.fieldErrors,
                        enabled = !uiState.isSaving,
                        actions = actions,
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupSettingsTopBar(
    uiState: GroupSettingsUiState,
    actions: GroupSettingsActions,
) {
    val title = stringResource(Res.string.group_settings_title)
    when (uiState) {
        is GroupSettingsUiState.Editing -> {
            EditorTopBar(
                title = title,
                isSaveEnabled = !uiState.isSaving,
                onSaveClicked = actions.onSaveClicked,
                onCloseClicked = actions.onClosed,
                isSaving = uiState.isSaving,
            )
        }

        is GroupSettingsUiState.ReadOnly -> {
            RtTopAppBar(title = title, onNavigationClicked = actions.onClosed)
        }

        GroupSettingsUiState.Loading, is GroupSettingsUiState.Error -> {
            RtTopAppBar(title = title, navigation = TopBarNavigation.Close, onNavigationClicked = actions.onClosed)
        }
    }
}

@Composable
private fun SettingsForm(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(ContentLayout.Form.padding),
        verticalArrangement = Arrangement.spacedBy(ContentLayout.Form.spacing),
    ) {
        content()
    }
}

@Composable
private fun SettingsFields(
    form: GroupSettingsForm,
    timeZoneOptions: List<TimeZoneOptionUiState>,
    fieldErrors: Map<GroupSettingsField, UiText>,
    enabled: Boolean,
    actions: GroupSettingsActions,
) {
    FormTextField(
        label = stringResource(Res.string.group_settings_name),
        text = form.name,
        onTextChanged = { actions.onFieldChanged(GroupSettingsField.Name, it) },
        error = fieldErrors[GroupSettingsField.Name],
        enabled = enabled,
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
    )
    TimeZoneField(
        options = timeZoneOptions,
        selectedId = form.timeZone,
        onSelected = actions.onTimeZoneSelected,
        enabled = enabled,
    )
    FieldGroup(
        label = stringResource(Res.string.group_settings_payment_order),
        hint = stringResource(Res.string.group_settings_payment_order_hint),
    ) {
        RtSegmentedButton(
            options = PaymentOrder.entries.map { it.label().asString() },
            selectedIndex = PaymentOrder.entries.indexOf(form.paymentOrder),
            onSelected = { actions.onPaymentOrderSelected(PaymentOrder.entries[it]) },
            enabled = enabled,
        )
    }
    WholeNumberField(
        label = stringResource(Res.string.group_settings_grace_days),
        text = form.graceDays,
        onTextChanged = { actions.onFieldChanged(GroupSettingsField.GraceDays, it) },
        error = fieldErrors[GroupSettingsField.GraceDays],
        hint = stringResource(Res.string.group_settings_grace_days_hint),
        enabled = enabled,
    )
    MoneyField(
        label = stringResource(Res.string.group_settings_late_fee),
        labelNote = stringResource(Res.string.group_settings_late_fee_optional),
        text = form.lateFee,
        onTextChanged = { actions.onFieldChanged(GroupSettingsField.LateFee, it) },
        error = fieldErrors[GroupSettingsField.LateFee],
        hint = stringResource(Res.string.group_settings_late_fee_hint),
        enabled = enabled,
    )
}

@Preview
@Composable
private fun GroupSettingsContentPreview() {
    RentTrackerTheme {
        GroupSettingsContent(
            uiState =
                GroupSettingsUiState.Editing(
                    form = GroupSettingsForm("1st Street", "America/Los_Angeles", PaymentOrder.RentFirst, "5", "50.00"),
                    timeZoneOptions =
                        listOf(
                            TimeZoneOptionUiState("America/Los_Angeles", UiText.Plain("Pacific Time")),
                        ),
                ),
            actions = GroupSettingsActions(),
        )
    }
}
