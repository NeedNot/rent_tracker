package net.neednot.renttracker.feature.tenants.di

import net.neednot.renttracker.core.navigation.RentTermEditorRoute
import net.neednot.renttracker.core.navigation.TenantEditorRoute
import net.neednot.renttracker.core.navigation.TenantRoute
import net.neednot.renttracker.feature.tenants.data.TenantRepositoryImpl
import net.neednot.renttracker.feature.tenants.domain.TenantRepository
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
                telemetry = get(),
            )
        }
        viewModel { (route: TenantRoute) -> TenantViewModel(tenantId = route.tenantId, tenantRepository = get()) }
        viewModel { (route: TenantEditorRoute) ->
            TenantEditorViewModel(groupId = route.groupId, tenantId = route.tenantId, tenantRepository = get())
        }
        viewModel { (route: RentTermEditorRoute) ->
            RentTermEditorViewModel(tenantId = route.tenantId, rentTermId = route.rentTermId, tenantRepository = get())
        }
    }
