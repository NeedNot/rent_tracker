package net.neednot.renttracker.feature.tenants.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.tenants.presentation.uistate.CalendarUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.calendar_caption
import net.neednot.renttracker.resources.calendar_next_year
import net.neednot.renttracker.resources.calendar_previous_year
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CalendarCard(
    calendar: CalendarUiState,
    onPreviousYearClicked: () -> Unit,
    onNextYearClicked: () -> Unit,
    onMonthClicked: (month: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            YearHeader(calendar.year, onPreviousYearClicked, onNextYearClicked)
            calendar.error?.let { error ->
                Text(
                    text = error.asString(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            CalendarGrid(squares = calendar.squares, onMonthClicked = onMonthClicked)
            Text(
                text = stringResource(Res.string.calendar_caption),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun YearHeader(
    year: String,
    onPreviousYearClicked: () -> Unit,
    onNextYearClicked: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPreviousYearClicked) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = stringResource(Res.string.calendar_previous_year))
        }
        Text(
            text = year,
            modifier = Modifier.weight(1f).semantics { heading() },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
        )
        IconButton(onClick = onNextYearClicked) {
            Icon(Icons.Filled.ChevronRight, contentDescription = stringResource(Res.string.calendar_next_year))
        }
    }
}
