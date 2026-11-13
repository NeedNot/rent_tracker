package net.neednot.renttracker.feature.home.data

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onErr
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.TimeZone
import kotlinx.serialization.builtins.serializer
import net.neednot.renttracker.core.backend.Backend
import net.neednot.renttracker.core.backend.ConvexJson
import net.neednot.renttracker.core.data.AsOfClock
import net.neednot.renttracker.core.data.RentTrackerDatabase
import net.neednot.renttracker.core.data.ScreenResultStore
import net.neednot.renttracker.core.data.toEntity
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.sync.syncedFlow
import net.neednot.renttracker.core.telemetry.Severity
import net.neednot.renttracker.core.telemetry.Telemetry
import net.neednot.renttracker.feature.home.domain.GroupOverview
import net.neednot.renttracker.feature.home.domain.HomeError
import net.neednot.renttracker.feature.home.domain.HomeRepository
import net.neednot.renttracker.model.groups.GroupsCreateArgs
import net.neednot.renttracker.model.home.HomeScreenArgs
import net.neednot.renttracker.model.home.HomeScreenResult
import kotlin.time.Duration.Companion.seconds

class HomeRepositoryImpl(
    private val backend: Backend,
    private val asOfClock: AsOfClock,
    database: RentTrackerDatabase,
    private val screenResultStore: ScreenResultStore,
    private val phoneTimeZone: () -> TimeZone,
    private val telemetry: Telemetry,
) : HomeRepository {
    private val groupDao = database.groupDao()
    private val groupSummaryDao = database.groupSummaryDao()

    override fun observeGroups(): Flow<Synced<List<GroupOverview>, HomeError>> =
        syncedFlow(cached = cachedGroups(), serverResults = homeScreenResults(), store = ::store)

    override suspend fun createGroup(name: String): Result<String, HomeError> {
        val args = GroupsCreateArgs(name = name, timeZone = phoneTimeZone().id)
        val answer =
            withTimeoutOrNull(BACKEND_TIMEOUT) {
                backend.mutation(
                    name = CREATE_GROUP,
                    args = ConvexJson.encodeArgs(GroupsCreateArgs.serializer(), args),
                    resultSerializer = String.serializer(),
                )
            } ?: return Err(HomeError.Offline)
        return answer.mapError { it.toHomeError() }.reportingUnexpected()
    }

    private fun cachedGroups(): Flow<List<GroupOverview>> =
        combine(groupDao.observeAll(), groupSummaryDao.observeAll()) { groups, summaries ->
            groups.toGroupOverviews(summaries)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun homeScreenResults(): Flow<Result<HomeScreenResult, HomeError>> =
        asOfClock.asOf
            .flatMapLatest { asOf ->
                backend.subscribe(
                    name = HOME_SCREEN,
                    args = ConvexJson.encodeArgs(HomeScreenArgs.serializer(), HomeScreenArgs(asOf = asOf)),
                    resultSerializer = HomeScreenResult.serializer(),
                )
            }.map { result -> result.mapError { it.toHomeError() }.reportingUnexpected() }

    private suspend fun store(result: HomeScreenResult) {
        screenResultStore.replaceHome(
            groups = result.groups.map { it.toEntity() },
            groupSummaries = result.groupSummaries.map { it.toEntity() },
        )
    }

    private fun <T> Result<T, HomeError>.reportingUnexpected(): Result<T, HomeError> =
        onErr { error ->
            if (error is HomeError.Unexpected) telemetry.log(TAG, error.message, Severity.Error)
        }

    private companion object {
        const val TAG = "HomeRepository"
        const val HOME_SCREEN = "home:screen"
        const val CREATE_GROUP = "groups:create"

        // The Convex client queues calls while it has no connection instead of failing them.
        val BACKEND_TIMEOUT = 20.seconds
    }
}
