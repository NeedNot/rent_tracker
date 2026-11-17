package net.neednot.renttracker.screenshots

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.data.PaymentMethod
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.payments.presentation.uistate.LateFeeOfferUiState
import net.neednot.renttracker.feature.payments.presentation.uistate.PaymentEditorUiState
import net.neednot.renttracker.feature.payments.presentation.uistate.PaymentHeaderUiState

internal val paymentEditorPopulated =
    PaymentEditorUiState(
        isNewPayment = true,
        isLoading = false,
        header =
            PaymentHeaderUiState(
                tenantName = "Apt 3B",
                groupName = "1st Street",
                balance = UiText.Plain("Owes $550.00"),
            ),
        today = LocalDate(2026, 3, 10),
        amount = "1,500.00",
        date = LocalDate(2026, 3, 10),
        method = PaymentMethod.Zelle,
        lastUsedMethod = PaymentMethod.Zelle,
        lateFeeOffer =
            LateFeeOfferUiState(
                month = YearMonth(2026, 3),
                isChecked = false,
                amount = "50.00",
                suggestedAmount = "$50.00",
            ),
    )
