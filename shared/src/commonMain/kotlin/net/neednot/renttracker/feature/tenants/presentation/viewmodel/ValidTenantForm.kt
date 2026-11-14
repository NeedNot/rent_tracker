package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import net.neednot.renttracker.feature.tenants.domain.FirstRentTerm
import net.neednot.renttracker.feature.tenants.domain.TenantDetails

internal data class ValidTenantForm(
    val details: TenantDetails,
    val firstRentTerm: FirstRentTerm?,
)
