package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.neednot.renttracker.core.ui.MessageDispatcher
import net.neednot.renttracker.core.ui.UiMessage
import net.neednot.renttracker.core.ui.UiMessageVariant
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.WhileScreenIsVisible
import net.neednot.renttracker.feature.tenants.domain.PendingPaymentRepository
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingPaymentUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingSheetAction
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.pending_removed

@OptIn(ExperimentalCoroutinesApi::class)
class PendingPaymentViewModel(
    openedClientId: String?,
    private val repository: PendingPaymentRepository,
    private val messageDispatcher: MessageDispatcher,
) : ViewModel() {
    private val selectedClientId = MutableStateFlow(openedClientId)
    private val isCancelDialogOpen = MutableStateFlow(false)
    private val isWorking = MutableStateFlow(false)

    val uiState: StateFlow<PendingPaymentUiState> =
        combine(
            selectedClientId.flatMapLatest { clientId ->
                if (clientId == null) flowOf(null) else repository.observeReview(clientId)
            },
            isCancelDialogOpen,
            isWorking,
        ) { review, dialogOpen, working ->
            val sheet = review?.toSheetUiState()
            PendingPaymentUiState(sheet = sheet, isCancelDialogOpen = dialogOpen && sheet != null, isWorking = working)
        }.stateIn(viewModelScope, WhileScreenIsVisible, PendingPaymentUiState())

    fun onPendingPaymentClicked(clientId: String) {
        isCancelDialogOpen.value = false
        selectedClientId.value = clientId
    }

    fun onSheetDismissed() {
        isCancelDialogOpen.value = false
        selectedClientId.value = null
    }

    fun onActionClicked(action: PendingSheetAction) {
        val clientId = selectedClientId.value ?: return
        when (action) {
            PendingSheetAction.Edit -> onSheetDismissed()
            PendingSheetAction.Cancel -> isCancelDialogOpen.value = true
            PendingSheetAction.SaveAnyway, PendingSheetAction.SendAnyway -> confirm(clientId)
            PendingSheetAction.CancelPayment -> discard(clientId, RemovalNotice.Shown)
            PendingSheetAction.Dismiss -> discard(clientId, RemovalNotice.Silent)
        }
    }

    fun onCancelConfirmed() {
        val clientId = selectedClientId.value ?: return
        discard(clientId, RemovalNotice.Shown)
    }

    fun onCancelDialogDismissed() {
        isCancelDialogOpen.value = false
    }

    private fun confirm(clientId: String) {
        runWork {
            repository
                .confirm(clientId)
                .onOk { onSheetDismissed() }
                .onErr { error -> messageDispatcher.show(UiMessage(error.text(), UiMessageVariant.Error)) }
        }
    }

    private fun discard(
        clientId: String,
        notice: RemovalNotice,
    ) {
        runWork {
            repository.discard(clientId)
            onSheetDismissed()
            if (notice == RemovalNotice.Shown) {
                messageDispatcher.show(UiMessage(UiText.Resource(Res.string.pending_removed)))
            }
        }
    }

    private fun runWork(work: suspend () -> Unit) {
        if (isWorking.value) return
        isWorking.value = true
        viewModelScope.launch {
            work()
            isWorking.value = false
        }
    }

    private enum class RemovalNotice {
        Shown,
        Silent,
    }
}
