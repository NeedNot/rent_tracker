package net.neednot.renttracker.feature.members.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onErr
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import net.neednot.renttracker.core.ui.MessageDispatcher
import net.neednot.renttracker.core.ui.UiMessage
import net.neednot.renttracker.core.ui.UiMessageVariant
import net.neednot.renttracker.core.ui.WhileScreenIsVisible
import net.neednot.renttracker.feature.members.domain.MembersOverview
import net.neednot.renttracker.feature.members.domain.MembersRepository
import net.neednot.renttracker.feature.members.presentation.uistate.MembersUiState

class MembersViewModel(
    groupId: String,
    private val membersRepository: MembersRepository,
    private val messageDispatcher: MessageDispatcher,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {
    private val removal = RemoveMemberFlow(viewModelScope, membersRepository)
    private var overview: MembersOverview? = null
    private val revokingInviteIds = mutableSetOf<String>()

    val uiState: StateFlow<MembersUiState> =
        combine(
            membersRepository.observeMembers(groupId).onEach { overview = it.value },
            removal.dialog,
        ) { members, removeDialog -> members.toMembersUiState(timeZone, removeDialog) }
            .stateIn(viewModelScope, WhileScreenIsVisible, MembersUiState.Loading)

    fun onRemoveClicked(membershipId: String) {
        val shown = overview ?: return
        val member = shown.member(membershipId) ?: return
        removal.open(member, shown.groupName)
    }

    fun onRemoveConfirmed() {
        removal.confirm()
    }

    fun onRemoveDismissed() {
        removal.dismiss()
    }

    fun onRevokeClicked(inviteId: String) {
        if (!revokingInviteIds.add(inviteId)) return
        viewModelScope.launch {
            membersRepository
                .revokeInvite(inviteId)
                .onErr { error -> messageDispatcher.show(UiMessage(error.text(), UiMessageVariant.Error)) }
            revokingInviteIds.remove(inviteId)
        }
    }
}
