package net.neednot.renttracker.feature.tenants.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import net.neednot.renttracker.core.designsystem.components.DetailLayout
import net.neednot.renttracker.core.designsystem.components.DetailRow
import net.neednot.renttracker.core.designsystem.components.DialogCard
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingChangeUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingSheetBody
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.pending_changed_balance
import net.neednot.renttracker.resources.pending_changed_balance_label
import net.neednot.renttracker.resources.pending_changed_new_entry_label
import net.neednot.renttracker.resources.pending_field_why
import org.jetbrains.compose.resources.stringResource

private const val PART_SEPARATOR = " · "

@Composable
internal fun PendingSheetBodyContent(body: PendingSheetBody) {
    when (body) {
        is PendingSheetBody.Waiting -> {
            Column {
                body.fields.forEach { field ->
                    DetailRow(field.label.asString(), field.value.asString(), layout = DetailLayout.Inline)
                }
            }
            HelpText(body.message)
        }

        is PendingSheetBody.PossibleDuplicate -> {
            BodyHeading(body.title)
            HelpText(body.intro)
            body.matches.forEach { match ->
                DialogCard(leadingText = match.parts.joinedText(), trailingText = match.amount)
            }
        }

        is PendingSheetBody.TenantChanged -> {
            BodyHeading(body.title)
            HelpText(body.intro)
            body.changes.forEach { change -> ChangeCard(change) }
        }

        is PendingSheetBody.Rejected -> {
            DetailRow(
                stringResource(Res.string.pending_field_why),
                body.reason.asString(),
                layout = DetailLayout.Inline,
            )
            HelpText(body.notSaved)
        }
    }
}

@Composable
private fun BodyHeading(text: UiText) {
    Text(
        text = text.asString(),
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.titleSmall,
    )
}

@Composable
private fun HelpText(text: UiText) {
    Text(
        text = text.asString(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun ChangeCard(change: PendingChangeUiState) {
    when (change) {
        is PendingChangeUiState.Balance -> {
            DialogCard(
                leadingText = stringResource(Res.string.pending_changed_balance_label),
                trailingText =
                    stringResource(Res.string.pending_changed_balance, change.was.asString(), change.now.asString()),
            )
        }

        is PendingChangeUiState.NewEntry -> {
            DialogCard(
                leadingText = stringResource(Res.string.pending_changed_new_entry_label),
                trailingText = change.parts.joinedText(),
            )
        }
    }
}

@Composable
private fun List<UiText>.joinedText(): String = map { it.asString() }.joinToString(PART_SEPARATOR)
