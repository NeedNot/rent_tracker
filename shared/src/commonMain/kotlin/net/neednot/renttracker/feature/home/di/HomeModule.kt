package net.neednot.renttracker.feature.home.di

import kotlinx.datetime.TimeZone
import net.neednot.renttracker.feature.home.data.HomeAccountRepositoryImpl
import net.neednot.renttracker.feature.home.data.HomeRepositoryImpl
import net.neednot.renttracker.feature.home.domain.HomeAccountRepository
import net.neednot.renttracker.feature.home.domain.HomeRepository
import net.neednot.renttracker.feature.home.presentation.viewmodel.ArchivedGroupsViewModel
import net.neednot.renttracker.feature.home.presentation.viewmodel.CreateGroupViewModel
import net.neednot.renttracker.feature.home.presentation.viewmodel.HomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val homeModule =
    module {
        single<HomeRepository> {
            HomeRepositoryImpl(
                backend = get(),
                asOfClock = get(),
                database = get(),
                screenResultStore = get(),
                phoneTimeZone = TimeZone::currentSystemDefault,
                telemetry = get(),
            )
        }
        single<HomeAccountRepository> { HomeAccountRepositoryImpl(accountProfileSync = get()) }
        viewModelOf(::HomeViewModel)
        viewModel { ArchivedGroupsViewModel(homeRepository = get()) }
        viewModelOf(::CreateGroupViewModel)
    }
