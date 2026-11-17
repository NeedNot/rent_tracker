package net.neednot.renttracker.feature.members.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.neednot.renttracker.core.access.CallerAccess
import net.neednot.renttracker.core.access.PermissionPreset
import net.neednot.renttracker.core.data.MembershipRole
import net.neednot.renttracker.core.designsystem.components.ActionTone
import net.neednot.renttracker.core.designsystem.components.Banner
import net.neednot.renttracker.core.designsystem.components.BannerKind
import net.neednot.renttracker.core.designsystem.components.ButtonVariant
import net.neednot.renttracker.core.designsystem.components.ContentLayout
import net.neednot.renttracker.core.designsystem.components.EditorTopBar
import net.neednot.renttracker.core.designsystem.components.ErrorState
import net.neednot.renttracker.core.designsystem.components.InitialsAvatar
import net.neednot.renttracker.core.designsystem.components.LoadingIndicator
import net.neednot.renttracker.core.designsystem.components.RtButton
import net.neednot.renttracker.core.designsystem.components.RtListCard
import net.neednot.renttracker.core.designsystem.components.RtListRow
import net.neednot.renttracker.core.designsystem.components.RtTopAppBar
import net.neednot.renttracker.core.designsystem.components.TopBarNavigation
import net.neednot.renttracker.core.designsystem.components.UnimplementedAction
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.members.presentation.components.PermissionEditor
import net.neednot.renttracker.feature.members.presentation.components.PermissionEditorActions
import net.neednot.renttracker.feature.members.presentation.components.RemoveMemberDialog
import net.neednot.renttracker.feature.members.presentation.uistate.MemberHeaderUiState
import net.neednot.renttracker.feature.members.presentation.uistate.MemberPermissionsUiState
import net.neednot.renttracker.feature.members.presentation.viewmodel.MemberPermissionsEvent
import net.neednot.renttracker.feature.members.presentation.viewmodel.MemberPermissionsViewModel
import net.neednot.renttracker.feature.members.presentation.viewmodel.permissionsForm
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.members_conflict_title
import net.neednot.renttracker.resources.members_error_title
import net.neednot.renttracker.resources.members_permissions_title
import net.neednot.renttracker.resources.members_remove_from_group
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
        actions =
            MemberPermissionsActions(
                editor =
                    PermissionEditorActions(
                        onPresetClicked = viewModel::onPresetClicked,
                        onLevelSelected = viewModel::onLevelSelected,
                        onEditOthersChanged = viewModel::onEditOthersChanged,
                    ),
                onSaveClicked = viewModel::onSaveClicked,
                onRemoveClicked = viewModel::onRemoveClicked,
                onRemoveConfirmed = viewModel::onRemoveConfirmed,
                onRemoveDismissed = viewModel::onRemoveDismissed,
                onCloseClicked = onClosed,
            ),
        modifier = modifier,
    )
}

@Composable
internal fun MemberPermissionsContent(
    uiState: MemberPermissionsUiState,
    actions: MemberPermissionsActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { PermissionsTopBar(uiState = uiState, actions = actions) },
    ) { contentPadding ->
        val bodyModifier = Modifier.padding(contentPadding)
        when {
            uiState.isLoading -> {
                LoadingIndicator(modifier = bodyModifier)
            }

            uiState.form == null -> {
                ErrorState(
                    title = stringResource(Res.string.members_error_title),
                    text = uiState.error?.asString(),
                    onRetryClicked = UnimplementedAction,
                    modifier = bodyModifier,
                )
            }

            else -> {
                PermissionsForm(uiState = uiState, actions = actions, modifier = bodyModifier)
            }
        }
    }
    uiState.removeDialog?.let { dialog ->
        RemoveMemberDialog(
            dialog = dialog,
            onConfirmed = actions.onRemoveConfirmed,
            onDismissed = actions.onRemoveDismissed,
        )
    }
}

@Composable
private fun PermissionsTopBar(
    uiState: MemberPermissionsUiState,
    actions: MemberPermissionsActions,
) {
    val title = stringResource(Res.string.members_permissions_title)
    if (uiState.form == null) {
        RtTopAppBar(title = title, navigation = TopBarNavigation.Close, onNavigationClicked = actions.onCloseClicked)
    } else {
        EditorTopBar(
            title = title,
            isSaveEnabled = !uiState.isSaving,
            onSaveClicked = actions.onSaveClicked,
            onCloseClicked = actions.onCloseClicked,
            isSaving = uiState.isSaving,
        )
    }
}

@Composable
private fun PermissionsForm(
    uiState: MemberPermissionsUiState,
    actions: MemberPermissionsActions,
    modifier: Modifier = Modifier,
) {
    val form = uiState.form ?: return
    val isEnabled = !uiState.isSaving
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(ContentLayout.Form.padding),
        verticalArrangement = Arrangement.spacedBy(ContentLayout.Form.spacing),
    ) {
        uiState.conflictBanner?.let { banner ->
            Banner(
                title = stringResource(Res.string.members_conflict_title),
                text = banner.asString(),
                kind = BannerKind.Warning,
            )
        }
        uiState.error?.let { Banner(text = it.asString(), kind = BannerKind.Error) }
        uiState.header?.let { MemberHeader(it) }
        PermissionEditor(form = form, showsCategories = true, actions = actions.editor, enabled = isEnabled)
        if (uiState.canRemove) {
            RtButton(
                text = stringResource(Res.string.members_remove_from_group),
                onClick = actions.onRemoveClicked,
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.Outlined,
                tone = ActionTone.Destructive,
                enabled = isEnabled,
            )
        }
    }
}

@Composable
private fun MemberHeader(header: MemberHeaderUiState) {
    RtListCard(items = listOf(header)) {
        RtListRow(
            title = it.name,
            subtitle = it.email,
            leading = { InitialsAvatar(initials = it.initials) },
        )
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
                    form = owner.permissionsForm(PermissionPreset.Viewer.permissions),
                    canRemove = true,
                ),
            actions = MemberPermissionsActions(),
        )
    }
}
