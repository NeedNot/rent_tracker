package net.neednot.renttracker.feature.auth.di

import net.neednot.renttracker.feature.auth.data.AuthRepositoryImpl
import net.neednot.renttracker.feature.auth.domain.AuthRepository
import net.neednot.renttracker.feature.auth.presentation.viewmodel.DisplayNameViewModel
import net.neednot.renttracker.feature.auth.presentation.viewmodel.SignInCodeViewModel
import net.neednot.renttracker.feature.auth.presentation.viewmodel.SignInViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val authModule =
    module {
        single<AuthRepository> {
            AuthRepositoryImpl(
                authSession = get(),
                googleIdTokenProvider = get(),
                backend = get(),
                telemetry = get(),
            )
        }
        viewModelOf(::SignInViewModel)
        viewModel { parameters -> SignInCodeViewModel(email = parameters.get(), authRepository = get()) }
        viewModel { parameters ->
            DisplayNameViewModel(defaultDisplayName = parameters.get(), authRepository = get())
        }
    }
