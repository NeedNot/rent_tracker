package net.neednot.renttracker.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
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
) {
    val label = stringResource(Res.string.otp_field_label)
    BasicTextField(
        state = state,
        modifier = modifier.semantics { contentDescription = label },
        inputTransformation = InputTransformation.maxLength(CODE_LENGTH).then(DigitsOnly),
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        decorator = {
            val code = state.text.toString()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                repeat(CODE_LENGTH) { index ->
                    DigitBox(
                        digit = code.getOrNull(index),
                        isCurrent = index == code.length.coerceAtMost(CODE_LENGTH - 1),
                        isError = isError,
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
    isError: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val borderColor =
        when {
            isError -> colors.error
            isCurrent -> colors.primary
            else -> colors.outline
        }
    Box(
        modifier =
            Modifier
                .size(48.dp)
                .border(width = 1.dp, color = borderColor, shape = MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center,
    ) {
        if (digit != null) {
            Text(
                text = digit.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Monospace,
            )
        } else if (isCurrent) {
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
        Box(modifier = Modifier.width(2.dp).height(24.dp).background(color))
    }
}

@Preview
@Composable
private fun OtpFieldPreview() {
    RentTrackerTheme {
        OtpField(state = rememberTextFieldState("123"))
    }
}
