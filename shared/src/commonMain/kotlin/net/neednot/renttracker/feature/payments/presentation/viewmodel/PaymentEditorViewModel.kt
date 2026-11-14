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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.data.PaymentMethod
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.feature.payments.domain.DuplicatePolicy
import net.neednot.renttracker.feature.payments.domain.PaymentEntryOverview
import net.neednot.renttracker.feature.payments.domain.PaymentError
import net.neednot.renttracker.feature.payments.domain.PaymentRepository
import net.neednot.renttracker.feature.payments.presentation.uistate.PaymentEditorField
import net.neednot.renttracker.feature.payments.presentation.uistate.PaymentEditorUiState
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class PaymentEditorViewModel(
    private val tenantId: String,
    private val paymentId: String?,
    private val paymentRepository: PaymentRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(PaymentEditorUiState(isNewPayment = paymentId == null))
    val uiState: StateFlow<PaymentEditorUiState> = mutableUiState.asStateFlow()

    private val eventChannel = Channel<PaymentEditorEvent>(Channel.BUFFERED)
    val events: Flow<PaymentEditorEvent> = eventChannel.receiveAsFlow()

    private val clientId = Uuid.random().toString()
    private var expectedVersion: Long? = null
    private var callerAccountId: String? = null

    init {
        viewModelScope.launch { show(paymentRepository.cachedOrFirstSyncedEntry(tenantId, paymentId)) }
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
            }.copy(fieldErrors = it.fieldErrors - field, error = null)
        }
    }

    fun onDateChanged(date: LocalDate) {
        mutableUiState.update {
            val changed = it.copy(date = date, error = null)
            val dateError = changed.dateError()
            changed.copy(
                fieldErrors =
                    if (dateError ==
                        null
                    ) {
                        it.fieldErrors - PaymentEditorField.Date
                    } else {
                        it.fieldErrors +
                            (PaymentEditorField.Date to dateError)
                    },
            )
        }
    }

    fun onMethodClicked(method: PaymentMethod) {
        mutableUiState.update { it.copy(method = if (it.method == method) null else method) }
    }

    fun onLateFeeToggled(isChecked: Boolean) {
        mutableUiState.update {
            it.copy(
                lateFeeOffer = it.lateFeeOffer?.copy(isChecked = isChecked),
                fieldErrors = it.fieldErrors - PaymentEditorField.LateFeeAmount,
            )
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

    private fun save(duplicatePolicy: DuplicatePolicy) {
        val state = uiState.value
        if (state.isSaving || state.isLoading) return
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
                viewModelScope.launch {
                    paymentRepository
                        .save(PaymentSaveRequest(tenantId, paymentId, clientId, expectedVersion), form, duplicatePolicy)
                        .onOk { eventChannel.send(PaymentEditorEvent.PaymentSaved) }
                        .onErr { error -> onSaveFailed(error) }
                    mutableUiState.update { it.copy(isSaving = false) }
                }
            },
            failure = { errors -> mutableUiState.update { it.copy(fieldErrors = errors) } },
        )
    }

    private suspend fun onSaveFailed(error: PaymentError) {
        when (error) {
            is PaymentError.PossibleDuplicate -> {
                val lateFeeMonth = uiState.value.lateFeeOffer?.month
                mutableUiState.update { it.copy(duplicate = error.matches.toDialog(callerAccountId, lateFeeMonth)) }
            }

            is PaymentError.EditConflict -> {
                mutableUiState.update { it.copy(conflictBanner = conflictBanner(error.editedByName)) }
                show(paymentRepository.entryFromServer(tenantId, paymentId))
            }

            else -> {
                mutableUiState.update { it.copy(error = error.text()) }
            }
        }
    }

    private fun show(snapshot: Synced<PaymentEntryOverview?, PaymentError>) {
        val overview = snapshot.value?.takeIf { paymentId == null || it.payment != null }
        expectedVersion = overview?.payment?.version
        callerAccountId = overview?.callerAccountId
        mutableUiState.update {
            if (overview == null) {
                it.copy(isLoading = false, error = it.error ?: snapshot.loadError().text())
            } else {
                it.showing(overview)
            }
        }
    }
}
