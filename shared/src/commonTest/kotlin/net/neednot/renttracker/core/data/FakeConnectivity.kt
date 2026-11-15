package net.neednot.renttracker.core.data

import kotlinx.coroutines.flow.MutableStateFlow

class FakeConnectivity(
    online: Boolean = true,
) : Connectivity {
    override val isOnline = MutableStateFlow(online)
}
