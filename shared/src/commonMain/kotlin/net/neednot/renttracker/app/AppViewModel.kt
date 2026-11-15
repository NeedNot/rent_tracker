package net.neednot.renttracker.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.neednot.renttracker.core.auth.AuthSessionState
import net.neednot.renttracker.core.data.AccessLossReason
import net.neednot.renttracker.core.data.LostGroupAccess
import net.neednot.renttracker.core.data.PendingPaymentLink
import net.neednot.renttracker.core.ui.MessageDispatcher
import net.neednot.renttracker.core.ui.UiMessage
import net.neednot.renttracker.core.ui.UiMessageVariant
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.auth.domain.AuthRepository
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.group_error_not_found
import net.neednot.renttracker.resources.lost_access_removed

class AppViewModel(
    authRepository: AuthRepository,
    lostGroupAccess: Flow<LostGroupAccess>,
    syncPendingPaymentsWhileOnline: suspend () -> Unit,
    pendingPaymentLinks: Flow<PendingPaymentLink>,
    private val messageDispatcher: MessageDispatcher,
) : ViewModel() {
    val sessionState: StateFlow<AuthSessionState> = authRepository.sessionState

    private val eventChannel = Channel<AppEvent>(Channel.BUFFERED)
    val events: Flow<AppEvent> = eventChannel.receiveAsFlow()

    init {
        viewModelScope.launch { syncPendingPaymentsWhileOnline() }
        viewModelScope.launch {
            pendingPaymentLinks.collect { link ->
                eventChannel.send(AppEvent.OpenPendingPayment(link.tenantId, link.clientId))
            }
        }
        viewModelScope.launch {
            lostGroupAccess.collect { loss ->
                messageDispatcher.show(loss.message())
                eventChannel.send(AppEvent.ReturnHome)
            }
        }
    }

    private fun LostGroupAccess.message(): UiMessage =
        when (reason) {
            AccessLossReason.Removed -> {
                UiMessage(text = UiText.Resource(Res.string.lost_access_removed, listOf(groupName)))
            }

            AccessLossReason.GroupGone -> {
                UiMessage(text = UiText.Resource(Res.string.group_error_not_found), variant = UiMessageVariant.Error)
            }
        }
}
