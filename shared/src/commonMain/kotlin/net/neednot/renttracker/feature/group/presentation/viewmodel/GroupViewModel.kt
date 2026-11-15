package net.neednot.renttracker.feature.group.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.Result
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
import kotlinx.datetime.TimeZone
import net.neednot.renttracker.core.ui.MessageDispatcher
import net.neednot.renttracker.core.ui.UiMessage
import net.neednot.renttracker.core.ui.UiMessageVariant
import net.neednot.renttracker.core.ui.WhileScreenIsVisible
import net.neednot.renttracker.feature.group.domain.GroupError
import net.neednot.renttracker.feature.group.domain.GroupRepository
import net.neednot.renttracker.feature.group.presentation.uistate.GroupUiState

class GroupViewModel(
    private val groupId: String,
    private val groupRepository: GroupRepository,
    private val messageDispatcher: MessageDispatcher,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {
    private val overlay = MutableStateFlow(GroupOverlay())

    val uiState: StateFlow<GroupUiState> =
        combine(groupRepository.observeGroup(groupId), overlay) { group, overlay ->
            group.toGroupUiState(overlay, timeZone)
        }.stateIn(viewModelScope, WhileScreenIsVisible, GroupUiState.Loading)

    private val eventChannel = Channel<GroupEvent>(Channel.BUFFERED)
    val events: Flow<GroupEvent> = eventChannel.receiveAsFlow()

    fun onArchiveClicked() {
        overlay.value = GroupOverlay(dialog = GroupDialog.Archive)
    }

    fun onLeaveClicked() {
        overlay.value = GroupOverlay(dialog = GroupDialog.Leave)
    }

    fun onDialogDismissed() {
        if (!overlay.value.isWorking) overlay.value = GroupOverlay()
    }

    fun onTransferOwnershipClicked() {
        overlay.value = GroupOverlay()
        eventChannel.trySend(GroupEvent.OpenTransferOwnership)
    }

    fun onArchiveConfirmed() {
        work { groupRepository.archiveGroup(groupId).onOk { eventChannel.send(GroupEvent.ReturnHome) } }
    }

    fun onLeaveConfirmed() {
        work(onFailure = ::onLeaveFailed) {
            groupRepository.leaveGroup(groupId).onOk { eventChannel.send(GroupEvent.ReturnHome) }
        }
    }

    fun onUnarchiveClicked() {
        work { groupRepository.unarchiveGroup(groupId) }
    }

    private fun work(
        onFailure: (GroupError) -> Unit = ::showFailure,
        action: suspend () -> Result<Unit, GroupError>,
    ) {
        if (overlay.value.isWorking) return
        overlay.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            action().onErr(onFailure)
            overlay.update { it.copy(isWorking = false) }
        }
    }

    private fun onLeaveFailed(error: GroupError) {
        if (error is GroupError.ValidationFailed) {
            overlay.update { it.copy(dialog = GroupDialog.LeaveOwner) }
        } else {
            showFailure(error)
        }
    }

    private fun showFailure(error: GroupError) {
        overlay.update { it.copy(dialog = null) }
        messageDispatcher.show(UiMessage(error.text(), UiMessageVariant.Error))
    }
}
