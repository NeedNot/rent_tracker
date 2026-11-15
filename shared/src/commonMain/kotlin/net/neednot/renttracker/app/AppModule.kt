package net.neednot.renttracker.app

import net.neednot.renttracker.core.data.GroupAccessWatcher
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule =
    module {
        viewModel {
            AppViewModel(
                authRepository = get(),
                lostGroupAccess = get<GroupAccessWatcher>().losses,
                messageDispatcher = get(),
            )
        }
    }
