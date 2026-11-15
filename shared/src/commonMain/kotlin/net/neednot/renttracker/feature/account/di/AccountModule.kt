package net.neednot.renttracker.feature.account.di

import net.neednot.renttracker.feature.account.data.AccountRepositoryImpl
import net.neednot.renttracker.feature.account.domain.AccountRepository
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
                pendingPaymentQueue = get(),
                telemetry = get(),
            )
        }
        viewModel { AccountViewModel(accountRepository = get(), messageDispatcher = get()) }
    }
