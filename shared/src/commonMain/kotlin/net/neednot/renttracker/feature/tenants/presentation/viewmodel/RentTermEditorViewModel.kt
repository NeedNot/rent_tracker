package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.fold
import com.github.michaelbull.result.map
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.domain.RentTermDraft
import net.neednot.renttracker.feature.tenants.domain.TenantError
import net.neednot.renttracker.feature.tenants.domain.TenantOverview
import net.neednot.renttracker.feature.tenants.domain.TenantRepository
import net.neednot.renttracker.feature.tenants.presentation.uistate.PastMonthWarningUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.RentTermDeletionUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.RentTermEditorField
import net.neednot.renttracker.feature.tenants.presentation.uistate.RentTermEditorUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.rent_term_editor_error_reason_required
import kotlin.time.Duration.Companion.milliseconds

class RentTermEditorViewModel(
    private val tenantId: String,
    private val rentTermId: String?,
    private val tenantRepository: TenantRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(RentTermEditorUiState(isNewTerm = rentTermId == null))
    val uiState: StateFlow<RentTermEditorUiState> = mutableUiState.asStateFlow()

    private val eventChannel = Channel<RentTermEditorEvent>(Channel.BUFFERED)
    val events: Flow<RentTermEditorEvent> = eventChannel.receiveAsFlow()

    private var loadedTerm: LoadedRentTerm? = null
    private var previewedDraft: RentTermDraft? = null
    private var previewJob: Job? = null

    init {
        viewModelScope.launch { show(tenantRepository.cachedOrFirstSyncedTenant(tenantId)) }
    }

    fun onStartMonthChanged(month: YearMonth) {
        mutableUiState.update {
            it.copy(startMonth = month, error = null)
        }
        refreshWarnings()
    }

    fun onFieldChanged(
        field: RentTermEditorField,
        text: String,
    ) {
        mutableUiState.update {
            when (field) {
                RentTermEditorField.Amount -> it.copy(amount = text)
                RentTermEditorField.DueDay -> it.copy(dueDay = text)
                RentTermEditorField.DeleteReason -> it.copy(deletion = it.deletion?.copy(reason = text, error = null))
            }.copy(fieldErrors = it.fieldErrors - field, error = null)
        }
        refreshWarnings()
    }

    fun onSaveClicked() {
        val state = uiState.value
        val draft = state.draftOrNull()
        if (state.isSaving || state.isLoading) return
        if (draft == null) {
            mutableUiState.update { it.copy(fieldErrors = it.formatErrors()) }
            return
        }
        mutableUiState.update { it.copy(isSaving = true, error = null) }
        submit {
            val version = loadedTerm?.expectedVersion
            when {
                rentTermId == null -> tenantRepository.addRentTerm(tenantId, draft)
                version == null -> Err(TenantError.NotFound)
                else -> tenantRepository.editRentTerm(rentTermId, version, draft)
            }.map { RentTermEditorEvent.RentTermSaved }
        }
    }

    fun onDeleteClicked() {
        mutableUiState.update { it.copy(deletion = RentTermDeletionUiState()) }
    }

    fun onDeleteDismissed() {
        mutableUiState.update { it.copy(deletion = null) }
    }

    fun onDeleteConfirmed() {
        val deletion = uiState.value.deletion?.takeUnless { it.isDeleting }
        val version = loadedTerm?.expectedVersion
        if (deletion == null || rentTermId == null || version == null) return
        val reason = deletion.reason.trim()
        if (reason.isEmpty()) {
            val reasonRequired = UiText.Resource(Res.string.rent_term_editor_error_reason_required)
            mutableUiState.update { it.copy(deletion = deletion.copy(error = reasonRequired)) }
            return
        }
        mutableUiState.update { it.copy(deletion = deletion.copy(isDeleting = true, error = null)) }
        submit {
            tenantRepository
                .deleteRentTerm(rentTermId, version, reason)
                .map { RentTermEditorEvent.RentTermDeleted }
        }
    }

    private fun submit(change: suspend () -> Result<RentTermEditorEvent, TenantError>) {
        viewModelScope.launch {
            change()
                .onOk { event -> eventChannel.send(event) }
                .onErr { error -> onChangeFailed(error) }
            mutableUiState.update { it.copy(isSaving = false, deletion = it.deletion?.copy(isDeleting = false)) }
        }
    }

    private suspend fun onChangeFailed(error: TenantError) {
        val isConflict = error is TenantError.EditConflict
        mutableUiState.update {
            if (it.deletion != null && !isConflict) {
                it.copy(deletion = it.deletion.copy(error = error.text()))
            } else {
                it.copy(deletion = null, error = error.text())
            }
        }
        if (isConflict) show(tenantRepository.tenantFromServer(tenantId))
    }

    private fun show(snapshot: Synced<TenantOverview?, TenantError>) {
        val term = snapshot.value?.loadRentTerm(rentTermId)
        loadedTerm = term
        previewedDraft = null
        mutableUiState.update {
            if (term == null) {
                it.copy(isLoading = false, error = it.error ?: snapshot.loadError().text())
            } else {
                it.copy(
                    isLoading = false,
                    startMonth = term.startMonth,
                    amount = term.amount,
                    dueDay = term.dueDay.toString(),
                    canDelete = term.canDelete,
                    fieldErrors = emptyMap(),
                )
            }
        }
        refreshWarnings()
    }

    private fun refreshWarnings() {
        val term = loadedTerm ?: return
        val state = uiState.value
        val dueDay = state.dueDay.trim().toIntOrNull()
        mutableUiState.update { it.copy(showsDueDayReminder = dueDay != null && dueDay != term.dueDay) }
        val pastDraft = state.draftOrNull()?.takeIf { it.startMonth < term.currentMonth }
        if (pastDraft == previewedDraft) return
        previewedDraft = pastDraft
        previewJob?.cancel()
        mutableUiState.update { it.copy(pastMonthWarning = pastDraft?.let { PastMonthWarningUiState.CheckingBalance }) }
        previewJob =
            pastDraft?.let { draft ->
                viewModelScope.launch {
                    delay(PREVIEW_DEBOUNCE)
                    val warning =
                        tenantRepository.previewRentTerm(tenantId, rentTermId, draft).fold(
                            success = {
                                PastMonthWarningUiState.BalanceChange(
                                    it.before.inWords(),
                                    it.after.inWords(),
                                )
                            },
                            failure = { PastMonthWarningUiState.CheckFailed(it.text()) },
                        )
                    mutableUiState.update { it.copy(pastMonthWarning = warning) }
                }
            }
    }

    private companion object {
        val PREVIEW_DEBOUNCE = 300.milliseconds
    }
}
