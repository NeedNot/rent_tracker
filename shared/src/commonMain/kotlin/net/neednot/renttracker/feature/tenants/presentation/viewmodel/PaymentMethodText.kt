package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import net.neednot.renttracker.core.data.PaymentMethod
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ledger_method_bank_transfer
import net.neednot.renttracker.resources.ledger_method_cash
import net.neednot.renttracker.resources.ledger_method_check
import net.neednot.renttracker.resources.ledger_method_other
import net.neednot.renttracker.resources.ledger_method_venmo
import net.neednot.renttracker.resources.ledger_method_zelle
import net.neednot.renttracker.resources.ledger_payment_title_bank_transfer
import net.neednot.renttracker.resources.ledger_payment_title_cash
import net.neednot.renttracker.resources.ledger_payment_title_check
import net.neednot.renttracker.resources.ledger_payment_title_other
import net.neednot.renttracker.resources.ledger_payment_title_venmo
import net.neednot.renttracker.resources.ledger_payment_title_zelle
import org.jetbrains.compose.resources.StringResource

internal fun PaymentMethod.label(): StringResource =
    when (this) {
        PaymentMethod.Cash -> Res.string.ledger_method_cash
        PaymentMethod.Check -> Res.string.ledger_method_check
        PaymentMethod.Zelle -> Res.string.ledger_method_zelle
        PaymentMethod.Venmo -> Res.string.ledger_method_venmo
        PaymentMethod.BankTransfer -> Res.string.ledger_method_bank_transfer
        PaymentMethod.Other -> Res.string.ledger_method_other
    }

internal fun PaymentMethod.paymentTitle(): StringResource =
    when (this) {
        PaymentMethod.Cash -> Res.string.ledger_payment_title_cash
        PaymentMethod.Check -> Res.string.ledger_payment_title_check
        PaymentMethod.Zelle -> Res.string.ledger_payment_title_zelle
        PaymentMethod.Venmo -> Res.string.ledger_payment_title_venmo
        PaymentMethod.BankTransfer -> Res.string.ledger_payment_title_bank_transfer
        PaymentMethod.Other -> Res.string.ledger_payment_title_other
    }
