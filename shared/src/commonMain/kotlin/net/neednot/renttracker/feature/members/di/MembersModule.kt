package net.neednot.renttracker.feature.members.di

import net.neednot.renttracker.feature.members.data.MembersRepositoryImpl
import net.neednot.renttracker.feature.members.domain.MembersRepository
import net.neednot.renttracker.feature.members.presentation.viewmodel.InviteMemberViewModel
import net.neednot.renttracker.feature.members.presentation.viewmodel.MemberPermissionsViewModel
import net.neednot.renttracker.feature.members.presentation.viewmodel.MembersViewModel
import net.neednot.renttracker.feature.members.presentation.viewmodel.TransferOwnershipViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val membersModule =
    module {
        single<MembersRepository> {
            MembersRepositoryImpl(
                backend = get(),
                asOfClock = get(),
                database = get(),
                screenResultStore = get(),
                groupAccessWatcher = get(),
                telemetry = get(),
            )
        }
        viewModel { (groupId: String) ->
            MembersViewModel(groupId = groupId, membersRepository = get(), messageDispatcher = get())
        }
        viewModel { (groupId: String) ->
            InviteMemberViewModel(groupId = groupId, membersRepository = get(), messageDispatcher = get())
        }
        viewModel { (groupId: String, membershipId: String) ->
            MemberPermissionsViewModel(groupId = groupId, membershipId = membershipId, membersRepository = get())
        }
        viewModel { (groupId: String) ->
            TransferOwnershipViewModel(groupId = groupId, membersRepository = get())
        }
    }
