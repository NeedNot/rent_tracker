package net.neednot.renttracker.feature.group.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.data.PaymentOrder
import net.neednot.renttracker.core.designsystem.components.EditorTopBar
import net.neednot.renttracker.core.designsystem.components.FormTextField
import net.neednot.renttracker.core.designsystem.components.MoneyField
import net.neednot.renttracker.core.designsystem.components.WarningNotice
import net.neednot.renttracker.core.designsystem.components.WholeNumberField
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.group.presentation.components.CenteredMessage
import net.neednot.renttracker.feature.group.presentation.components.PaymentOrderToggle
import net.neednot.renttracker.feature.group.presentation.components.TimeZoneField
import net.neednot.renttracker.feature.group.presentation.uistate.GroupSettingsField
import net.neednot.renttracker.feature.group.presentation.uistate.GroupSettingsForm
import net.neednot.renttracker.feature.group.presentation.uistate.GroupSettingsUiState
import net.neednot.renttracker.feature.group.presentation.uistate.TimeZoneOptionUiState
import net.neednot.renttracker.feature.group.presentation.viewmodel.GroupSettingsEvent
import net.neednot.renttracker.feature.group.presentation.viewmodel.GroupSettingsViewModel
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.group_back
import net.neednot.renttracker.resources.group_settings_grace_days
import net.neednot.renttracker.resources.group_settings_grace_days_hint
import net.neednot.renttracker.resources.group_settings_late_fee
import net.neednot.renttracker.resources.group_settings_late_fee_hint
import net.neednot.renttracker.resources.group_settings_loading
import net.neednot.renttracker.resources.group_settings_name
import net.neednot.renttracker.resources.group_settings_payment_order
import net.neednot.renttracker.resources.group_settings_read_only
import net.neednot.renttracker.resources.group_settings_time_zone
import net.neednot.renttracker.resources.group_settings_title
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
private fun GroupSettingsContent(
    uiState: GroupSettingsUiState,
    actions: GroupSettingsActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            when (uiState) {
                is GroupSettingsUiState.Editing -> {
                    EditorTopBar(
                        title = stringResource(Res.string.group_settings_title),
                        isSaveEnabled = !uiState.isSaving,
                        onSaveClicked = actions.onSaveClicked,
                        onCloseClicked = actions.onClosed,
                    )
                }

                else -> {
                    ReadOnlyTopBar(
                        groupName = (uiState as? GroupSettingsUiState.ReadOnly)?.groupName,
                        onBackClicked = actions.onClosed,
                    )
                }
            }
        },
    ) { contentPadding ->
        when (uiState) {
            GroupSettingsUiState.Loading -> {
                val description = stringResource(Res.string.group_settings_loading)
                Box(modifier = Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = description })
                }
            }

            is GroupSettingsUiState.Error -> {
                CenteredMessage(title = uiState.message.asString(), modifier = Modifier.padding(contentPadding))
            }

            is GroupSettingsUiState.ReadOnly -> {
                ReadOnlySettings(settings = uiState, modifier = Modifier.padding(contentPadding))
            }

            is GroupSettingsUiState.Editing -> {
                EditableSettings(settings = uiState, actions = actions, modifier = Modifier.padding(contentPadding))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadOnlyTopBar(
    groupName: String?,
    onBackClicked: () -> Unit,
) {
    TopAppBar(
        title = {
            Column {
                Text(stringResource(Res.string.group_settings_title))
                groupName?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClicked) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.group_back),
                )
            }
        },
    )
}

@Composable
private fun ReadOnlySettings(
    settings: GroupSettingsUiState.ReadOnly,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SettingRow(stringResource(Res.string.group_settings_name), settings.groupName)
        SettingRow(stringResource(Res.string.group_settings_time_zone), settings.timeZone.asString())
        SettingRow(stringResource(Res.string.group_settings_payment_order), settings.paymentOrder.asString())
        SettingRow(stringResource(Res.string.group_settings_grace_days), settings.graceDays.asString())
        SettingRow(stringResource(Res.string.group_settings_late_fee), settings.lateFee.asString(), isLast = true)
        Row(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Visibility,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(Res.string.group_settings_read_only),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    value: String,
    isLast: Boolean = false,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
        if (!isLast) HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun EditableSettings(
    settings: GroupSettingsUiState.Editing,
    actions: GroupSettingsActions,
    modifier: Modifier = Modifier,
) {
    val form = settings.form
    val isEnabled = !settings.isSaving
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        settings.conflictBanner?.let { WarningNotice(it.asString()) }
        settings.error?.let { error ->
            Text(
                text = error.asString(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        FormTextField(
            label = stringResource(Res.string.group_settings_name),
            text = form.name,
            onTextChanged = { actions.onFieldChanged(GroupSettingsField.Name, it) },
            error = settings.fieldErrors[GroupSettingsField.Name],
            enabled = isEnabled,
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
        )
        TimeZoneField(
            options = settings.timeZoneOptions,
            selectedId = form.timeZone,
            onSelected = actions.onTimeZoneSelected,
            enabled = isEnabled,
        )
        PaymentOrderToggle(
            selected = form.paymentOrder,
            onSelected = actions.onPaymentOrderSelected,
            enabled = isEnabled,
        )
        WholeNumberField(
            label = stringResource(Res.string.group_settings_grace_days),
            text = form.graceDays,
            onTextChanged = { actions.onFieldChanged(GroupSettingsField.GraceDays, it) },
            error = settings.fieldErrors[GroupSettingsField.GraceDays],
            hint = stringResource(Res.string.group_settings_grace_days_hint),
            enabled = isEnabled,
        )
        MoneyField(
            label = stringResource(Res.string.group_settings_late_fee),
            text = form.lateFee,
            onTextChanged = { actions.onFieldChanged(GroupSettingsField.LateFee, it) },
            error = settings.fieldErrors[GroupSettingsField.LateFee],
            hint = stringResource(Res.string.group_settings_late_fee_hint),
            enabled = isEnabled,
        )
    }
}

private data class GroupSettingsActions(
    val onFieldChanged: (GroupSettingsField, String) -> Unit = { _, _ -> },
    val onTimeZoneSelected: (String) -> Unit = {},
    val onPaymentOrderSelected: (PaymentOrder) -> Unit = {},
    val onSaveClicked: () -> Unit = {},
    val onClosed: () -> Unit = {},
)

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
