package net.neednot.renttracker.core.data

import net.neednot.renttracker.core.backend.BackendError

private const val NOT_SIGNED_IN = "NotSignedIn"

internal fun BackendError.isRetryable(): Boolean = this !is BackendError.Convex || code == NOT_SIGNED_IN
