package net.neednot.renttracker.feature.tenants.domain

data class TenantCalendar(
    val year: Int,
    val months: List<CalendarMonth>,
)
