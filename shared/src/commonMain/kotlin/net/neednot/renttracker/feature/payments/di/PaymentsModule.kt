package net.neednot.renttracker.feature.payments.di

import net.neednot.renttracker.feature.payments.data.PaymentBackendCalls
import net.neednot.renttracker.feature.payments.data.PaymentRepositoryImpl
import net.neednot.renttracker.feature.payments.data.PhoneOutbox
import net.neednot.renttracker.feature.payments.domain.PaymentRepository
import net.neednot.renttracker.feature.payments.presentation.viewmodel.PaymentEditorTarget
import net.neednot.renttracker.feature.payments.presentation.viewmodel.PaymentEditorViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val paymentsModule =
    module {
        single<PaymentRepository> {
            PaymentRepositoryImpl(
                calls = PaymentBackendCalls(backend = get(), telemetry = get()),
                asOfClock = get(),
                database = get(),
                screenResultStore = get(),
                groupAccessWatcher = get(),
                phoneOutbox = PhoneOutbox(connectivity = get(), pendingPaymentQueue = get()),
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
