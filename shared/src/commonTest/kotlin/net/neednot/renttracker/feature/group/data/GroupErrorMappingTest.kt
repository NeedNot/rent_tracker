package net.neednot.renttracker.feature.group.data

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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

    @Test
    fun aRejectedValueIsAValidationErrorWithItsField() {
        val error =
            BackendError.Convex(
                code = "ValidationFailed",
                message = "Group name is required.",
                details = buildJsonObject { put("field", "Group name") },
            )

        assertEquals(GroupError.ValidationFailed("Group name is required.", "Group name"), error.toGroupError())
    }

    @Test
    fun aStaleVersionIsAnEditConflictNamingTheEditor() {
        val error =
            BackendError.Convex(
                code = "EditConflict",
                message = "Sam Rivera changed this group while you were editing.",
                details = buildJsonObject { put("lastEditedByName", "Sam Rivera") },
            )

        assertEquals(
            GroupError.EditConflict("Sam Rivera changed this group while you were editing.", "Sam Rivera"),
            error.toGroupError(),
        )
    }

    @Test
    fun aDeniedCallIsNoAccess() {
        val error = BackendError.Convex(code = "PermissionDenied", message = "No.", details = null)

        assertEquals(GroupError.NoAccess, error.toGroupError())
    }
}
