package net.neednot.renttracker.core.backend

import kotlinx.serialization.json.JsonElement

sealed interface BackendError {
    data class Convex(
        val code: String,
        val message: String,
        val details: JsonElement?,
    ) : BackendError

    data class Server(
        val message: String,
    ) : BackendError

    data class Internal(
        val message: String,
    ) : BackendError
}
