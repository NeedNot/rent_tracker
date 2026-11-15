package net.neednot.renttracker.feature.payments.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal enum class DuplicateKindDetails {
    @SerialName("payment")
    Payment,

    @SerialName("lateFee")
    LateFee,
}
