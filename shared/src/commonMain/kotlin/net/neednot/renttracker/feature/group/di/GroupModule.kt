package net.neednot.renttracker.feature.group.di

import net.neednot.renttracker.feature.group.data.GroupRepositoryImpl
import net.neednot.renttracker.feature.group.domain.GroupRepository
import net.neednot.renttracker.feature.group.presentation.viewmodel.ArchivedTenantsViewModel
import net.neednot.renttracker.feature.group.presentation.viewmodel.GroupViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val groupModule =
    module {
        single<GroupRepository> {
            GroupRepositoryImpl(
                backend = get(),
                asOfClock = get(),
                database = get(),
                screenResultStore = get(),
                telemetry = get(),
            )
        }
        viewModel { parameters ->
            GroupViewModel(groupId = parameters.get(), groupRepository = get(), messageDispatcher = get())
        }
        viewModel { parameters ->
            ArchivedTenantsViewModel(groupId = parameters.get(), groupRepository = get(), messageDispatcher = get())
        }
    }
