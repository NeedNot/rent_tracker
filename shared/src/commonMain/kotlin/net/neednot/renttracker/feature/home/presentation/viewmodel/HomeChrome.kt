package net.neednot.renttracker.feature.home.presentation.viewmodel

import net.neednot.renttracker.core.sync.Freshness
import net.neednot.renttracker.feature.home.domain.RejectedPayment

internal data class HomeChrome(
    val accountInitial: String?,
    val freshness: Freshness,
    val rejectedPayments: List<RejectedPayment>,
)
