package net.neednot.renttracker.feature.payments.di

import net.neednot.renttracker.feature.payments.data.PaymentRepositoryImpl
import net.neednot.renttracker.feature.payments.domain.PaymentRepository
import net.neednot.renttracker.feature.payments.presentation.viewmodel.PaymentEditorTarget
import net.neednot.renttracker.feature.payments.presentation.viewmodel.PaymentEditorViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val paymentsModule =
    module {
        single<PaymentRepository> {
            PaymentRepositoryImpl(
                backend = get(),
                asOfClock = get(),
                database = get(),
                screenResultStore = get(),
                telemetry = get(),
            )
        }
        viewModel { (target: PaymentEditorTarget) ->
            PaymentEditorViewModel(
                tenantId = target.tenantId,
                paymentId = target.paymentId,
                paymentRepository = get(),
            )
        }
    }
