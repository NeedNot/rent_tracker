package net.neednot.renttracker.feature.tenants.di

import net.neednot.renttracker.core.navigation.LedgerRoute
import net.neednot.renttracker.core.navigation.MoveTenantRoute
import net.neednot.renttracker.core.navigation.RentTermEditorRoute
import net.neednot.renttracker.core.navigation.TenantEditorRoute
import net.neednot.renttracker.core.navigation.TenantRoute
import net.neednot.renttracker.feature.tenants.data.LedgerEntryRepositoryImpl
import net.neednot.renttracker.feature.tenants.data.MoveTenantRepositoryImpl
import net.neednot.renttracker.feature.tenants.data.PendingPaymentRepositoryImpl
import net.neednot.renttracker.feature.tenants.data.TenantCalendarRepositoryImpl
import net.neednot.renttracker.feature.tenants.data.TenantFreshnessRepositoryImpl
import net.neednot.renttracker.feature.tenants.data.TenantRepositoryImpl
import net.neednot.renttracker.feature.tenants.domain.LedgerEntryRepository
import net.neednot.renttracker.feature.tenants.domain.MoveTenantRepository
import net.neednot.renttracker.feature.tenants.domain.PendingPaymentRepository
import net.neednot.renttracker.feature.tenants.domain.TenantCalendarRepository
import net.neednot.renttracker.feature.tenants.domain.TenantFreshnessRepository
import net.neednot.renttracker.feature.tenants.domain.TenantRepository
import net.neednot.renttracker.feature.tenants.presentation.viewmodel.LedgerEntryViewModel
import net.neednot.renttracker.feature.tenants.presentation.viewmodel.LedgerViewModel
import net.neednot.renttracker.feature.tenants.presentation.viewmodel.MoveTenantViewModel
import net.neednot.renttracker.feature.tenants.presentation.viewmodel.PendingPaymentViewModel
import net.neednot.renttracker.feature.tenants.presentation.viewmodel.RentTermEditorViewModel
import net.neednot.renttracker.feature.tenants.presentation.viewmodel.TenantEditorViewModel
import net.neednot.renttracker.feature.tenants.presentation.viewmodel.TenantViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val tenantsModule =
    module {
        single<TenantRepository> {
            TenantRepositoryImpl(
                backend = get(),
                asOfClock = get(),
                database = get(),
                screenResultStore = get(),
                groupAccessWatcher = get(),
                telemetry = get(),
            )
        }
        single<MoveTenantRepository> { MoveTenantRepositoryImpl(backend = get(), asOfClock = get(), telemetry = get()) }
        single<TenantCalendarRepository> {
            TenantCalendarRepositoryImpl(
                backend = get(),
                asOfClock = get(),
                database = get(),
                screenResultStore = get(),
                telemetry = get(),
            )
        }
        single<TenantFreshnessRepository> {
            TenantFreshnessRepositoryImpl(database = get(), asOfClock = get(), freshnessSource = get())
        }
        single<PendingPaymentRepository> {
            PendingPaymentRepositoryImpl(database = get(), reader = get(), queue = get())
        }
        single<LedgerEntryRepository> { LedgerEntryRepositoryImpl(backend = get(), telemetry = get()) }
        viewModel { (route: TenantRoute) ->
            TenantViewModel(
                tenantId = route.tenantId,
                tenantRepository = get(),
                calendarRepository = get(),
                freshnessRepository = get(),
                pendingPaymentRepository = get(),
            )
        }
        viewModel { (route: TenantRoute) ->
            PendingPaymentViewModel(
                openedClientId = route.pendingClientId,
                repository = get(),
                messageDispatcher = get(),
            )
        }
        viewModel { (route: MoveTenantRoute) ->
            MoveTenantViewModel(
                tenantId = route.tenantId,
                tenantRepository = get(),
                moveRepository = get(),
                messageDispatcher = get(),
            )
        }
        viewModel { (route: TenantEditorRoute) ->
            TenantEditorViewModel(groupId = route.groupId, tenantId = route.tenantId, tenantRepository = get())
        }
        viewModel { (route: RentTermEditorRoute) ->
            RentTermEditorViewModel(tenantId = route.tenantId, rentTermId = route.rentTermId, tenantRepository = get())
        }
        viewModel { (route: LedgerRoute) ->
            LedgerViewModel(
                tenantId = route.tenantId,
                tenantRepository = get(),
                freshnessRepository = get(),
                pendingPaymentRepository = get(),
            )
        }
        viewModel { (tenantId: String) ->
            LedgerEntryViewModel(
                tenantId = tenantId,
                tenantRepository = get(),
                freshnessRepository = get(),
                ledgerEntryRepository = get(),
                messageDispatcher = get(),
            )
        }
    }
