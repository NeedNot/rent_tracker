package net.neednot.renttracker.feature.home.data

import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.feature.home.domain.HomeError
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeErrorMappingTest {
    @Test
    fun aValidationFailureKeepsTheServersMessage() {
        val error = BackendError.Convex(code = "ValidationFailed", message = "Group name is too long.", details = null)

        assertEquals(HomeError.ValidationFailed("Group name is too long."), error.toHomeError())
    }

    @Test
    fun aMissingInviteAndADeniedCallKeepTheServersMessage() {
        val notFound = BackendError.Convex(code = "NotFound", message = "This invite no longer exists.", details = null)
        val denied = BackendError.Convex(code = "PermissionDenied", message = "Not yours.", details = null)

        assertEquals(HomeError.Rejected("This invite no longer exists."), notFound.toHomeError())
        assertEquals(HomeError.Rejected("Not yours."), denied.toHomeError())
    }

    @Test
    fun otherServerCodesAreUnexpected() {
        val error = BackendError.Convex(code = "NotSignedIn", message = "Sign in first.", details = null)

        assertEquals(HomeError.Unexpected("NotSignedIn: Sign in first."), error.toHomeError())
    }

    @Test
    fun serverAndInternalFailuresAreUnexpected() {
        assertEquals(HomeError.Unexpected("boom"), BackendError.Server("boom").toHomeError())
        assertEquals(HomeError.Unexpected("bad json"), BackendError.Internal("bad json").toHomeError())
    }
}
