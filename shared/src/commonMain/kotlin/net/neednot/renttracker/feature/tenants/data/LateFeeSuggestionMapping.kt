package net.neednot.renttracker.feature.tenants.data

import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.data.TenantSummaryEntity
import net.neednot.renttracker.feature.tenants.domain.CallerPermissions
import net.neednot.renttracker.feature.tenants.domain.LateFeeSuggestion

internal fun TenantSummaryEntity.toLateFeeSuggestion(
    isArchived: Boolean,
    permissions: CallerPermissions?,
    dismissedMonths: List<String>,
): LateFeeSuggestion? {
    val month = lateFeeSuggestionMonth
    val cents = lateFeeSuggestionCents
    val isHidden = isArchived || month in dismissedMonths || permissions?.canAddCharges != true
    return if (month == null || cents == null || isHidden) null else LateFeeSuggestion(YearMonth.parse(month), cents)
}
