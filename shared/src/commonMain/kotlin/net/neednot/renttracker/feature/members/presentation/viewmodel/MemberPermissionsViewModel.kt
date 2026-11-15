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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.neednot.renttracker.core.access.CallerAccess
import net.neednot.renttracker.core.access.PermissionPreset
import net.neednot.renttracker.core.data.AccessLevel
import net.neednot.renttracker.core.data.PermissionSet
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.initialsOf
import net.neednot.renttracker.feature.members.domain.Member
import net.neednot.renttracker.feature.members.domain.MembersError
import net.neednot.renttracker.feature.members.domain.MembersOverview
import net.neednot.renttracker.feature.members.domain.MembersRepository
import net.neednot.renttracker.feature.members.presentation.uistate.MemberHeaderUiState
import net.neednot.renttracker.feature.members.presentation.uistate.MemberPermissionsUiState
import net.neednot.renttracker.feature.members.presentation.uistate.PermissionArea
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.members_error_member_gone

class MemberPermissionsViewModel(
    private val groupId: String,
    private val membershipId: String,
    private val membersRepository: MembersRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(MemberPermissionsUiState())
    val uiState: StateFlow<MemberPermissionsUiState> = mutableUiState.asStateFlow()

    private val eventChannel = Channel<MemberPermissionsEvent>(Channel.BUFFERED)
    val events: Flow<MemberPermissionsEvent> = eventChannel.receiveAsFlow()

    private val removal =
        RemoveMemberFlow(viewModelScope, membersRepository) { eventChannel.send(MemberPermissionsEvent.MemberRemoved) }
    private var access: CallerAccess? = null
    private var savedMember: Member? = null
    private var groupName = ""

    init {
        removal.dialog
            .onEach { dialog ->
                mutableUiState.update { it.copy(removeDialog = dialog) }
            }.launchIn(viewModelScope)
        viewModelScope.launch { show(membersRepository.cachedOrFirstSyncedMembers(groupId)) }
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

    fun onSaveClicked() {
        val state = uiState.value
        val form = state.form
        val member = savedMember
        if (form == null || member == null || state.isSaving) return
        mutableUiState.update { it.copy(isSaving = true, error = null, conflictBanner = null) }
        viewModelScope.launch {
            membersRepository
                .updatePermissions(member.membershipId, member.version, form.permissions)
                .onOk { eventChannel.send(MemberPermissionsEvent.PermissionsSaved) }
                .onErr { error -> onSaveFailed(error) }
            mutableUiState.update { it.copy(isSaving = false) }
        }
    }

    fun onRemoveClicked() {
        val member = savedMember ?: return
        removal.open(member, groupName)
    }

    fun onRemoveConfirmed() {
        removal.confirm()
    }

    fun onRemoveDismissed() {
        removal.dismiss()
    }

    private suspend fun onSaveFailed(error: MembersError) {
        if (error is MembersError.EditConflict) {
            mutableUiState.update { it.copy(conflictBanner = membersConflictBanner(error.editedByName)) }
            show(membersRepository.membersFromServer(groupId))
        } else {
            mutableUiState.update { it.copy(error = error.text()) }
        }
    }

    private fun changePermissions(change: (PermissionSet) -> PermissionSet) {
        val access = access ?: return
        val saved = savedMember?.permissions ?: return
        mutableUiState.update { state ->
            val form = state.form ?: return@update state
            state.copy(form = access.permissionsForm(change(form.permissions), saved), error = null)
        }
    }

    private fun show(snapshot: Synced<MembersOverview?, MembersError>) {
        val overview = snapshot.value
        val member = overview?.member(membershipId)
        if (overview == null || member == null) {
            val error =
                if (overview ==
                    null
                ) {
                    snapshot.loadErrorText()
                } else {
                    UiText.Resource(Res.string.members_error_member_gone)
                }
            mutableUiState.update { it.copy(isLoading = false, form = null, error = error) }
            return
        }
        val callerAccess = overview.access
        access = callerAccess
        savedMember = member
        groupName = overview.groupName
        mutableUiState.update {
            it.copy(
                isLoading = false,
                header = MemberHeaderUiState(initialsOf(member.displayName), member.displayName, member.email),
                form = callerAccess.permissionsForm(member.permissions, saved = member.permissions),
                canRemove = callerAccess.canRemove(member.role, member.permissions, member.accountId),
            )
        }
    }
}
