package net.neednot.renttracker.core.backend

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlin.math.abs
import kotlin.math.floor

/**
 * Convex stores every `v.number()` as a float64, so whole numbers such as cents can arrive as `150000.0`.
 * This converts between that wire format and the Long fields our models use.
 */
object ConvexJson {
    private const val MAX_SAFE_INTEGER = 9_007_199_254_740_991.0

    private val json = Json { ignoreUnknownKeys = true }

    fun <T> decode(
        serializer: KSerializer<T>,
        value: JsonElement,
    ): T = json.decodeFromJsonElement(serializer, normalize(value))

    fun <T> encodeArgs(
        serializer: KSerializer<T>,
        args: T,
    ): JsonObject = json.encodeToJsonElement(serializer, args).jsonObject

    fun normalize(element: JsonElement): JsonElement =
        when (element) {
            is JsonObject -> JsonObject(element.mapValues { (_, value) -> normalize(value) })
            is JsonArray -> JsonArray(element.map(::normalize))
            is JsonPrimitive -> element.withWholeNumberAsInteger()
        }

    // The Android client sends a Kotlin Long as an int64, which `v.number()` rejects, so numbers go out as Double.
    fun toClientArgs(args: JsonObject): Map<String, Any?> = args.mapValues { (_, value) -> value.toClientValue() }

    fun parseError(data: String): BackendError =
        try {
            val error = json.decodeFromString(ConvexErrorData.serializer(), data)
            BackendError.Convex(code = error.code, message = error.message, details = error.details)
        } catch (exception: SerializationException) {
            BackendError.Internal("Unrecognized ConvexError data: $data (${exception.message})")
        }

    private fun JsonPrimitive.withWholeNumberAsInteger(): JsonPrimitive {
        val number = if (isString) null else doubleOrNull
        val isWholeNumber = number != null && number == floor(number) && abs(number) <= MAX_SAFE_INTEGER
        return if (isWholeNumber) JsonPrimitive(number.toLong()) else this
    }

    private fun JsonElement.toClientValue(): Any? =
        when (this) {
            is JsonObject -> mapValues { (_, value) -> value.toClientValue() }
            is JsonArray -> map { it.toClientValue() }
            is JsonNull -> null
            is JsonPrimitive -> toClientPrimitive()
        }

    private fun JsonPrimitive.toClientPrimitive(): Any =
        if (isString) content else booleanOrNull ?: requireNotNull(doubleOrNull) { "Not a JSON number: $content" }

    @Serializable
    private data class ConvexErrorData(
        val code: String,
        val message: String,
        val details: JsonElement? = null,
    )
}
