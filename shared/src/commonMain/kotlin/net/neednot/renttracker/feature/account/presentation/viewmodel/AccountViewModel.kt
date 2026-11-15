package net.neednot.renttracker.feature.account.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onErr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import net.neednot.renttracker.core.ui.MessageDispatcher
import net.neednot.renttracker.core.ui.UiMessage
import net.neednot.renttracker.core.ui.UiMessageVariant
import net.neednot.renttracker.core.ui.WhileScreenIsVisible
import net.neednot.renttracker.feature.account.domain.AccountError
import net.neednot.renttracker.feature.account.domain.AccountRepository
import net.neednot.renttracker.feature.account.presentation.uistate.AccountUiState

class AccountViewModel(
    private val accountRepository: AccountRepository,
    private val messageDispatcher: MessageDispatcher,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {
    private val nameEditing = EditNameFlow(viewModelScope, accountRepository, ::showError)
    private val isDeleteDialogShown = MutableStateFlow(false)
    private val signOutFlow = SignOutFlow(viewModelScope, accountRepository, ::showError)
    private var currentName = ""
    private var isRequestingDeletion = false

    val uiState: StateFlow<AccountUiState> =
        combine(
            accountRepository.observeProfile(),
            nameEditing.dialog,
            isDeleteDialogShown,
            signOutFlow.unsyncedPayments,
        ) { profile, nameDialog, deleteDialogShown, unsynced ->
            currentName = profile.value?.displayName.orEmpty()
            profile.toAccountUiState(timeZone, nameDialog, deleteDialogShown, unsynced)
        }.stateIn(viewModelScope, WhileScreenIsVisible, AccountUiState.Loading)

    fun onEditNameClicked() {
        nameEditing.open(currentName)
    }

    fun onNameChanged(name: String) {
        nameEditing.change(name)
    }

    fun onEditNameDismissed() {
        nameEditing.dismiss()
    }

    fun onEditNameSaved() {
        nameEditing.save()
    }

    fun onRequestDeletionClicked() {
        isDeleteDialogShown.value = true
    }

    fun onRequestDeletionDismissed() {
        isDeleteDialogShown.value = false
    }

    fun onRequestDeletionConfirmed() {
        isDeleteDialogShown.value = false
        if (isRequestingDeletion) return
        isRequestingDeletion = true
        viewModelScope.launch {
            accountRepository.requestDeletion().onErr(::showError)
            isRequestingDeletion = false
        }
    }

    fun onSignOutClicked() {
        signOutFlow.request()
    }

    fun onSignOutWarningAnswered(answer: SignOutWarningAnswer) {
        signOutFlow.answer(answer)
    }

    private fun showError(error: AccountError) {
        messageDispatcher.show(UiMessage(error.text(), UiMessageVariant.Error))
    }
}
