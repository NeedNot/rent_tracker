package net.neednot.renttracker.feature.home.domain

sealed interface HomeError {
    data class ValidationFailed(
        val message: String,
    ) : HomeError

    data object Offline : HomeError

    data class Unexpected(
        val message: String,
    ) : HomeError
}
