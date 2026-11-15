package net.neednot.renttracker.app

import net.neednot.renttracker.core.data.GroupAccessWatcher
import net.neednot.renttracker.core.data.GroupLinks
import net.neednot.renttracker.core.data.PendingPaymentAutoSync
import net.neednot.renttracker.core.data.PendingPaymentLinks
import net.neednot.renttracker.core.data.PushTokenRegistration
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule =
    module {
        viewModel {
            AppViewModel(
                authRepository = get(),
                lostGroupAccess = get<GroupAccessWatcher>().losses,
                whileAppIsOpen =
                    listOf(
                        get<PendingPaymentAutoSync>()::syncWhileOnline,
                        get<PushTokenRegistration>()::registerWhileSignedIn,
                    ),
                pendingPaymentLinks = get<PendingPaymentLinks>().opened,
                groupLinks = get<GroupLinks>().opened,
                messageDispatcher = get(),
            )
        }
    }
