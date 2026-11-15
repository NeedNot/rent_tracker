package net.neednot.renttracker.feature.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.TimeZone
import net.neednot.renttracker.core.ui.WhileScreenIsVisible
import net.neednot.renttracker.feature.home.domain.HomeRepository
import net.neednot.renttracker.feature.home.presentation.uistate.ArchivedGroupsUiState

class ArchivedGroupsViewModel(
    homeRepository: HomeRepository,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {
    val uiState: StateFlow<ArchivedGroupsUiState> =
        homeRepository
            .observeArchivedGroups()
            .map { it.toArchivedGroupsUiState(timeZone) }
            .stateIn(viewModelScope, WhileScreenIsVisible, ArchivedGroupsUiState.Loading)
}
