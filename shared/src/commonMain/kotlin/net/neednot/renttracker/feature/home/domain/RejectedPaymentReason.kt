package net.neednot.renttracker.feature.home.domain

enum class RejectedPaymentReason {
    NoPermission,
    TenantArchived,
    TenantMoved,
    Other,
}
