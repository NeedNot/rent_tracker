package net.neednot.renttracker.feature.tenants.presentation.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import net.neednot.renttracker.core.designsystem.components.AppIcon
import net.neednot.renttracker.core.designsystem.components.Banner
import net.neednot.renttracker.core.designsystem.components.BannerKind
import net.neednot.renttracker.core.designsystem.components.RtCard
import net.neednot.renttracker.core.designsystem.theme.Spacing
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.tenants.presentation.uistate.CalendarUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.calendar_caption
import net.neednot.renttracker.resources.calendar_next_year
import net.neednot.renttracker.resources.calendar_previous_year
import net.neednot.renttracker.resources.ic_chevron_left
import net.neednot.renttracker.resources.ic_chevron_right
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CalendarCard(
    calendar: CalendarUiState,
    actions: CalendarActions,
    modifier: Modifier = Modifier,
) {
    RtCard(
        modifier = modifier,
        contentPadding = PaddingValues(start = Spacing.lg, top = Spacing.sm, end = Spacing.lg, bottom = Spacing.lg),
        contentSpacing = Spacing.sm,
    ) {
        YearStepper(calendar.year, calendar.hasPreviousYear, actions)
        calendar.error?.let { error -> Banner(text = error.asString(), kind = BannerKind.Error) }
        CalendarGrid(squares = calendar.squares, onMonthClicked = actions.onMonthClicked)
        Text(
            text = stringResource(Res.string.calendar_caption),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun YearStepper(
    year: String,
    hasPreviousYear: Boolean,
    actions: CalendarActions,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = actions.onPreviousYearClicked, enabled = hasPreviousYear) {
            AppIcon(
                icon = Res.drawable.ic_chevron_left,
                contentDescription = stringResource(Res.string.calendar_previous_year),
            )
        }
        Text(
            text = year,
            modifier = Modifier.weight(1f).semantics { heading() },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
        )
        IconButton(onClick = actions.onNextYearClicked) {
            AppIcon(
                icon = Res.drawable.ic_chevron_right,
                contentDescription = stringResource(Res.string.calendar_next_year),
            )
        }
    }
}
