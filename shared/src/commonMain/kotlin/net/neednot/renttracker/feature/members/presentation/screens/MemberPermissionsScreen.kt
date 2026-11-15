package net.neednot.renttracker.feature.members.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import net.neednot.renttracker.core.access.CallerAccess
import net.neednot.renttracker.core.access.PermissionPreset
import net.neednot.renttracker.core.data.AccessLevel
import net.neednot.renttracker.core.data.MembershipRole
import net.neednot.renttracker.core.designsystem.components.WarningNotice
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.members.presentation.components.MemberHeaderCard
import net.neednot.renttracker.feature.members.presentation.components.MembersEditorTopBar
import net.neednot.renttracker.feature.members.presentation.components.PermissionLevelsCard
import net.neednot.renttracker.feature.members.presentation.components.PresetChips
import net.neednot.renttracker.feature.members.presentation.components.RemoveMemberDialog
import net.neednot.renttracker.feature.members.presentation.uistate.EditOthersUiState
import net.neednot.renttracker.feature.members.presentation.uistate.MemberHeaderUiState
import net.neednot.renttracker.feature.members.presentation.uistate.MemberPermissionsUiState
import net.neednot.renttracker.feature.members.presentation.uistate.PermissionArea
import net.neednot.renttracker.feature.members.presentation.uistate.PermissionsFormUiState
import net.neednot.renttracker.feature.members.presentation.viewmodel.MemberPermissionsEvent
import net.neednot.renttracker.feature.members.presentation.viewmodel.MemberPermissionsViewModel
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.members_loading
import net.neednot.renttracker.resources.members_permissions_title
import net.neednot.renttracker.resources.members_remove_from_group
import net.neednot.renttracker.resources.members_save
import net.neednot.renttracker.resources.members_saving
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun MemberPermissionsScreen(
    groupId: String,
    membershipId: String,
    onClosed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MemberPermissionsViewModel = koinViewModel { parametersOf(groupId, membershipId) },
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                MemberPermissionsEvent.PermissionsSaved, MemberPermissionsEvent.MemberRemoved -> onClosed()
            }
        }
    }
    MemberPermissionsContent(
        uiState = uiState,
        onPresetClicked = viewModel::onPresetClicked,
        onLevelSelected = viewModel::onLevelSelected,
        onEditOthersChanged = viewModel::onEditOthersChanged,
        onSaveClicked = viewModel::onSaveClicked,
        onRemoveClicked = viewModel::onRemoveClicked,
        onRemoveConfirmed = viewModel::onRemoveConfirmed,
        onRemoveDismissed = viewModel::onRemoveDismissed,
        onCloseClicked = onClosed,
        modifier = modifier,
    )
}

@Composable
private fun MemberPermissionsContent(
    uiState: MemberPermissionsUiState,
    onPresetClicked: (PermissionPreset) -> Unit,
    onLevelSelected: (PermissionArea, AccessLevel) -> Unit,
    onEditOthersChanged: (Boolean) -> Unit,
    onSaveClicked: () -> Unit,
    onRemoveClicked: () -> Unit,
    onRemoveConfirmed: () -> Unit,
    onRemoveDismissed: () -> Unit,
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            MembersEditorTopBar(
                title = stringResource(Res.string.members_permissions_title),
                actionLabel = stringResource(Res.string.members_save),
                workingLabel = stringResource(Res.string.members_saving),
                isWorking = uiState.isSaving,
                isActionEnabled = !uiState.isSaving && uiState.form != null,
                onActionClicked = onSaveClicked,
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
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PermissionsFields(
                    uiState = uiState,
                    onPresetClicked = onPresetClicked,
                    onLevelSelected = onLevelSelected,
                    onEditOthersChanged = onEditOthersChanged,
                    onRemoveClicked = onRemoveClicked,
                )
            }
        }
    }
    uiState.removeDialog?.let { dialog ->
        RemoveMemberDialog(dialog = dialog, onConfirmed = onRemoveConfirmed, onDismissed = onRemoveDismissed)
    }
}

@Composable
private fun PermissionsFields(
    uiState: MemberPermissionsUiState,
    onPresetClicked: (PermissionPreset) -> Unit,
    onLevelSelected: (PermissionArea, AccessLevel) -> Unit,
    onEditOthersChanged: (Boolean) -> Unit,
    onRemoveClicked: () -> Unit,
) {
    val isEnabled = !uiState.isSaving
    uiState.conflictBanner?.let { banner -> WarningNotice(banner.asString()) }
    uiState.header?.let { header -> MemberHeaderCard(header) }
    uiState.error?.let { error ->
        Text(
            text = error.asString(),
            color = MaterialTheme.colorScheme.error,
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
    PermissionLevelsCard(
        form = form,
        onLevelSelected = onLevelSelected,
        onEditOthersChanged = onEditOthersChanged,
        enabled = isEnabled,
    )
    if (uiState.canRemove) {
        OutlinedButton(
            onClick = onRemoveClicked,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            enabled = isEnabled,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
        ) {
            Text(stringResource(Res.string.members_remove_from_group))
        }
    }
}

@Preview
@Composable
private fun MemberPermissionsContentPreview() {
    val owner =
        CallerAccess(
            membershipId = "m1",
            accountId = "me",
            role = MembershipRole.Owner,
            permissions = PermissionPreset.ownerPermissions,
            groupArchived = false,
        )
    RentTrackerTheme {
        MemberPermissionsContent(
            uiState =
                MemberPermissionsUiState(
                    isLoading = false,
                    header = MemberHeaderUiState("KC", "Kevin Cho", "kevin.cho@example.com"),
                    form =
                        PermissionsFormUiState(
                            permissions = PermissionPreset.Viewer.permissions,
                            presets = owner.grantablePresets(),
                            levelRows = emptyList(),
                            editOthers = EditOthersUiState(isOn = false, appliesNow = false, isEnabled = false),
                        ),
                    canRemove = true,
                ),
            onPresetClicked = {},
            onLevelSelected = { _, _ -> },
            onEditOthersChanged = {},
            onSaveClicked = {},
            onRemoveClicked = {},
            onRemoveConfirmed = {},
            onRemoveDismissed = {},
            onCloseClicked = {},
        )
    }
}
