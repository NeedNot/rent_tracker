package net.neednot.renttracker.core.ui

import org.koin.dsl.module

val coreUiModule =
    module {
        single<MessageDispatcher> { DefaultMessageDispatcher() }
    }
