package net.neednot.renttracker.core.backend

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals

class ConvexJsonTest {
    @Serializable
    private data class Payment(
        val amountCents: Long,
        val note: String?,
    )

    @Test
    fun decodesFloatWholeNumbersIntoLongFields() {
        val value =
            buildJsonObject {
                put("amountCents", JsonPrimitive(150000.0))
                put("note", JsonNull)
            }

        assertEquals(Payment(amountCents = 150000, note = null), ConvexJson.decode(Payment.serializer(), value))
    }

    @Test
    fun keepsFractionalNumbersAndStringsUnchanged() {
        val value =
            buildJsonArray {
                add(JsonPrimitive(1.5))
                add(JsonPrimitive("150000.0"))
            }

        assertEquals(value, ConvexJson.normalize(value))
    }

    @Test
    fun sendsNumbersAsDoublesSoConvexReceivesFloat64() {
        val args =
            buildJsonObject {
                put("amountCents", 150000L)
                put("tenantId", "t1")
                put("confirmed", true)
                put("note", JsonNull)
                put("months", buildJsonArray { add(JsonPrimitive("2026-03")) })
            }

        val expected =
            mapOf(
                "amountCents" to 150000.0,
                "tenantId" to "t1",
                "confirmed" to true,
                "note" to null,
                "months" to listOf("2026-03"),
            )
        assertEquals(expected, ConvexJson.toClientArgs(args))
    }

    @Test
    fun parsesOurConvexErrorShape() {
        val error =
            ConvexJson.parseError(
                """{"code":"EditConflict","message":"Sam changed this payment","details":{"by":"a1"}}""",
            )

        assertEquals(
            BackendError.Convex(
                code = "EditConflict",
                message = "Sam changed this payment",
                details = buildJsonObject { put("by", "a1") },
            ),
            error,
        )
    }

    @Test
    fun reportsUnrecognizedConvexErrorDataAsInternal() {
        val error = ConvexJson.parseError("\"plain text\"")

        assertEquals(true, error is BackendError.Internal)
    }
}
