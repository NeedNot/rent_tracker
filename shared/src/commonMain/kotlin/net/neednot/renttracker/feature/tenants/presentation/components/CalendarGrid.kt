package net.neednot.renttracker.feature.tenants.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.designsystem.components.CalendarSquare
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.tenants.presentation.uistate.CalendarSquareUiState

private const val COLUMNS = 3

@Composable
internal fun CalendarGrid(
    squares: List<CalendarSquareUiState>,
    onMonthClicked: (month: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        squares.chunked(COLUMNS).forEach { rowSquares ->
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                rowSquares.forEach { square ->
                    Square(square, onMonthClicked, Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

@Composable
private fun Square(
    square: CalendarSquareUiState,
    onMonthClicked: (month: String) -> Unit,
    modifier: Modifier,
) {
    val description = square.description.map { it.asString() }.joinToString(", ")
    CalendarSquare(
        monthName = square.monthName,
        state = square.state,
        stateLabel = square.stateLabel.asString(),
        description = description,
        modifier = modifier,
        owed = square.owed?.asString(),
        feeMarker = square.feeMarker?.asString(),
        onClick = if (square.isTappable) ({ onMonthClicked(square.month) }) else null,
    )
}
