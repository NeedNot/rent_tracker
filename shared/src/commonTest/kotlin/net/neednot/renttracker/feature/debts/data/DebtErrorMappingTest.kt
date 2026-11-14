package net.neednot.renttracker.feature.debts.data

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.feature.debts.domain.DebtError
import kotlin.test.Test
import kotlin.test.assertEquals

class DebtErrorMappingTest {
    @Test
    fun aValidationFailureKeepsTheFieldItNames() {
        val details = buildJsonObject { put("field", "Amount") }
        val error = BackendError.Convex(code = "ValidationFailed", message = "Bad amount.", details = details)

        assertEquals(DebtError.ValidationFailed("Bad amount.", field = "Amount"), error.toDebtError())
    }

    @Test
    fun aValidationFailureWithoutDetailsHasNoField() {
        val error = BackendError.Convex(code = "ValidationFailed", message = "Bad.", details = null)

        assertEquals(DebtError.ValidationFailed("Bad.", field = null), error.toDebtError())
    }
}
