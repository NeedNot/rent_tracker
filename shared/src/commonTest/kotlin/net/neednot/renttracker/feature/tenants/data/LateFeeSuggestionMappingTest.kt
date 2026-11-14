package net.neednot.renttracker.feature.tenants.data

import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.data.AccessLevel
import net.neednot.renttracker.core.data.TenantSummaryEntity
import net.neednot.renttracker.core.data.tenantSummaryEntity
import net.neednot.renttracker.feature.tenants.domain.LateFeeSuggestion
import net.neednot.renttracker.feature.tenants.domain.callerPermissions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LateFeeSuggestionMappingTest {
    private val summary: TenantSummaryEntity =
        tenantSummaryEntity("t1").copy(lateFeeSuggestionMonth = "2026-03", lateFeeSuggestionCents = 5_000)

    private val permissions = callerPermissions(isOwner = false, debts = AccessLevel.AddEdit)

    @Test
    fun aServerSuggestionBecomesTheMonthAndAmount() {
        val suggestion = summary.toLateFeeSuggestion(isArchived = false, permissions, dismissedMonths = emptyList())

        assertEquals(LateFeeSuggestion(month = YearMonth(2026, 3), cents = 5_000), suggestion)
    }

    @Test
    fun noServerSuggestionMeansNone() {
        val none = tenantSummaryEntity("t1")

        assertNull(none.toLateFeeSuggestion(isArchived = false, permissions, dismissedMonths = emptyList()))
    }

    @Test
    fun aMonthDismissedOnThisPhoneIsHidden() {
        assertNull(summary.toLateFeeSuggestion(isArchived = false, permissions, dismissedMonths = listOf("2026-03")))
    }

    @Test
    fun aDismissedEarlierMonthDoesNotHideALaterOne() {
        val suggestion =
            summary.toLateFeeSuggestion(
                isArchived = false,
                permissions,
                dismissedMonths = listOf("2026-02"),
            )

        assertEquals(YearMonth(2026, 3), suggestion?.month)
    }

    @Test
    fun anArchivedTenantHasNoSuggestion() {
        assertNull(summary.toLateFeeSuggestion(isArchived = true, permissions, dismissedMonths = emptyList()))
    }

    @Test
    fun aCallerWhoCannotAddDebtsSeesNoSuggestion() {
        val readOnly = callerPermissions(isOwner = false, debts = AccessLevel.Read)

        assertNull(summary.toLateFeeSuggestion(isArchived = false, readOnly, dismissedMonths = emptyList()))
        assertNull(summary.toLateFeeSuggestion(isArchived = false, permissions = null, dismissedMonths = emptyList()))
    }
}
