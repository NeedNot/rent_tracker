package net.neednot.renttracker.feature.tenants.data

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.feature.tenants.domain.TenantError
import kotlin.test.Test
import kotlin.test.assertEquals

class TenantErrorMappingTest {
    @Test
    fun eachServerCodeMapsToItsTenantError() {
        val cases =
            listOf(
                "ValidationFailed" to TenantError.ValidationFailed(MESSAGE),
                "EditConflict" to TenantError.EditConflict(MESSAGE),
                "NotFound" to TenantError.NotFound,
                "TenantArchived" to TenantError.TenantArchived(MESSAGE),
                "PermissionDenied" to TenantError.PermissionDenied(MESSAGE),
                "Surprise" to TenantError.Unexpected("Surprise: $MESSAGE"),
            )
        cases.forEach { (code, expected) ->
            val error = BackendError.Convex(code = code, message = MESSAGE, details = null)
            assertEquals(expected, error.toTenantError(), "code = $code")
        }
    }

    @Test
    fun aValidationFailureKeepsTheFieldItNames() {
        val details = buildJsonObject { put("field", "Due day") }
        val error = BackendError.Convex(code = "ValidationFailed", message = MESSAGE, details = details)

        assertEquals(TenantError.ValidationFailed(MESSAGE, field = "Due day"), error.toTenantError())
    }

    @Test
    fun serverAndInternalFailuresAreUnexpected() {
        assertEquals(TenantError.Unexpected("boom"), BackendError.Server("boom").toTenantError())
        assertEquals(TenantError.Unexpected("bad json"), BackendError.Internal("bad json").toTenantError())
    }

    private companion object {
        const val MESSAGE = "Sam changed this tenant while you were editing."
    }
}
