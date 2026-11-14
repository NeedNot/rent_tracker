package net.neednot.renttracker.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp

@Composable
fun CalendarSquare(
    monthName: String,
    state: CalendarState,
    stateLabel: String,
    description: String,
    modifier: Modifier = Modifier,
    owed: String? = null,
    feeMarker: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = state.colors()
    val semantics =
        Modifier.clearAndSetSemantics {
            contentDescription = description
            if (onClick != null) role = Role.Button
        }
    val shape = MaterialTheme.shapes.small
    val border = colors.border?.let { BorderStroke(2.dp, it) }
    if (onClick == null) {
        Surface(
            modifier = modifier.then(semantics),
            shape = shape,
            color = colors.container,
            contentColor = colors.content,
            border = border,
        ) {
            SquareContent(monthName, state, stateLabel, owed, feeMarker)
        }
    } else {
        Surface(
            onClick = onClick,
            modifier = modifier.then(semantics),
            shape = shape,
            color = colors.container,
            contentColor = colors.content,
            border = border,
        ) {
            SquareContent(monthName, state, stateLabel, owed, feeMarker)
        }
    }
}

@Composable
private fun SquareContent(
    monthName: String,
    state: CalendarState,
    stateLabel: String,
    owed: String?,
    feeMarker: String?,
) {
    if (state == CalendarState.NotATenant) {
        Box(modifier = Modifier.defaultMinSize(minHeight = SquareMinHeight))
        return
    }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = SquareMinHeight)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = monthName, style = MaterialTheme.typography.labelLarge)
            feeMarker?.let { FeeMarker(it) }
        }
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                state.icon()?.let { Icon(imageVector = it, contentDescription = null, modifier = Modifier.size(13.dp)) }
                Text(text = stateLabel, style = MaterialTheme.typography.labelSmall)
            }
            owed?.let { Text(text = it, style = MaterialTheme.typography.labelSmall) }
        }
    }
}

@Composable
private fun FeeMarker(text: String) {
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.error,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 5.dp),
            maxLines = 1,
            softWrap = false,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

private val SquareMinHeight = 60.dp
