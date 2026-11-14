package net.neednot.renttracker.feature.debts.di

import net.neednot.renttracker.feature.debts.data.DebtRepositoryImpl
import net.neednot.renttracker.feature.debts.domain.DebtRepository
import net.neednot.renttracker.feature.debts.presentation.viewmodel.ChargeEditorTarget
import net.neednot.renttracker.feature.debts.presentation.viewmodel.ChargeEditorViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val debtsModule =
    module {
        single<DebtRepository> {
            DebtRepositoryImpl(
                backend = get(),
                asOfClock = get(),
                database = get(),
                screenResultStore = get(),
                telemetry = get(),
            )
        }
        viewModel { (target: ChargeEditorTarget) -> ChargeEditorViewModel(target = target, debtRepository = get()) }
    }
