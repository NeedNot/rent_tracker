package net.neednot.renttracker.feature.payments.data

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.feature.payments.domain.PaymentError
import kotlin.test.Test
import kotlin.test.assertEquals

class PaymentErrorMappingTest {
    @Test
    fun aValidationFailureKeepsTheFieldItNames() {
        val details = buildJsonObject { put("field", "Amount") }
        val error = BackendError.Convex(code = "ValidationFailed", message = "Bad amount.", details = details)

        assertEquals(PaymentError.ValidationFailed("Bad amount.", field = "Amount"), error.toPaymentError())
    }

    @Test
    fun aValidationFailureWithoutDetailsHasNoField() {
        val error = BackendError.Convex(code = "ValidationFailed", message = "Bad.", details = null)

        assertEquals(PaymentError.ValidationFailed("Bad.", field = null), error.toPaymentError())
    }
}
