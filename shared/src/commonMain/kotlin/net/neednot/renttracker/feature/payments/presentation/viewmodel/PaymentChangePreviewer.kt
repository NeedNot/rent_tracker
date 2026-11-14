package net.neednot.renttracker.feature.payments.presentation.viewmodel

import com.github.michaelbull.result.fold
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.neednot.renttracker.feature.payments.domain.PaymentDraft
import net.neednot.renttracker.feature.payments.domain.PaymentRepository
import net.neednot.renttracker.feature.payments.presentation.uistate.MonthChangesUiState
import net.neednot.renttracker.feature.payments.presentation.uistate.PaymentEditorUiState
import kotlin.time.Duration.Companion.milliseconds

internal class PaymentChangePreviewer(
    private val scope: CoroutineScope,
    private val repository: PaymentRepository,
    private val tenantId: String,
    private val paymentId: String?,
    private val publish: (MonthChangesUiState?) -> Unit,
) {
    private var saved: PaymentDraft? = null
    private var previewed: ProposedPaymentChange? = null
    private var job: Job? = null

    fun restart(
        savedDetails: PaymentDraft?,
        state: PaymentEditorUiState,
    ) {
        saved = savedDetails
        previewed = null
        refresh(state)
    }

    fun refresh(state: PaymentEditorUiState) {
        val change = state.proposedChange(saved)
        if (change == previewed) return
        previewed = change
        job?.cancel()
        publish(null)
        val id = paymentId
        job = if (change == null || id == null) null else scope.launch { preview(id, change) }
    }

    private suspend fun preview(
        id: String,
        change: ProposedPaymentChange,
    ) {
        delay(DEBOUNCE)
        val monthChanges =
            repository
                .previewPaymentChange(tenantId, id, change.amountCents, change.dateReceived)
                .fold(success = { it.toUiState() }, failure = { MonthChangesUiState.CheckFailed })
        if (previewed == change) publish(monthChanges)
    }

    private companion object {
        val DEBOUNCE = 300.milliseconds
    }
}
