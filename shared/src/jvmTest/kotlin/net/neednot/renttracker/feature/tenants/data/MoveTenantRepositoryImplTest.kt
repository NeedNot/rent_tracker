package net.neednot.renttracker.feature.tenants.data

import app.cash.turbine.test
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.core.backend.FakeBackend
import net.neednot.renttracker.core.data.FakeAsOfClock
import net.neednot.renttracker.core.data.PaymentOrder
import net.neednot.renttracker.core.telemetry.NoOpTelemetry
import net.neednot.renttracker.feature.tenants.domain.MoveTarget
import net.neednot.renttracker.feature.tenants.domain.TenantError
import net.neednot.renttracker.model.shared.Group
import net.neednot.renttracker.model.shared.GroupPaymentOrder
import net.neednot.renttracker.model.shared.GroupSummary
import net.neednot.renttracker.model.tenants.TenantsMoveTargetsResult
import kotlin.test.Test
import kotlin.test.assertEquals

class MoveTenantRepositoryImplTest {
    private val backend = FakeBackend()
    private val repository =
        MoveTenantRepositoryImpl(
            backend = backend,
            asOfClock = FakeAsOfClock(TEN_O_CLOCK_UTC),
            telemetry = NoOpTelemetry(),
        )

    @Test
    fun movingATenantSendsTheTargetGroupAndTheExpectedVersion() =
        runTest {
            backend.mutationAnswers[MOVE_TENANT] = Ok(JsonNull)

            assertEquals(Ok(Unit), repository.move("t1", toGroupId = "g2", expectedVersion = 7))

            val expectedArgs =
                buildJsonObject {
                    put("expectedVersion", 7)
                    put("tenantId", "t1")
                    put("toGroupId", "g2")
                }
            assertEquals(listOf(MOVE_TENANT to expectedArgs), backend.mutations)
        }

    @Test
    fun aMoveConflictAndADeniedMoveBecomeTenantErrors() =
        runTest {
            val cases =
                listOf(
                    "EditConflict" to TenantError.EditConflict(MESSAGE),
                    "PermissionDenied" to TenantError.PermissionDenied(MESSAGE),
                )

            cases.forEach { (code, expected) ->
                backend.mutationAnswers[MOVE_TENANT] = Err(BackendError.Convex(code, MESSAGE, null))

                assertEquals(Err(expected), repository.move("t1", "g2", 7), "code = $code")
            }
        }

    @Test
    fun movingWithoutAConnectionReportsOffline() =
        runTest {
            backend.isReachable = false

            assertEquals(Err(TenantError.Offline), repository.move("t1", "g2", 7))
        }

    @Test
    fun moveTargetsAreAskedForTheTenantAtTheCurrentHourAndListTheirTenantCountAndSettings() =
        runTest {
            repository.observeTargets("t1").test {
                val maple =
                    Group(
                        creationTime = 1.25,
                        id = "g2",
                        graceDays = 3,
                        name = "Maple Court",
                        ownerId = "owner",
                        paymentOrder = GroupPaymentOrder.ByDate,
                        timeZone = "America/Chicago",
                        version = 1,
                    )
                val result =
                    TenantsMoveTargetsResult(
                        groups = listOf(maple),
                        groupSummaries =
                            listOf(
                                GroupSummary(
                                    activeTenantCount = 4,
                                    archivedOwedCents = 0,
                                    archivedTenantCount = 0,
                                    groupId = "g2",
                                    lateTenantCount = 0,
                                    totalOwedCents = 0,
                                ),
                            ),
                    )
                backend.answerSubscription(
                    MOVE_TARGETS,
                    Ok(Json.encodeToJsonElement(TenantsMoveTargetsResult.serializer(), result)),
                )

                val expected =
                    MoveTarget("g2", "Maple Court", tenantCount = 4, paymentOrder = PaymentOrder.ByDate, graceDays = 3)
                assertEquals(Ok(listOf(expected)), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            val expectedArgs =
                buildJsonObject {
                    put("asOf", TEN_O_CLOCK_UTC)
                    put("tenantId", "t1")
                }
            assertEquals(listOf(MOVE_TARGETS to expectedArgs), backend.subscriptions)
        }

    @Test
    fun aDeniedMoveTargetQueryBecomesAPermissionError() =
        runTest {
            repository.observeTargets("t1").test {
                backend.answerSubscription(MOVE_TARGETS, Err(BackendError.Convex("PermissionDenied", MESSAGE, null)))

                assertEquals(Err(TenantError.PermissionDenied(MESSAGE)), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    private companion object {
        const val MOVE_TENANT = "tenants:move"
        const val MOVE_TARGETS = "tenants:moveTargets"
        const val MESSAGE = "Sam changed this tenant while you were editing."
        const val TEN_O_CLOCK_UTC = 1_794_736_800_000L
    }
}
