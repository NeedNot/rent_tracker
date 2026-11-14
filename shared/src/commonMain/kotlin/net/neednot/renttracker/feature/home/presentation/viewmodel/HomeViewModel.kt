package net.neednot.renttracker.feature.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.neednot.renttracker.core.ui.WhileScreenIsVisible
import net.neednot.renttracker.feature.home.domain.HomeRepository
import net.neednot.renttracker.feature.home.presentation.uistate.HomeUiState

class HomeViewModel(
    homeRepository: HomeRepository,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> =
        homeRepository
            .observeGroups()
            .map { it.toHomeUiState() }
            .stateIn(viewModelScope, WhileScreenIsVisible, HomeUiState.Loading)
}
