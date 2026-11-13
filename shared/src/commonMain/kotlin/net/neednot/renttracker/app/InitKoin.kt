package net.neednot.renttracker.app

import net.neednot.renttracker.core.data.coreDataModule
import net.neednot.renttracker.core.ui.coreUiModule
import net.neednot.renttracker.feature.group.di.groupModule
import net.neednot.renttracker.feature.home.di.homeModule
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

private val sharedModules =
    listOf(
        coreDataModule,
        coreUiModule,
        homeModule,
        groupModule,
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
