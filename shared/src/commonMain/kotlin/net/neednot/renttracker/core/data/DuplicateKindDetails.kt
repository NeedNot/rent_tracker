package net.neednot.renttracker.core.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class DuplicateKindDetails {
    @SerialName("payment")
    Payment,

    @SerialName("lateFee")
    LateFee,
}
