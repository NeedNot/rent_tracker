package net.neednot.renttracker.feature.home.data

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
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
import net.neednot.renttracker.feature.home.domain.ArchivedGroup
import net.neednot.renttracker.feature.home.domain.HomeError
import net.neednot.renttracker.feature.home.domain.HomeOverview
import net.neednot.renttracker.feature.home.domain.HomeRepository
import net.neednot.renttracker.model.groups.GroupsCreateArgs
import net.neednot.renttracker.model.home.HomeScreenArgs
import net.neednot.renttracker.model.home.HomeScreenResult
import net.neednot.renttracker.model.invites.InvitesAcceptArgs
import net.neednot.renttracker.model.invites.InvitesDeclineArgs
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
    private val homeInvitationDao = database.homeInvitationDao()

    override fun observeHome(): Flow<Synced<HomeOverview, HomeError>> =
        syncedFlow(cached = cachedHome, serverResults = homeScreenResults(), store = ::store)

    override fun observeArchivedGroups(): Flow<Synced<List<ArchivedGroup>, HomeError>> =
        syncedFlow(cached = cachedArchivedGroups, serverResults = homeScreenResults(), store = ::store)

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

    override suspend fun acceptInvitation(inviteId: String): Result<Unit, HomeError> =
        answerInvitation(
            name = ACCEPT_INVITE,
            args = ConvexJson.encodeArgs(InvitesAcceptArgs.serializer(), InvitesAcceptArgs(inviteId)),
        )

    override suspend fun declineInvitation(inviteId: String): Result<Unit, HomeError> =
        answerInvitation(
            name = DECLINE_INVITE,
            args = ConvexJson.encodeArgs(InvitesDeclineArgs.serializer(), InvitesDeclineArgs(inviteId)),
        )

    private suspend fun answerInvitation(
        name: String,
        args: JsonObject,
    ): Result<Unit, HomeError> {
        val answer =
            withTimeoutOrNull(BACKEND_TIMEOUT) {
                backend.mutation(name = name, args = args, resultSerializer = JsonElement.serializer())
            } ?: return Err(HomeError.Offline)
        return answer.map { }.mapError { it.toHomeError() }.reportingUnexpected()
    }

    private val cachedHome: Flow<HomeOverview> =
        combine(
            groupDao.observeAll(),
            groupSummaryDao.observeAll(),
            homeInvitationDao.observeAll(),
        ) { groups, summaries, invitations ->
            HomeOverview(
                groups = groups.toGroupOverviews(summaries),
                invitations = invitations.map { it.toHomeInvitation() },
                archivedGroupCount = groups.count { it.archivedAt != null },
            )
        }

    private val cachedArchivedGroups: Flow<List<ArchivedGroup>> =
        combine(groupDao.observeAll(), groupSummaryDao.observeAll()) { groups, summaries ->
            groups.toArchivedGroups(summaries)
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
            invitations = result.invitations.map { it.toEntity() },
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
        const val ACCEPT_INVITE = "invites:accept"
        const val DECLINE_INVITE = "invites:decline"

        // The Convex client queues calls while it has no connection instead of failing them.
        val BACKEND_TIMEOUT = 20.seconds
    }
}
