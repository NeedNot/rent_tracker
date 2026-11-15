package net.neednot.renttracker.core.data

import kotlinx.coroutines.flow.StateFlow

interface Connectivity {
    val isOnline: StateFlow<Boolean>
}
