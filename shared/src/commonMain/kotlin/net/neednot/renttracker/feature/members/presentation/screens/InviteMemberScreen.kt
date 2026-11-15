package net.neednot.renttracker.feature.members.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.access.CallerAccess
import net.neednot.renttracker.core.access.PermissionPreset
import net.neednot.renttracker.core.data.AccessLevel
import net.neednot.renttracker.core.data.MembershipRole
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.members.presentation.components.MembersEditorTopBar
import net.neednot.renttracker.feature.members.presentation.components.PermissionLevelsCard
import net.neednot.renttracker.feature.members.presentation.components.PresetChips
import net.neednot.renttracker.feature.members.presentation.uistate.EditOthersUiState
import net.neednot.renttracker.feature.members.presentation.uistate.InviteMemberUiState
import net.neednot.renttracker.feature.members.presentation.uistate.PermissionArea
import net.neednot.renttracker.feature.members.presentation.uistate.PermissionsFormUiState
import net.neednot.renttracker.feature.members.presentation.viewmodel.InviteMemberEvent
import net.neednot.renttracker.feature.members.presentation.viewmodel.InviteMemberViewModel
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.members_customize
import net.neednot.renttracker.resources.members_invite_email
import net.neednot.renttracker.resources.members_invite_email_hint
import net.neednot.renttracker.resources.members_invite_email_placeholder
import net.neednot.renttracker.resources.members_invite_limited
import net.neednot.renttracker.resources.members_invite_send
import net.neednot.renttracker.resources.members_invite_sending
import net.neednot.renttracker.resources.members_invite_title
import net.neednot.renttracker.resources.members_loading
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun InviteMemberScreen(
    groupId: String,
    onClosed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InviteMemberViewModel = koinViewModel { parametersOf(groupId) },
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                InviteMemberEvent.InviteSent -> onClosed()
            }
        }
    }
    InviteMemberContent(
        uiState = uiState,
        onEmailChanged = viewModel::onEmailChanged,
        onPresetClicked = viewModel::onPresetClicked,
        onCustomizeClicked = viewModel::onCustomizeClicked,
        onLevelSelected = viewModel::onLevelSelected,
        onEditOthersChanged = viewModel::onEditOthersChanged,
        onSendClicked = viewModel::onSendClicked,
        onCloseClicked = onClosed,
        modifier = modifier,
    )
}

@Composable
private fun InviteMemberContent(
    uiState: InviteMemberUiState,
    onEmailChanged: (String) -> Unit,
    onPresetClicked: (PermissionPreset) -> Unit,
    onCustomizeClicked: () -> Unit,
    onLevelSelected: (PermissionArea, AccessLevel) -> Unit,
    onEditOthersChanged: (Boolean) -> Unit,
    onSendClicked: () -> Unit,
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            MembersEditorTopBar(
                title = stringResource(Res.string.members_invite_title),
                actionLabel = stringResource(Res.string.members_invite_send),
                workingLabel = stringResource(Res.string.members_invite_sending),
                isWorking = uiState.isSending,
                isActionEnabled = !uiState.isSending && uiState.form != null,
                onActionClicked = onSendClicked,
                onCloseClicked = onCloseClicked,
            )
        },
    ) { contentPadding ->
        if (uiState.isLoading) {
            val description = stringResource(Res.string.members_loading)
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
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                InviteFields(
                    uiState = uiState,
                    onEmailChanged = onEmailChanged,
                    onPresetClicked = onPresetClicked,
                    onCustomizeClicked = onCustomizeClicked,
                    onLevelSelected = onLevelSelected,
                    onEditOthersChanged = onEditOthersChanged,
                )
            }
        }
    }
}

@Composable
private fun InviteFields(
    uiState: InviteMemberUiState,
    onEmailChanged: (String) -> Unit,
    onPresetClicked: (PermissionPreset) -> Unit,
    onCustomizeClicked: () -> Unit,
    onLevelSelected: (PermissionArea, AccessLevel) -> Unit,
    onEditOthersChanged: (Boolean) -> Unit,
) {
    val isEnabled = !uiState.isSending
    uiState.error?.let { error ->
        Text(
            text = error.asString(),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    EmailField(email = uiState.email, error = uiState.emailError, onEmailChanged = onEmailChanged, enabled = isEnabled)
    uiState.limitedInviterName?.let { name ->
        Text(
            text = stringResource(Res.string.members_invite_limited, name),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    val form = uiState.form ?: return
    PresetChips(
        presets = form.presets,
        selected = form.selectedPreset,
        onPresetClicked = onPresetClicked,
        enabled = isEnabled,
    )
    CustomizeToggle(isExpanded = uiState.isCustomizeExpanded, onClick = onCustomizeClicked)
    if (uiState.isCustomizeExpanded) {
        PermissionLevelsCard(
            form = form,
            onLevelSelected = onLevelSelected,
            onEditOthersChanged = onEditOthersChanged,
            enabled = isEnabled,
        )
    }
}

@Composable
private fun EmailField(
    email: String,
    error: UiText?,
    onEmailChanged: (String) -> Unit,
    enabled: Boolean,
) {
    OutlinedTextField(
        value = email,
        onValueChange = onEmailChanged,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        label = { Text(stringResource(Res.string.members_invite_email)) },
        placeholder = { Text(stringResource(Res.string.members_invite_email_placeholder)) },
        supportingText = { Text(error?.asString() ?: stringResource(Res.string.members_invite_email_hint)) },
        isError = error != null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
    )
}

@Composable
private fun CustomizeToggle(
    isExpanded: Boolean,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.heightIn(min = 48.dp),
    ) {
        Text(stringResource(Res.string.members_customize))
        Icon(
            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null,
        )
    }
}

@Preview
@Composable
private fun InviteMemberContentPreview() {
    val access =
        CallerAccess(
            membershipId = "m1",
            accountId = "me",
            role = MembershipRole.Owner,
            permissions = PermissionPreset.ownerPermissions,
            groupArchived = false,
        )
    RentTrackerTheme {
        InviteMemberContent(
            uiState =
                InviteMemberUiState(
                    isLoading = false,
                    email = "alex.m@example.com",
                    form =
                        PermissionsFormUiState(
                            permissions = PermissionPreset.Bookkeeper.permissions,
                            presets = access.grantablePresets(),
                            levelRows = emptyList(),
                            editOthers = EditOthersUiState(isOn = false, appliesNow = true, isEnabled = true),
                        ),
                ),
            onEmailChanged = {},
            onPresetClicked = {},
            onCustomizeClicked = {},
            onLevelSelected = { _, _ -> },
            onEditOthersChanged = {},
            onSendClicked = {},
            onCloseClicked = {},
        )
    }
}
