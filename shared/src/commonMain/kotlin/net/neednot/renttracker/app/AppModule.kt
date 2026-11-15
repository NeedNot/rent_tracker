package net.neednot.renttracker.app

import net.neednot.renttracker.core.data.GroupAccessWatcher
import net.neednot.renttracker.core.data.PendingPaymentAutoSync
import net.neednot.renttracker.core.data.PendingPaymentLinks
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule =
    module {
        viewModel {
            AppViewModel(
                authRepository = get(),
                lostGroupAccess = get<GroupAccessWatcher>().losses,
                syncPendingPaymentsWhileOnline = get<PendingPaymentAutoSync>()::syncWhileOnline,
                pendingPaymentLinks = get<PendingPaymentLinks>().opened,
                messageDispatcher = get(),
            )
        }
    }
