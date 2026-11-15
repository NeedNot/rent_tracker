package net.neednot.renttracker.feature.account.presentation.viewmodel

import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.validation.InputLimits
import net.neednot.renttracker.feature.account.domain.AccountError
import net.neednot.renttracker.feature.account.domain.AccountRepository
import net.neednot.renttracker.feature.account.presentation.uistate.EditNameDialogUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.account_error_name_length

internal class EditNameFlow(
    private val scope: CoroutineScope,
    private val accountRepository: AccountRepository,
    private val onFailure: (AccountError) -> Unit,
) {
    private val mutableDialog = MutableStateFlow<EditNameDialogUiState?>(null)
    val dialog: StateFlow<EditNameDialogUiState?> = mutableDialog.asStateFlow()

    fun open(currentName: String) {
        mutableDialog.value = EditNameDialogUiState(name = currentName)
    }

    fun change(name: String) {
        mutableDialog.update { it?.copy(name = name, error = lengthError(name)) }
    }

    fun dismiss() {
        if (mutableDialog.value?.isSaving != true) mutableDialog.value = null
    }

    fun save() {
        val opened = mutableDialog.value
        if (opened == null || opened.isSaving) return
        val name = opened.name.trim()
        val invalid =
            lengthError(name) ?: UiText.Resource(Res.string.account_error_name_length).takeIf { name.isEmpty() }
        if (invalid != null) {
            mutableDialog.value = opened.copy(error = invalid)
            return
        }
        mutableDialog.value = opened.copy(isSaving = true, error = null)
        scope.launch {
            accountRepository
                .editName(name)
                .onOk { mutableDialog.value = null }
                .onErr { error -> showFailure(error) }
        }
    }

    private fun showFailure(error: AccountError) {
        if (error is AccountError.ValidationFailed) {
            mutableDialog.update { it?.copy(isSaving = false, error = error.text()) }
        } else {
            mutableDialog.update { it?.copy(isSaving = false) }
            onFailure(error)
        }
    }

    private fun lengthError(name: String): UiText? =
        if (name.trim().length > InputLimits.MAX_NAME_LENGTH) {
            UiText.Resource(Res.string.account_error_name_length)
        } else {
            null
        }
}
