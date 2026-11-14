package net.neednot.renttracker.core.designsystem.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.designsystem.format.formatDate
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.common_cancel
import net.neednot.renttracker.resources.common_ok
import org.jetbrains.compose.resources.stringResource

private const val MILLIS_PER_DAY = 86_400_000L

@Composable
internal fun DateField(
    label: String,
    date: LocalDate?,
    onDateChanged: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    error: UiText? = null,
    hint: String? = null,
    enabled: Boolean = true,
    onCleared: (() -> Unit)? = null,
) {
    var isPickerOpen by rememberSaveable { mutableStateOf(false) }
    PickerField(
        label = label,
        value = date?.let(::formatDate).orEmpty(),
        onOpenClicked = { isPickerOpen = true },
        modifier = modifier,
        error = error,
        hint = hint,
        enabled = enabled,
        onCleared = onCleared,
    )
    if (isPickerOpen) {
        DatePickerPopup(
            initialDate = date,
            onDateChosen = { chosen ->
                isPickerOpen = false
                onDateChanged(chosen)
            },
            onDismissed = { isPickerOpen = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerPopup(
    initialDate: LocalDate?,
    onDateChosen: (LocalDate) -> Unit,
    onDismissed: () -> Unit,
) {
    // The Material date picker speaks UTC midnights; a calendar date maps to one without any time zone.
    val pickerState =
        rememberDatePickerState(initialSelectedDateMillis = initialDate?.let { it.toEpochDays() * MILLIS_PER_DAY })
    DatePickerDialog(
        onDismissRequest = onDismissed,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onDateChosen(LocalDate.fromEpochDays(millis / MILLIS_PER_DAY))
                    }
                },
                enabled = pickerState.selectedDateMillis != null,
            ) {
                Text(stringResource(Res.string.common_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissed) { Text(stringResource(Res.string.common_cancel)) }
        },
    ) {
        DatePicker(state = pickerState)
    }
}
