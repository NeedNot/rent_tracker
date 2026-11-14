package net.neednot.renttracker.feature.debts.presentation.viewmodel

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
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.data.DebtKind
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.feature.debts.domain.ChargeDraft
import net.neednot.renttracker.feature.debts.domain.ChargeEntryOverview
import net.neednot.renttracker.feature.debts.domain.DebtError
import net.neednot.renttracker.feature.debts.domain.DebtRepository
import net.neednot.renttracker.feature.debts.domain.DuplicatePolicy
import net.neednot.renttracker.feature.debts.presentation.uistate.ChargeEditorField
import net.neednot.renttracker.feature.debts.presentation.uistate.ChargeEditorUiState
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val MAX_NOTE_LENGTH = 100

@OptIn(ExperimentalUuidApi::class)
class ChargeEditorViewModel(
    private val target: ChargeEditorTarget,
    private val debtRepository: DebtRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ChargeEditorUiState(isNewCharge = target.debtId == null))
    val uiState: StateFlow<ChargeEditorUiState> = mutableUiState.asStateFlow()

    private val eventChannel = Channel<ChargeEditorEvent>(Channel.BUFFERED)
    val events: Flow<ChargeEditorEvent> = eventChannel.receiveAsFlow()

    private val clientId = Uuid.random().toString()
    private var expectedVersion: Long? = null
    private var callerAccountId: String? = null

    init {
        viewModelScope.launch { show(debtRepository.cachedOrFirstSyncedEntry(target.tenantId, target.debtId)) }
    }

    fun onKindSelected(kind: DebtKind) {
        if (!uiState.value.isNewCharge) return
        mutableUiState.update {
            it.copy(
                kind = kind,
                fieldErrors = it.fieldErrors - ChargeEditorField.Note,
                error = null,
            )
        }
    }

    fun onFieldChanged(
        field: ChargeEditorField,
        text: String,
    ) {
        mutableUiState.update {
            when (field) {
                ChargeEditorField.Amount -> it.copy(amount = text)
                ChargeEditorField.Note -> it.copy(note = text.take(MAX_NOTE_LENGTH))
            }.copy(fieldErrors = it.fieldErrors - field, error = null)
        }
    }

    fun onDateChanged(date: LocalDate) {
        mutableUiState.update { it.copy(date = date, error = null) }
    }

    fun onForMonthChanged(month: YearMonth) {
        mutableUiState.update { it.copy(forMonth = month, error = null) }
    }

    fun onSaveClicked() {
        save(DuplicatePolicy.AskFirst)
    }

    fun onDuplicateConfirmed() {
        save(DuplicatePolicy.AddAnyway)
    }

    fun onDuplicateDismissed() {
        mutableUiState.update { it.copy(duplicate = null) }
    }

    private fun save(duplicatePolicy: DuplicatePolicy) {
        val state = uiState.value
        if (state.isSaving || state.isLoading) return
        state.validate().fold(
            success = { draft ->
                mutableUiState.update {
                    it.copy(
                        isSaving = true,
                        error = null,
                        conflictBanner = null,
                        duplicate = null,
                    )
                }
                viewModelScope.launch {
                    debtRepository
                        .save(
                            ChargeSaveRequest(target.tenantId, target.debtId, clientId, expectedVersion),
                            draft,
                            duplicatePolicy,
                        ).onOk { eventChannel.send(ChargeEditorEvent.ChargeSaved) }
                        .onErr { error -> onSaveFailed(error, draft) }
                    mutableUiState.update { it.copy(isSaving = false) }
                }
            },
            failure = { errors -> mutableUiState.update { it.copy(fieldErrors = errors) } },
        )
    }

    private suspend fun onSaveFailed(
        error: DebtError,
        draft: ChargeDraft,
    ) {
        when (error) {
            is DebtError.PossibleDuplicate -> {
                val month = (draft as? ChargeDraft.LateFee)?.forMonth
                mutableUiState.update {
                    if (month == null) {
                        it.copy(error = error.text())
                    } else {
                        it.copy(duplicate = error.matches.toDialog(month, callerAccountId))
                    }
                }
            }

            is DebtError.EditConflict -> {
                mutableUiState.update { it.copy(conflictBanner = conflictBanner(error.editedByName)) }
                show(debtRepository.entryFromServer(target.tenantId, target.debtId))
            }

            else -> {
                mutableUiState.update { it.copy(error = error.text()) }
            }
        }
    }

    private fun show(snapshot: Synced<ChargeEntryOverview?, DebtError>) {
        val overview = snapshot.value?.takeIf { target.debtId == null || it.charge != null }
        expectedVersion = overview?.charge?.version
        callerAccountId = overview?.callerAccountId
        mutableUiState.update {
            if (overview == null) {
                it.copy(isLoading = false, error = it.error ?: snapshot.loadError().text())
            } else {
                it.showing(overview, target.suggestion)
            }
        }
    }
}
