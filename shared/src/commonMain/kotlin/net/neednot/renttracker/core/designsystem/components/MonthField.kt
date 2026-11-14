package net.neednot.renttracker.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Month
import kotlinx.datetime.YearMonth
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.number
import net.neednot.renttracker.core.designsystem.format.formatMonth
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.common_cancel
import net.neednot.renttracker.resources.common_next_year
import net.neednot.renttracker.resources.common_previous_year
import org.jetbrains.compose.resources.stringResource

private const val MONTHS_PER_ROW = 3

@Composable
internal fun MonthField(
    label: String,
    month: YearMonth,
    onMonthChanged: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    error: UiText? = null,
    enabled: Boolean = true,
) {
    var isPickerOpen by rememberSaveable { mutableStateOf(false) }
    PickerField(
        label = label,
        value = formatMonth(month),
        onOpenClicked = { isPickerOpen = true },
        modifier = modifier,
        error = error,
        enabled = enabled,
    )
    if (isPickerOpen) {
        MonthPickerDialog(
            title = label,
            selected = month,
            onMonthChosen = { chosen ->
                isPickerOpen = false
                onMonthChanged(chosen)
            },
            onDismissed = { isPickerOpen = false },
        )
    }
}

@Composable
private fun MonthPickerDialog(
    title: String,
    selected: YearMonth,
    onMonthChosen: (YearMonth) -> Unit,
    onDismissed: () -> Unit,
) {
    var year by rememberSaveable { mutableIntStateOf(selected.year) }
    AlertDialog(
        onDismissRequest = onDismissed,
        confirmButton = {
            TextButton(onClick = onDismissed) { Text(stringResource(Res.string.common_cancel)) }
        },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                YearStepper(year = year, onYearChanged = { year = it })
                Month.entries.chunked(MONTHS_PER_ROW).forEach { monthsInRow ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        monthsInRow.forEach { month ->
                            MonthButton(
                                month = month,
                                year = year,
                                isSelected = selected == YearMonth(year, month),
                                onClick = { onMonthChosen(YearMonth(year, month)) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun YearStepper(
    year: Int,
    onYearChanged: (Int) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onYearChanged(year - 1) }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(Res.string.common_previous_year),
            )
        }
        Text(
            text = year.toString(),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
        )
        IconButton(onClick = { onYearChanged(year + 1) }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(Res.string.common_next_year),
            )
        }
    }
}

@Composable
private fun MonthButton(
    month: Month,
    year: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spokenName = "${MonthNames.ENGLISH_FULL.names[month.number - 1]} $year"
    val colors =
        if (isSelected) {
            ButtonDefaults.textButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        } else {
            ButtonDefaults.textButtonColors()
        }
    TextButton(
        onClick = onClick,
        modifier =
            modifier.heightIn(min = 48.dp).semantics {
                contentDescription = spokenName
                selected = isSelected
            },
        colors = colors,
        contentPadding = ButtonDefaults.TextButtonContentPadding,
    ) {
        Text(text = MonthNames.ENGLISH_ABBREVIATED.names[month.number - 1], maxLines = 1)
    }
}
