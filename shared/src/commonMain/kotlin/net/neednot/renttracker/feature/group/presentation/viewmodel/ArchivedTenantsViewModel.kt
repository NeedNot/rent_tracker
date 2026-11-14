package net.neednot.renttracker.feature.group.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import net.neednot.renttracker.core.ui.MessageDispatcher
import net.neednot.renttracker.core.ui.WhileScreenIsVisible
import net.neednot.renttracker.feature.group.domain.GroupRepository
import net.neednot.renttracker.feature.group.presentation.uistate.ArchivedTenantsUiState

class ArchivedTenantsViewModel(
    groupId: String,
    groupRepository: GroupRepository,
    messageDispatcher: MessageDispatcher,
) : ViewModel() {
    private val eventChannel = Channel<GroupEvent>(Channel.BUFFERED)
    val events: Flow<GroupEvent> = eventChannel.receiveAsFlow()

    val uiState: StateFlow<ArchivedTenantsUiState> =
        groupRepository
            .observeGroup(groupId)
            .announcingGroupGone(messageDispatcher, eventChannel)
            .map { it.toArchivedTenantsUiState() }
            .stateIn(viewModelScope, WhileScreenIsVisible, ArchivedTenantsUiState.Loading)
}
