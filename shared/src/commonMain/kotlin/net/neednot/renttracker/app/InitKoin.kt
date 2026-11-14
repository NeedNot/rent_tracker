package net.neednot.renttracker.app

import net.neednot.renttracker.core.data.coreDataModule
import net.neednot.renttracker.core.ui.coreUiModule
import net.neednot.renttracker.feature.auth.di.authModule
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

private val sharedModules =
    listOf(
        appModule,
        coreDataModule,
        coreUiModule,
        authModule,
    )

fun initKoin(
    platformModules: List<Module>,
    appDeclaration: KoinAppDeclaration = {},
) {
    startKoin {
        appDeclaration()
        modules(platformModules + sharedModules)
    }
}
