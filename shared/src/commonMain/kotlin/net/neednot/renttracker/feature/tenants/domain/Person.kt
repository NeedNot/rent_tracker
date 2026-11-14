package net.neednot.renttracker.feature.tenants.domain

sealed interface Person {
    data object You : Person

    data class Named(
        val name: String,
    ) : Person
}
