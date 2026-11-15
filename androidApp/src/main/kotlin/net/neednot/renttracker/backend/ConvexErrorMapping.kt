package net.neednot.renttracker.backend

import dev.convex.android.ConvexError
import dev.convex.android.ServerError
import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.core.backend.ConvexJson

internal fun Throwable.toBackendError(): BackendError =
    when (this) {
        is ConvexError -> ConvexJson.parseError(data)
        is ServerError -> BackendError.Server(message.orEmpty())
        else -> BackendError.Internal(message ?: toString())
    }
