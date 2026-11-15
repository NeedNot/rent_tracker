package net.neednot.renttracker.feature.group.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import net.neednot.renttracker.core.ui.WhileScreenIsVisible
import net.neednot.renttracker.feature.group.domain.GroupFreshnessRepository
import net.neednot.renttracker.feature.group.domain.GroupRepository
import net.neednot.renttracker.feature.group.presentation.uistate.ArchivedTenantsUiState

class ArchivedTenantsViewModel(
    groupId: String,
    groupRepository: GroupRepository,
    groupFreshnessRepository: GroupFreshnessRepository,
) : ViewModel() {
    val uiState: StateFlow<ArchivedTenantsUiState> =
        combine(
            groupRepository.observeGroup(groupId),
            groupFreshnessRepository.observeFreshness(groupId),
        ) { group, freshness -> group.toArchivedTenantsUiState(freshness) }
            .stateIn(viewModelScope, WhileScreenIsVisible, ArchivedTenantsUiState.Loading())
}
