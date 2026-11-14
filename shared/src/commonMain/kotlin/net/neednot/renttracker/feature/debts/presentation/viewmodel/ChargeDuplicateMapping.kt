package net.neednot.renttracker.feature.debts.presentation.viewmodel

import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.designsystem.format.formatMoney
import net.neednot.renttracker.core.designsystem.format.formatMonthDay
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.debts.domain.DuplicateLateFee
import net.neednot.renttracker.feature.debts.presentation.uistate.DuplicateLateFeeUiState
import net.neednot.renttracker.feature.debts.presentation.uistate.LateFeeDuplicateUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.charge_duplicate_intro
import net.neednot.renttracker.resources.charge_duplicate_you

internal fun List<DuplicateLateFee>.toDialog(
    month: YearMonth,
    callerAccountId: String?,
): LateFeeDuplicateUiState =
    LateFeeDuplicateUiState(
        intro = UiText.Resource(Res.string.charge_duplicate_intro, listOf(month.monthName())),
        matches = map { it.toUiState(callerAccountId) },
    )

private fun DuplicateLateFee.toUiState(callerAccountId: String?): DuplicateLateFeeUiState =
    DuplicateLateFeeUiState(
        date = formatMonthDay(date),
        by =
            if (callerAccountId != null && byAccountId == callerAccountId) {
                UiText.Resource(Res.string.charge_duplicate_you)
            } else {
                UiText.Plain(byName)
            },
        amount = formatMoney(amountCents),
    )
