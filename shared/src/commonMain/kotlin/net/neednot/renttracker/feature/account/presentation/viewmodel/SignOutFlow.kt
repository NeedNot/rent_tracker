package net.neednot.renttracker.feature.account.presentation.viewmodel

import com.github.michaelbull.result.onErr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.neednot.renttracker.feature.account.domain.AccountError
import net.neednot.renttracker.feature.account.domain.AccountRepository

internal class SignOutFlow(
    private val scope: CoroutineScope,
    private val accountRepository: AccountRepository,
    private val onFailure: (AccountError) -> Unit,
) {
    private val mutableUnsyncedPayments = MutableStateFlow<Int?>(null)
    val unsyncedPayments: StateFlow<Int?> = mutableUnsyncedPayments.asStateFlow()

    fun request() {
        scope.launch {
            mutableUnsyncedPayments.value = accountRepository.observeUnsyncedPaymentCount().first()
        }
    }

    fun answer(answer: SignOutWarningAnswer) {
        mutableUnsyncedPayments.value = null
        if (answer == SignOutWarningAnswer.SignOutAnyway) scope.launch { signOut() }
    }

    private suspend fun signOut() {
        accountRepository.signOut().onErr(onFailure)
    }
}
