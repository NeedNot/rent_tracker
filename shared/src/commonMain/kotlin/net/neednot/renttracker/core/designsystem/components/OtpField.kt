package net.neednot.renttracker.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.then
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import net.neednot.renttracker.core.designsystem.theme.Opacity
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.designsystem.theme.Sizes
import net.neednot.renttracker.core.designsystem.theme.Spacing
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.otp_field_label
import org.jetbrains.compose.resources.stringResource

private const val CODE_LENGTH = 6
private const val CURSOR_BLINK_MILLIS = 500L

private val DigitsOnly = InputTransformation { if (!asCharSequence().all(Char::isDigit)) revertAllChanges() }

@Composable
fun OtpField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    enabled: Boolean = true,
    onDone: () -> Unit = {},
) {
    val label = stringResource(Res.string.otp_field_label)
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    BasicTextField(
        state = state,
        modifier =
            modifier.alpha(if (enabled) 1f else Opacity.DISABLED).semantics {
                contentDescription = label
                contentType = ContentType.SmsOtpCode
            },
        enabled = enabled,
        inputTransformation = InputTransformation.maxLength(CODE_LENGTH).then(DigitsOnly),
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        onKeyboardAction = { onDone() },
        interactionSource = interactionSource,
        decorator = {
            val code = state.text.toString()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                repeat(CODE_LENGTH) { index ->
                    DigitBox(
                        digit = code.getOrNull(index),
                        isCurrent = enabled && index == code.length,
                        hasCursor = isFocused,
                        isError = isError,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
    )
}

@Composable
private fun DigitBox(
    digit: Char?,
    isCurrent: Boolean,
    hasCursor: Boolean,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val border =
        when {
            isError -> BorderStroke(Sizes.strokeThick, colors.error)
            isCurrent -> BorderStroke(Sizes.strokeThick, colors.primary)
            else -> BorderStroke(Sizes.strokeThin, colors.outline)
        }
    Box(
        modifier =
            modifier
                .height(Sizes.field)
                .background(color = colors.surfaceContainerLow, shape = MaterialTheme.shapes.medium)
                .border(border = border, shape = MaterialTheme.shapes.medium),
        contentAlignment = Alignment.Center,
    ) {
        if (digit != null) {
            Text(text = digit.toString(), color = colors.onSurface, style = MaterialTheme.typography.titleLarge)
        } else if (isCurrent && hasCursor) {
            BlinkingCursor(color = colors.onSurface)
        }
    }
}

@Composable
private fun BlinkingCursor(color: Color) {
    var isVisible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(CURSOR_BLINK_MILLIS)
            isVisible = !isVisible
        }
    }
    if (isVisible) {
        Box(modifier = Modifier.width(Sizes.strokeThick).height(Sizes.icon).background(color))
    }
}

@Preview
@Composable
private fun OtpFieldPreview() {
    RentTrackerTheme {
        OtpField(state = rememberTextFieldState("123"))
    }
}
