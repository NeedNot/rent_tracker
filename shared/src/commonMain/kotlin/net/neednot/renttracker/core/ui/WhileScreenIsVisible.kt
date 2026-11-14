package net.neednot.renttracker.core.ui

import kotlinx.coroutines.flow.SharingStarted

private const val LINGER_MILLIS = 5_000L

// Outlives a rotation or a brief app switch, so the Convex subscription behind a screen is not dropped.
val WhileScreenIsVisible: SharingStarted = SharingStarted.WhileSubscribed(stopTimeoutMillis = LINGER_MILLIS)
