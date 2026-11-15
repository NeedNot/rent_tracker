package net.neednot.renttracker.feature.tenants.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerFieldUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingChangeUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingMatchUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingSheetBody
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.pending_changed_balance
import net.neednot.renttracker.resources.pending_changed_balance_label
import net.neednot.renttracker.resources.pending_changed_new_entry_label
import org.jetbrains.compose.resources.stringResource

private const val PART_SEPARATOR = " · "

@Composable
internal fun PendingSheetBodyContent(body: PendingSheetBody) {
    when (body) {
        is PendingSheetBody.Waiting -> {
            BodyText(body.message)
            Column { body.fields.forEach { field -> FieldRow(field) } }
        }

        is PendingSheetBody.PossibleDuplicate -> {
            BodyHeading(body.title)
            BodyText(body.intro)
            body.matches.forEach { match -> MatchCard(match) }
        }

        is PendingSheetBody.TenantChanged -> {
            BodyHeading(body.title)
            BodyText(body.intro)
            body.changes.forEach { change -> ChangeCard(change) }
        }

        is PendingSheetBody.Rejected -> {
            RejectionNotice(body.reason)
            BodyText(body.notSaved)
        }
    }
}

@Composable
private fun BodyHeading(text: UiText) {
    Text(
        text = text.asString(),
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun BodyText(text: UiText) {
    Text(
        text = text.asString(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun FieldRow(field: LedgerFieldUiState) {
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = field.label.asString(),
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(text = field.value.asString(), textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun MatchCard(match: PendingMatchUiState) {
    LineCard(label = match.parts.joinedText(), value = match.amount)
}

@Composable
private fun ChangeCard(change: PendingChangeUiState) {
    when (change) {
        is PendingChangeUiState.Balance -> {
            LineCard(
                label = stringResource(Res.string.pending_changed_balance_label),
                value =
                    stringResource(
                        Res.string.pending_changed_balance,
                        change.was.asString(),
                        change.now.asString(),
                    ),
            )
        }

        is PendingChangeUiState.NewEntry -> {
            LineCard(
                label = stringResource(Res.string.pending_changed_new_entry_label),
                value = change.parts.joinedText(),
            )
        }
    }
}

@Composable
private fun List<UiText>.joinedText(): String = map { it.asString() }.joinToString(PART_SEPARATOR)

@Composable
private fun LineCard(
    label: String,
    value: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(text = value, textAlign = TextAlign.End, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun RejectionNotice(reason: UiText) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(imageVector = Icons.Outlined.ErrorOutline, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(text = reason.asString(), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
