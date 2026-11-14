package net.neednot.renttracker.core.ui

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

sealed interface UiText {
    data class Plain(
        val value: String,
    ) : UiText

    data class Resource(
        val resource: StringResource,
        val args: List<Any> = emptyList(),
    ) : UiText

    data class Plural(
        val resource: PluralStringResource,
        val quantity: Int,
        val args: List<Any> = emptyList(),
    ) : UiText
}

@Composable
fun UiText.asString(): String =
    when (this) {
        is UiText.Plain -> value
        is UiText.Resource -> stringResource(resource, *args.toTypedArray())
        is UiText.Plural -> pluralStringResource(resource, quantity, *args.toTypedArray())
    }

suspend fun UiText.resolve(): String =
    when (this) {
        is UiText.Plain -> value
        is UiText.Resource -> getString(resource, *args.toTypedArray())
        is UiText.Plural -> getPluralString(resource, quantity, *args.toTypedArray())
    }
