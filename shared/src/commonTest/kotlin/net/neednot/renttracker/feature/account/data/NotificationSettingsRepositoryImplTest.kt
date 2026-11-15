package net.neednot.renttracker.feature.account.data

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
import net.neednot.renttracker.core.data.FakeLateRentAlerts
import net.neednot.renttracker.core.telemetry.NoOpTelemetry
import net.neednot.renttracker.feature.account.domain.AccountError
import net.neednot.renttracker.feature.account.domain.GroupNotificationSetting
import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationSettingsRepositoryImplTest {
    private val backend = FakeBackend()
    private val lateRentAlerts = FakeLateRentAlerts()
    private val repository = NotificationSettingsRepositoryImpl(backend, lateRentAlerts, NoOpTelemetry())

    @Test
    fun listsTheServersGroupSettings() =
        runTest {
            backend.answerSubscription(
                "notificationSettings:list",
                Ok(Json.parseToJsonElement("""[{"groupId":"g1","groupName":"1st Street","notificationsOn":false}]""")),
            )

            repository.observeSettings().test {
                assertEquals(
                    Ok(listOf(GroupNotificationSetting("g1", "1st Street", notificationsOn = false))),
                    awaitItem(),
                )
            }
        }

    @Test
    fun aFailedListBecomesAnAccountError() =
        runTest {
            backend.answerSubscription("notificationSettings:list", Err(BackendError.Server("down")))

            repository.observeSettings().test {
                assertEquals(Err(AccountError.Unexpected("down")), awaitItem())
            }
        }

    @Test
    fun mutingSendsTheGroupAndTheNewValue() =
        runTest {
            backend.mutationAnswers["notificationSettings:set"] = Ok(JsonNull)

            assertEquals(Ok(Unit), repository.setNotificationsOn("g1", notificationsOn = false))

            val expected =
                buildJsonObject {
                    put("groupId", "g1")
                    put("notificationsOn", false)
                }
            assertEquals(listOf("notificationSettings:set" to expected), backend.mutations)
        }

    @Test
    fun aChangeWithoutAConnectionReportsOffline() =
        runTest {
            backend.isReachable = false

            assertEquals(Err(AccountError.Offline), repository.setNotificationsOn("g1", notificationsOn = true))
        }

    @Test
    fun saysWhenThePhoneBlocksNotifications() {
        lateRentAlerts.blockedOnPhone = true

        assertEquals(true, repository.areNotificationsBlockedOnPhone())
    }
}
