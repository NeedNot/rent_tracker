package net.neednot.renttracker.core.data

import kotlinx.coroutines.flow.MutableStateFlow

class FakeAsOfClock(
    initialAsOf: Long,
) : AsOfClock {
    override val asOf = MutableStateFlow(initialAsOf)
}
