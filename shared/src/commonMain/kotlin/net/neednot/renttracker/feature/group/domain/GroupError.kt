package net.neednot.renttracker.feature.group.domain

sealed interface GroupError {
    data object NotFound : GroupError

    data object Offline : GroupError

    data class Unexpected(
        val message: String,
    ) : GroupError
}
