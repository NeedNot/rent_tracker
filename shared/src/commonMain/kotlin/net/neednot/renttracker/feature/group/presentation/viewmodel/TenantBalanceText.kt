package net.neednot.renttracker.feature.group.presentation.viewmodel

import net.neednot.renttracker.core.designsystem.format.creditText
import net.neednot.renttracker.core.designsystem.format.owesText
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.group.domain.TenantBalance
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.balance_paid_up

internal fun TenantBalance.text(): UiText =
    when (this) {
        is TenantBalance.Owes -> owesText(cents)
        is TenantBalance.Credit -> creditText(cents)
        TenantBalance.PaidUp -> UiText.Resource(Res.string.balance_paid_up)
    }
