package net.neednot.renttracker.core.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import net.neednot.renttracker.core.backend.FakeBackend
import net.neednot.renttracker.model.shared.Group
import net.neednot.renttracker.model.shared.GroupPaymentOrder
import net.neednot.renttracker.model.shared.Tenant
import net.neednot.renttracker.model.shared.TenantStatus
import net.neednot.renttracker.model.shared.TenantSummary
import net.neednot.renttracker.model.shared.TenantSummaryState
import net.neednot.renttracker.model.tenants.Payment
import net.neednot.renttracker.model.tenants.TenantsScreenResult

const val TENANT_SCREEN = "tenants:screen"
const val LOG_PAYMENT = "payments:log"

fun inMemoryDatabase(): RentTrackerDatabase =
    Room
        .inMemoryDatabaseBuilder<RentTrackerDatabase>()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()

fun tenantScreen(
    tenantId: String = "t1",
    groupId: String = "g1",
    tenantVersion: Long = 1,
    status: TenantStatus = TenantStatus.Active,
    payments: List<Payment> = emptyList(),
    rentOwedCents: Long = 55_000,
) = TenantsScreenResult(
    accounts = emptyList(),
    allocations = emptyList(),
    calendarMonths = emptyList(),
    debts = emptyList(),
    groups =
        listOf(
            Group(
                creationTime = 1.0,
                id = groupId,
                graceDays = 5,
                name = "1st Street",
                ownerId = "owner",
                paymentOrder = GroupPaymentOrder.RentFirst,
                timeZone = "America/Chicago",
                version = 1,
            ),
        ),
    ledgerRows = emptyList(),
    memberships = emptyList(),
    payments = payments,
    rentTerms = emptyList(),
    tenantSummaries =
        listOf(
            TenantSummary(
                creditCents = 0,
                lateFeesOwedCents = 0,
                otherOwedCents = 0,
                rentOwedCents = rentOwedCents,
                state = TenantSummaryState.Owes,
                tenantId = tenantId,
            ),
        ),
    tenants =
        listOf(
            Tenant(
                creationTime = 2.0,
                id = tenantId,
                groupId = groupId,
                moveIn = "2026-01-01",
                name = "Apt 3B",
                status = status,
                version = tenantVersion,
            ),
        ),
)

fun serverPayment(
    id: String,
    tenantId: String = "t1",
    version: Long = 1,
) = Payment(
    creationTime = 3.0,
    id = id,
    amountCents = 150_000,
    clientId = "server-$id",
    dateReceived = "2026-03-02",
    loggedBy = "sam",
    tenantId = tenantId,
    version = version,
)

fun FakeBackend.answerTenantScreen(result: TenantsScreenResult) {
    answerSubscription(TENANT_SCREEN, Ok(Json.encodeToJsonElement(TenantsScreenResult.serializer(), result)))
}

val marchSecondPayment = PendingPaymentDetails(150_000, "2026-03-02", PaymentMethod.Check, null)
