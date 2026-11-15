package net.neednot.renttracker.core.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PaymentMethodDetails {
    @SerialName("cash")
    Cash,

    @SerialName("check")
    Check,

    @SerialName("zelle")
    Zelle,

    @SerialName("venmo")
    Venmo,

    @SerialName("bankTransfer")
    BankTransfer,

    @SerialName("other")
    Other,
}
