package net.neednot.renttracker.feature.group.di

import net.neednot.renttracker.feature.group.data.GroupFreshnessRepositoryImpl
import net.neednot.renttracker.feature.group.data.GroupRepositoryImpl
import net.neednot.renttracker.feature.group.domain.GroupFreshnessRepository
import net.neednot.renttracker.feature.group.domain.GroupRepository
import net.neednot.renttracker.feature.group.presentation.viewmodel.ArchivedTenantsViewModel
import net.neednot.renttracker.feature.group.presentation.viewmodel.GroupSettingsViewModel
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
                groupAccessWatcher = get(),
                telemetry = get(),
            )
        }
        single<GroupFreshnessRepository> { GroupFreshnessRepositoryImpl(freshnessSource = get()) }
        viewModel { parameters ->
            GroupViewModel(
                groupId = parameters.get(),
                groupRepository = get(),
                groupFreshnessRepository = get(),
                messageDispatcher = get(),
            )
        }
        viewModel { parameters ->
            GroupSettingsViewModel(groupId = parameters.get(), groupRepository = get())
        }
        viewModel { parameters ->
            ArchivedTenantsViewModel(
                groupId = parameters.get(),
                groupRepository = get(),
                groupFreshnessRepository = get(),
            )
        }
    }
