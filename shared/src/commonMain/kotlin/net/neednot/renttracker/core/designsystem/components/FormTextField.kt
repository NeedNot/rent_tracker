package net.neednot.renttracker.core.designsystem.components

import androidx.compose.foundation.text.KeyboardOptions
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
    labelNote: String? = null,
    error: UiText? = null,
    hint: String? = null,
    counter: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    prefix: String? = null,
) {
    FieldBox(
        label = label,
        value = text,
        onValueChange = onTextChanged,
        modifier = modifier,
        labelNote = labelNote,
        error = error?.asString(),
        hint = hint,
        counter = counter,
        placeholder = placeholder,
        prefix = prefix,
        enabled = enabled,
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = keyboardOptions,
    )
}

@Composable
internal fun MoneyField(
    label: String,
    text: String,
    onTextChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    labelNote: String? = null,
    error: UiText? = null,
    hint: String? = null,
    enabled: Boolean = true,
) {
    FormTextField(
        label = label,
        text = text,
        onTextChanged = { typed -> onTextChanged(typed.filter { it.isDigit() || it == '.' || it == ',' }) },
        modifier = modifier,
        labelNote = labelNote,
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
    labelNote: String? = null,
    error: UiText? = null,
    hint: String? = null,
    enabled: Boolean = true,
) {
    FormTextField(
        label = label,
        text = text,
        onTextChanged = { typed -> onTextChanged(typed.filter { it.isDigit() }) },
        modifier = modifier,
        labelNote = labelNote,
        error = error,
        hint = hint,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
    )
}
