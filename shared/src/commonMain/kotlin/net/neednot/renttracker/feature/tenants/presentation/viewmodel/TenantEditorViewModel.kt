package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
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
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.feature.tenants.domain.TenantError
import net.neednot.renttracker.feature.tenants.domain.TenantOverview
import net.neednot.renttracker.feature.tenants.domain.TenantRepository
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantEditorField
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantEditorForm
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantEditorUiState

class TenantEditorViewModel(
    private val groupId: String,
    private val tenantId: String?,
    private val tenantRepository: TenantRepository,
) : ViewModel() {
    private val mutableUiState =
        MutableStateFlow(TenantEditorUiState(isNewTenant = tenantId == null, isLoading = tenantId != null))
    val uiState: StateFlow<TenantEditorUiState> = mutableUiState.asStateFlow()

    private val eventChannel = Channel<TenantEditorEvent>(Channel.BUFFERED)
    val events: Flow<TenantEditorEvent> = eventChannel.receiveAsFlow()

    private var expectedVersion: Long? = null
    private var hasTriedSaving = false

    init {
        if (tenantId != null) {
            viewModelScope.launch { show(tenantRepository.cachedOrFirstSyncedTenant(tenantId)) }
        }
    }

    fun onFieldChanged(
        field: TenantEditorField,
        text: String,
    ) {
        updateForm { it.with(field, text) }
    }

    fun onMoveInChanged(date: LocalDate) {
        updateForm { it.copy(moveIn = date) }
    }

    fun onLastDayInUnitChanged(date: LocalDate?) {
        updateForm { it.copy(lastDayInUnit = date) }
    }

    fun onSaveClicked() {
        val state = uiState.value
        if (state.isSaving || state.isLoading) return
        hasTriedSaving = true
        state.form
            .validate(state.isNewTenant)
            .onOk(::save)
            .onErr { fieldErrors -> mutableUiState.update { it.copy(fieldErrors = fieldErrors) } }
    }

    private fun updateForm(change: (TenantEditorForm) -> TenantEditorForm) {
        mutableUiState.update {
            val form = change(it.form)
            it.copy(
                form = form,
                fieldErrors = form.fieldErrors(it.isNewTenant, includesMissing = hasTriedSaving),
                error = null,
            )
        }
    }

    private fun save(form: ValidTenantForm) {
        mutableUiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            saved(form)
                .onOk { event -> eventChannel.send(event) }
                .onErr { error -> onSaveFailed(error) }
            mutableUiState.update { it.copy(isSaving = false) }
        }
    }

    private suspend fun saved(form: ValidTenantForm): Result<TenantEditorEvent, TenantError> {
        val version = expectedVersion
        return when {
            tenantId == null && form.firstRentTerm != null -> {
                tenantRepository
                    .addTenant(groupId, form.details, form.firstRentTerm)
                    .map { TenantEditorEvent.TenantAdded(it) }
            }

            tenantId != null && version != null -> {
                tenantRepository.editTenant(tenantId, version, form.details).map { TenantEditorEvent.TenantSaved }
            }

            else -> {
                Err(TenantError.NotFound)
            }
        }
    }

    private suspend fun onSaveFailed(error: TenantError) {
        val field = (error as? TenantError.ValidationFailed)?.let { tenantEditorFieldOf(it.field) }
        mutableUiState.update {
            if (field != null) {
                it.copy(fieldErrors = it.fieldErrors + (field to error.text()))
            } else {
                it.copy(error = error.text())
            }
        }
        if (error is TenantError.EditConflict && tenantId != null) {
            show(tenantRepository.tenantFromServer(tenantId))
        }
    }

    private fun show(snapshot: Synced<TenantOverview?, TenantError>) {
        val tenant = snapshot.value?.tenant
        expectedVersion = tenant?.version
        mutableUiState.update {
            if (tenant == null) {
                it.copy(isLoading = false, error = it.error ?: snapshot.loadError().text())
            } else {
                it.copy(isLoading = false, form = tenant.details.toForm(), fieldErrors = emptyMap())
            }
        }
    }
}
