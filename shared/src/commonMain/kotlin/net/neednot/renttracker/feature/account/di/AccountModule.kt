package net.neednot.renttracker.feature.account.di

import net.neednot.renttracker.feature.account.data.AccountRepositoryImpl
import net.neednot.renttracker.feature.account.data.NotificationSettingsRepositoryImpl
import net.neednot.renttracker.feature.account.domain.AccountRepository
import net.neednot.renttracker.feature.account.domain.NotificationSettingsRepository
import net.neednot.renttracker.feature.account.presentation.viewmodel.AccountNotificationsViewModel
import net.neednot.renttracker.feature.account.presentation.viewmodel.AccountViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val accountModule =
    module {
        single<AccountRepository> {
            AccountRepositoryImpl(
                backend = get(),
                authSession = get(),
                accountProfileSync = get(),
                pendingPaymentReader = get(),
                signOutCleanup = get(),
                telemetry = get(),
            )
        }
        single<NotificationSettingsRepository> {
            NotificationSettingsRepositoryImpl(backend = get(), lateRentAlerts = get(), telemetry = get())
        }
        viewModel { AccountViewModel(accountRepository = get(), messageDispatcher = get()) }
        viewModel {
            AccountNotificationsViewModel(notificationSettingsRepository = get(), messageDispatcher = get())
        }
    }
