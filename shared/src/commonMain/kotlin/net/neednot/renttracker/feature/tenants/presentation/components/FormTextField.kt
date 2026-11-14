package net.neednot.renttracker.feature.tenants.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.asString

@Composable
internal fun FormTextField(
    label: String,
    text: String,
    onTextChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: UiText? = null,
    hint: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    prefix: String? = null,
) {
    val supportingText = error?.asString() ?: hint
    OutlinedTextField(
        value = text,
        onValueChange = onTextChanged,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        label = { Text(label) },
        prefix = prefix?.let { { Text(it) } },
        supportingText = supportingText?.let { { Text(it) } },
        isError = error != null,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
    )
}

@Composable
internal fun MoneyField(
    label: String,
    text: String,
    onTextChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: UiText? = null,
    hint: String? = null,
    enabled: Boolean = true,
) {
    FormTextField(
        label = label,
        text = text,
        onTextChanged = onTextChanged,
        modifier = modifier,
        error = error,
        hint = hint,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        prefix = "$",
    )
}

@Composable
internal fun WholeNumberField(
    label: String,
    text: String,
    onTextChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: UiText? = null,
    hint: String? = null,
    enabled: Boolean = true,
) {
    FormTextField(
        label = label,
        text = text,
        onTextChanged = onTextChanged,
        modifier = modifier,
        error = error,
        hint = hint,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
    )
}
