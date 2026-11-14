package net.neednot.renttracker.feature.payments.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.neednot.renttracker.core.data.GroupEntity
import net.neednot.renttracker.core.data.MembershipEntity
import net.neednot.renttracker.core.data.RentTrackerDatabase
import kotlin.time.Instant

internal fun RentTrackerDatabase.observeGroupToday(
    groupId: String,
    asOf: Flow<Long>,
): Flow<GroupToday?> =
    combine(groupDao().observeById(groupId), asOf) { group, asOfMillis ->
        group?.let { GroupToday(group, group.localDateAt(asOfMillis)) }
    }

internal fun RentTrackerDatabase.observeCaller(groupId: String): Flow<MembershipEntity?> =
    membershipDao().observeByGroupId(groupId)

private fun GroupEntity.localDateAt(epochMillis: Long): LocalDate =
    Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.of(timeZone)).date
