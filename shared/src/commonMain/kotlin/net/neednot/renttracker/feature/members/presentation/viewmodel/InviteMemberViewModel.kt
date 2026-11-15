package net.neednot.renttracker.feature.members.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.neednot.renttracker.core.access.CallerAccess
import net.neednot.renttracker.core.access.PermissionPreset
import net.neednot.renttracker.core.data.AccessLevel
import net.neednot.renttracker.core.data.PermissionSet
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.ui.MessageDispatcher
import net.neednot.renttracker.core.ui.UiMessage
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.members.domain.MembersError
import net.neednot.renttracker.feature.members.domain.MembersOverview
import net.neednot.renttracker.feature.members.domain.MembersRepository
import net.neednot.renttracker.feature.members.presentation.uistate.InviteMemberUiState
import net.neednot.renttracker.feature.members.presentation.uistate.PermissionArea
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.members_invite_sent

class InviteMemberViewModel(
    private val groupId: String,
    private val membersRepository: MembersRepository,
    private val messageDispatcher: MessageDispatcher,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(InviteMemberUiState())
    val uiState: StateFlow<InviteMemberUiState> = mutableUiState.asStateFlow()

    private val eventChannel = Channel<InviteMemberEvent>(Channel.BUFFERED)
    val events: Flow<InviteMemberEvent> = eventChannel.receiveAsFlow()

    private var access: CallerAccess? = null
    private var hasTriedSending = false

    init {
        viewModelScope.launch { show(membersRepository.cachedOrFirstSyncedMembers(groupId)) }
    }

    fun onEmailChanged(text: String) {
        mutableUiState.update {
            it.copy(
                email = text,
                emailError = inviteEmailError(text, hasTriedSending),
                error = null,
            )
        }
    }

    fun onPresetClicked(preset: PermissionPreset) {
        changePermissions { preset.permissions }
    }

    fun onLevelSelected(
        area: PermissionArea,
        level: AccessLevel,
    ) {
        changePermissions { it.withLevel(area, level) }
    }

    fun onEditOthersChanged(isOn: Boolean) {
        changePermissions { it.copy(editOthers = isOn) }
    }

    fun onCustomizeClicked() {
        mutableUiState.update { it.copy(isCustomizeExpanded = !it.isCustomizeExpanded) }
    }

    fun onSendClicked() {
        val state = uiState.value
        val form = state.form
        if (form == null || state.isSending) return
        hasTriedSending = true
        val emailError = inviteEmailError(state.email, includesMissing = true)
        if (emailError != null) {
            mutableUiState.update { it.copy(emailError = emailError) }
            return
        }
        val email = state.email.trim()
        mutableUiState.update { it.copy(isSending = true, error = null) }
        viewModelScope.launch {
            membersRepository
                .sendInvite(groupId, email, form.permissions)
                .onOk {
                    messageDispatcher.show(UiMessage(UiText.Resource(Res.string.members_invite_sent, listOf(email))))
                    eventChannel.send(InviteMemberEvent.InviteSent)
                }.onErr { error -> onSendFailed(error) }
            mutableUiState.update { it.copy(isSending = false) }
        }
    }

    private fun onSendFailed(error: MembersError) {
        mutableUiState.update {
            if (error is MembersError.ValidationFailed && error.field == EMAIL_FIELD) {
                it.copy(emailError = UiText.Plain(error.message))
            } else {
                it.copy(error = error.text())
            }
        }
    }

    private fun changePermissions(change: (PermissionSet) -> PermissionSet) {
        val access = access ?: return
        mutableUiState.update { state ->
            val form = state.form ?: return@update state
            state.copy(form = access.permissionsForm(change(form.permissions)), error = null)
        }
    }

    private fun show(snapshot: Synced<MembersOverview?, MembersError>) {
        val overview = snapshot.value
        if (overview == null) {
            mutableUiState.update { it.copy(isLoading = false, error = snapshot.loadErrorText()) }
            return
        }
        val callerAccess = overview.access
        access = callerAccess
        mutableUiState.update {
            it.copy(
                isLoading = false,
                limitedInviterName = overview.caller?.displayName?.takeIf { callerAccess.limitsInvites() },
                form = callerAccess.permissionsForm(callerAccess.defaultInvitePreset().permissions),
            )
        }
    }
}
