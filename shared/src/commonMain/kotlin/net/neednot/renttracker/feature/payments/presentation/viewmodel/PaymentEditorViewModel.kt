package net.neednot.renttracker.feature.payments.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.fold
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.data.PaymentMethod
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.ui.MessageDispatcher
import net.neednot.renttracker.core.ui.offlineBannerText
import net.neednot.renttracker.feature.payments.domain.DuplicatePolicy
import net.neednot.renttracker.feature.payments.domain.PaymentEntryOverview
import net.neednot.renttracker.feature.payments.domain.PaymentError
import net.neednot.renttracker.feature.payments.domain.PaymentRepository
import net.neednot.renttracker.feature.payments.domain.PaymentSyncAlerts
import net.neednot.renttracker.feature.payments.presentation.uistate.PaymentEditorField
import net.neednot.renttracker.feature.payments.presentation.uistate.PaymentEditorUiState
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class PaymentEditorViewModel(
    private val tenantId: String,
    private val paymentId: String?,
    private val pendingClientId: String?,
    private val paymentRepository: PaymentRepository,
    syncAlerts: PaymentSyncAlerts,
    messageDispatcher: MessageDispatcher,
) : ViewModel() {
    private val savedOnPhoneNotice = SavedOnPhoneNotice(syncAlerts, messageDispatcher)
    private val mutableUiState =
        MutableStateFlow(PaymentEditorUiState(isNewPayment = paymentId == null && pendingClientId == null))
    val uiState: StateFlow<PaymentEditorUiState> = mutableUiState.asStateFlow()

    private val eventChannel = Channel<PaymentEditorEvent>(Channel.BUFFERED)
    val events: Flow<PaymentEditorEvent> = eventChannel.receiveAsFlow()

    private val clientId = Uuid.random().toString()
    private var expectedVersion: Long? = null
    private var hasTriedSaving = false
    private val previewer =
        PaymentChangePreviewer(viewModelScope, paymentRepository, tenantId, paymentId) { changes ->
            mutableUiState.update { it.copy(monthChanges = changes) }
        }

    init {
        viewModelScope.launch {
            paymentRepository.observeFreshness(tenantId).collect { freshness ->
                mutableUiState.update { it.copy(offlineBanner = freshness.offlineBannerText()) }
            }
        }
        viewModelScope.launch {
            if (pendingClientId == null) {
                show(paymentRepository.cachedOrFirstSyncedEntry(tenantId, paymentId))
            } else {
                val pending = paymentRepository.observePendingPayment(pendingClientId).first()
                mutableUiState.update { it.showingPending(pending) }
            }
        }
    }

    fun onFieldChanged(
        field: PaymentEditorField,
        text: String,
    ) {
        mutableUiState.update {
            when (field) {
                PaymentEditorField.Amount -> it.copy(amount = text)
                PaymentEditorField.Note -> it.copy(note = text)
                PaymentEditorField.LateFeeAmount -> it.copy(lateFeeOffer = it.lateFeeOffer?.copy(amount = text))
                PaymentEditorField.Date -> it
            }.revalidated(hasTriedSaving)
        }
        previewer.refresh(uiState.value)
    }

    fun onDateChanged(date: LocalDate) {
        mutableUiState.update { it.copy(date = date).revalidated(hasTriedSaving) }
        previewer.refresh(uiState.value)
    }

    fun onMethodClicked(method: PaymentMethod) {
        mutableUiState.update { it.copy(method = if (it.method == method) null else method) }
    }

    fun onLateFeeToggled(isChecked: Boolean) {
        mutableUiState.update {
            it.copy(lateFeeOffer = it.lateFeeOffer?.copy(isChecked = isChecked)).revalidated(hasTriedSaving)
        }
    }

    fun onSaveClicked() {
        save(DuplicatePolicy.AskFirst)
    }

    fun onDuplicateConfirmed() {
        save(DuplicatePolicy.SaveAnyway)
    }

    fun onDuplicateDismissed() {
        mutableUiState.update { it.copy(duplicate = null) }
    }

    fun onSyncAlertsAnswered(answer: SyncAlertsAnswer) {
        mutableUiState.update { it.copy(isSyncAlertsExplainerShown = false) }
        savedOnPhoneNotice.explained(answer)
        eventChannel.trySend(PaymentEditorEvent.PaymentSaved)
    }

    private fun save(duplicatePolicy: DuplicatePolicy) {
        val state = uiState.value
        if (state.isSaving || state.isLoading) return
        hasTriedSaving = true
        state.validate().fold(
            success = { form ->
                mutableUiState.update {
                    it.copy(
                        isSaving = true,
                        error = null,
                        conflictBanner = null,
                        duplicate = null,
                    )
                }
                val request = PaymentSaveRequest(tenantId, paymentId, pendingClientId, clientId, expectedVersion)
                viewModelScope.launch {
                    paymentRepository
                        .save(request, form, duplicatePolicy)
                        .onOk { outcome ->
                            when {
                                outcome == PaymentSaveOutcome.Synced -> {
                                    eventChannel.send(PaymentEditorEvent.PaymentSaved)
                                }

                                savedOnPhoneNotice.needsExplanation() -> {
                                    mutableUiState.update { it.copy(isSyncAlertsExplainerShown = true) }
                                }

                                else -> {
                                    savedOnPhoneNotice.announce()
                                    eventChannel.send(PaymentEditorEvent.PaymentSaved)
                                }
                            }
                        }.onErr { error ->
                            mutableUiState.update { it.afterSaveFailure(error) }
                            if (error is PaymentError.EditConflict) {
                                show(paymentRepository.entryFromServer(tenantId, paymentId))
                            }
                        }
                    mutableUiState.update { it.copy(isSaving = false) }
                }
            },
            failure = { errors -> mutableUiState.update { it.copy(fieldErrors = errors) } },
        )
    }

    private fun show(snapshot: Synced<PaymentEntryOverview?, PaymentError>) {
        val overview = snapshot.value?.takeIf { paymentId == null || it.payment != null }
        expectedVersion = overview?.payment?.version
        mutableUiState.update {
            if (overview == null) {
                it.copy(isLoading = false, error = it.error ?: snapshot.loadError().text())
            } else {
                it.showing(overview)
            }
        }
        previewer.restart(overview?.payment?.details, uiState.value)
    }
}
