package net.neednot.renttracker.feature.payments.presentation.uistate

import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.data.PaymentMethod
import net.neednot.renttracker.core.ui.UiText

data class PaymentEditorUiState(
    val isNewPayment: Boolean,
    val isLoading: Boolean = true,
    val header: PaymentHeaderUiState? = null,
    val today: LocalDate? = null,
    val amount: String = "",
    val date: LocalDate? = null,
    val method: PaymentMethod? = null,
    val lastUsedMethod: PaymentMethod? = null,
    val note: String = "",
    val lateFeeOffer: LateFeeOfferUiState? = null,
    val fieldErrors: Map<PaymentEditorField, UiText> = emptyMap(),
    val conflictBanner: UiText? = null,
    val monthChanges: MonthChangesUiState? = null,
    val error: UiText? = null,
    val duplicate: DuplicateDialogUiState? = null,
    val isSaving: Boolean = false,
)
