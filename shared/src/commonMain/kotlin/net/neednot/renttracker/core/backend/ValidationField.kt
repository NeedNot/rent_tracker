package net.neednot.renttracker.core.backend

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

fun BackendError.Convex.validationField(): String? =
    (details as? JsonObject)?.get("field")?.jsonPrimitive?.contentOrNull
