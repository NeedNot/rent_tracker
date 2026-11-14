package net.neednot.renttracker.feature.payments.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal enum class PaymentMethodDetails {
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
