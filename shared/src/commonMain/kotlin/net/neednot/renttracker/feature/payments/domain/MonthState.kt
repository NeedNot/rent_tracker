package net.neednot.renttracker.feature.payments.domain

enum class MonthState {
    Paid,
    Partial,
    Owed,
    DueNotLate,
    Upcoming,
    Prepaid,
    NotATenant,
}
