package net.neednot.renttracker.feature.members.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.neednot.renttracker.core.ui.WhileScreenIsVisible
import net.neednot.renttracker.core.ui.firstNameOf
import net.neednot.renttracker.feature.members.domain.MembersRepository
import net.neednot.renttracker.feature.members.presentation.uistate.TransferConfirmUiState
import net.neednot.renttracker.feature.members.presentation.uistate.TransferOwnershipUiState

class TransferOwnershipViewModel(
    private val groupId: String,
    private val membersRepository: MembersRepository,
) : ViewModel() {
    private val choice = MutableStateFlow(TransferOwnershipUiState())

    val uiState: StateFlow<TransferOwnershipUiState> =
        combine(membersRepository.observeMembers(groupId), choice) { members, choice ->
            members.toTransferOwnershipUiState(choice)
        }.stateIn(viewModelScope, WhileScreenIsVisible, TransferOwnershipUiState())

    private val eventChannel = Channel<TransferOwnershipEvent>(Channel.BUFFERED)
    val events: Flow<TransferOwnershipEvent> = eventChannel.receiveAsFlow()

    fun onCandidateClicked(membershipId: String) {
        val state = uiState.value
        val candidate = state.candidates.firstOrNull { it.membershipId == membershipId } ?: return
        choice.update {
            it.copy(
                error = null,
                confirm =
                    TransferConfirmUiState(
                        membershipId = candidate.membershipId,
                        newOwnerName = candidate.name,
                        newOwnerFirstName = firstNameOf(candidate.name),
                        groupName = state.groupName,
                    ),
            )
        }
    }

    fun onConfirmDismissed() {
        choice.update { if (it.confirm?.isWorking == true) it else it.copy(confirm = null) }
    }

    fun onTransferConfirmed() {
        val confirm = choice.value.confirm
        if (confirm == null || confirm.isWorking) return
        choice.update { it.copy(confirm = confirm.copy(isWorking = true), error = null) }
        viewModelScope.launch {
            membersRepository
                .transferOwnership(groupId, confirm.membershipId)
                .onOk { eventChannel.send(TransferOwnershipEvent.OwnershipTransferred) }
                .onErr { error -> choice.update { it.copy(confirm = null, error = error.text()) } }
        }
    }
}
