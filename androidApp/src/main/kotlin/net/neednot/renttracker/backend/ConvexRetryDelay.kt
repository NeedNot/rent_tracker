package net.neednot.renttracker.backend

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val FIRST_RETRY_DELAY = 2.seconds
private val LONGEST_RETRY_DELAY = 60.seconds
private const val MAX_DOUBLINGS = 5

internal fun convexRetryDelay(failuresSoFar: Long): Duration =
    (FIRST_RETRY_DELAY * (1 shl failuresSoFar.coerceIn(0L, MAX_DOUBLINGS.toLong()).toInt()))
        .coerceAtMost(LONGEST_RETRY_DELAY)
