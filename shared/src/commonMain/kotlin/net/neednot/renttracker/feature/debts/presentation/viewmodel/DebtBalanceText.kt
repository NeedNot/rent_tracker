package net.neednot.renttracker.feature.debts.presentation.viewmodel

import net.neednot.renttracker.core.designsystem.format.creditText
import net.neednot.renttracker.core.designsystem.format.owesText
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.debts.domain.DebtBalance
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.balance_paid_up

internal fun DebtBalance.inWords(): UiText =
    when {
        owedCents > 0 -> owesText(owedCents)
        creditCents > 0 -> creditText(creditCents)
        else -> UiText.Resource(Res.string.balance_paid_up)
    }
