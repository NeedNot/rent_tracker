package net.neednot.renttracker.feature.group.data

import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.feature.group.domain.GroupError
import kotlin.test.Test
import kotlin.test.assertEquals

class GroupErrorMappingTest {
    @Test
    fun aMissingGroupIsNotFound() {
        val error = BackendError.Convex(code = "NotFound", message = "This group no longer exists.", details = null)

        assertEquals(GroupError.NotFound, error.toGroupError())
    }

    @Test
    fun otherServerCodesAreUnexpected() {
        val error = BackendError.Convex(code = "Forbidden", message = "You cannot see this group.", details = null)

        assertEquals(GroupError.Unexpected("Forbidden: You cannot see this group."), error.toGroupError())
    }

    @Test
    fun serverAndInternalFailuresAreUnexpected() {
        assertEquals(GroupError.Unexpected("boom"), BackendError.Server("boom").toGroupError())
        assertEquals(GroupError.Unexpected("bad json"), BackendError.Internal("bad json").toGroupError())
    }
}
