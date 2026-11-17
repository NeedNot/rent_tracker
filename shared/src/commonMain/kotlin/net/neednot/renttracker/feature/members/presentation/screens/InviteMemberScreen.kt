package net.neednot.renttracker.feature.members.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import net.neednot.renttracker.core.access.CallerAccess
import net.neednot.renttracker.core.access.PermissionPreset
import net.neednot.renttracker.core.data.MembershipRole
import net.neednot.renttracker.core.designsystem.components.Banner
import net.neednot.renttracker.core.designsystem.components.BannerKind
import net.neednot.renttracker.core.designsystem.components.ButtonVariant
import net.neednot.renttracker.core.designsystem.components.ContentLayout
import net.neednot.renttracker.core.designsystem.components.EditorTopBar
import net.neednot.renttracker.core.designsystem.components.ErrorState
import net.neednot.renttracker.core.designsystem.components.FormTextField
import net.neednot.renttracker.core.designsystem.components.LoadingIndicator
import net.neednot.renttracker.core.designsystem.components.RtButton
import net.neednot.renttracker.core.designsystem.components.UnimplementedAction
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.members.presentation.components.PermissionEditor
import net.neednot.renttracker.feature.members.presentation.components.PermissionEditorActions
import net.neednot.renttracker.feature.members.presentation.uistate.InviteMemberUiState
import net.neednot.renttracker.feature.members.presentation.viewmodel.InviteMemberEvent
import net.neednot.renttracker.feature.members.presentation.viewmodel.InviteMemberViewModel
import net.neednot.renttracker.feature.members.presentation.viewmodel.permissionsForm
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.members_customize
import net.neednot.renttracker.resources.members_error_title
import net.neednot.renttracker.resources.members_invite_email
import net.neednot.renttracker.resources.members_invite_email_hint
import net.neednot.renttracker.resources.members_invite_email_placeholder
import net.neednot.renttracker.resources.members_invite_limited
import net.neednot.renttracker.resources.members_invite_send
import net.neednot.renttracker.resources.members_invite_title
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
        actions =
            InviteMemberActions(
                editor =
                    PermissionEditorActions(
                        onPresetClicked = viewModel::onPresetClicked,
                        onLevelSelected = viewModel::onLevelSelected,
                        onEditOthersChanged = viewModel::onEditOthersChanged,
                    ),
                onEmailChanged = viewModel::onEmailChanged,
                onCustomizeClicked = viewModel::onCustomizeClicked,
                onSendClicked = viewModel::onSendClicked,
                onCloseClicked = onClosed,
            ),
        modifier = modifier,
    )
}

@Composable
internal fun InviteMemberContent(
    uiState: InviteMemberUiState,
    actions: InviteMemberActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            EditorTopBar(
                title = stringResource(Res.string.members_invite_title),
                isSaveEnabled = uiState.form != null,
                onSaveClicked = actions.onSendClicked,
                onCloseClicked = actions.onCloseClicked,
                isSaving = uiState.isSending,
                saveLabel = stringResource(Res.string.members_invite_send),
            )
        },
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
                InviteForm(uiState = uiState, actions = actions, modifier = bodyModifier)
            }
        }
    }
}

@Composable
private fun InviteForm(
    uiState: InviteMemberUiState,
    actions: InviteMemberActions,
    modifier: Modifier = Modifier,
) {
    val form = uiState.form ?: return
    val isEnabled = !uiState.isSending
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(ContentLayout.Form.padding),
        verticalArrangement = Arrangement.spacedBy(ContentLayout.Form.spacing),
    ) {
        uiState.error?.let { Banner(text = it.asString(), kind = BannerKind.Error) }
        FormTextField(
            label = stringResource(Res.string.members_invite_email),
            text = uiState.email,
            onTextChanged = actions.onEmailChanged,
            error = uiState.emailError,
            hint = stringResource(Res.string.members_invite_email_hint),
            placeholder = stringResource(Res.string.members_invite_email_placeholder),
            enabled = isEnabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
        )
        PermissionEditor(
            form = form,
            showsCategories = uiState.isCustomizeExpanded,
            actions = actions.editor,
            presetHint = uiState.limitedInviterName?.let { stringResource(Res.string.members_invite_limited, it) },
            enabled = isEnabled,
        )
        if (!uiState.isCustomizeExpanded) {
            RtButton(
                text = stringResource(Res.string.members_customize),
                onClick = actions.onCustomizeClicked,
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.Text,
                enabled = isEnabled,
            )
        }
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
                    form = access.permissionsForm(PermissionPreset.Bookkeeper.permissions),
                ),
            actions = InviteMemberActions(),
        )
    }
}
