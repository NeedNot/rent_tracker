package net.neednot.renttracker.feature.payments.presentation.components

import net.neednot.renttracker.core.data.PaymentMethod
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.entry_method_bank_transfer
import net.neednot.renttracker.resources.entry_method_cash
import net.neednot.renttracker.resources.entry_method_check
import net.neednot.renttracker.resources.entry_method_other
import net.neednot.renttracker.resources.entry_method_venmo
import net.neednot.renttracker.resources.entry_method_zelle
import org.jetbrains.compose.resources.StringResource

internal val PaymentMethod.label: StringResource
    get() =
        when (this) {
            PaymentMethod.Cash -> Res.string.entry_method_cash
            PaymentMethod.Check -> Res.string.entry_method_check
            PaymentMethod.Zelle -> Res.string.entry_method_zelle
            PaymentMethod.Venmo -> Res.string.entry_method_venmo
            PaymentMethod.BankTransfer -> Res.string.entry_method_bank_transfer
            PaymentMethod.Other -> Res.string.entry_method_other
        }
