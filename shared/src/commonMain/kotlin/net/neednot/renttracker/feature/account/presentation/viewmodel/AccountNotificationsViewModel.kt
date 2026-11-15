package net.neednot.renttracker.feature.account.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import net.neednot.renttracker.core.ui.MessageDispatcher
import net.neednot.renttracker.core.ui.UiMessage
import net.neednot.renttracker.core.ui.UiMessageVariant
import net.neednot.renttracker.core.ui.WhileScreenIsVisible
import net.neednot.renttracker.feature.account.domain.AccountError
import net.neednot.renttracker.feature.account.domain.NotificationSettingsRepository
import net.neednot.renttracker.feature.account.presentation.uistate.NotificationsSectionUiState

class AccountNotificationsViewModel(
    notificationSettingsRepository: NotificationSettingsRepository,
    private val messageDispatcher: MessageDispatcher,
) : ViewModel() {
    private val settings = NotificationSettingsFlow(viewModelScope, notificationSettingsRepository, ::showError)

    val uiState: StateFlow<NotificationsSectionUiState> =
        settings.section.stateIn(viewModelScope, WhileScreenIsVisible, NotificationsSectionUiState.Hidden)

    fun onGroupToggled(groupId: String) {
        settings.toggle(groupId)
    }

    private fun showError(error: AccountError) {
        messageDispatcher.show(UiMessage(error.text(), UiMessageVariant.Error))
    }
}
