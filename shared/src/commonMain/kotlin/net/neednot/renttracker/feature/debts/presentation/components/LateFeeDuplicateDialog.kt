package net.neednot.renttracker.feature.debts.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.debts.presentation.uistate.LateFeeDuplicateUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.charge_duplicate_add_anyway
import net.neednot.renttracker.resources.charge_duplicate_go_back
import net.neednot.renttracker.resources.charge_duplicate_line
import net.neednot.renttracker.resources.charge_duplicate_question
import net.neednot.renttracker.resources.charge_duplicate_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LateFeeDuplicateDialog(
    dialog: LateFeeDuplicateUiState,
    onConfirmed: () -> Unit,
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismissed,
        modifier = modifier,
        confirmButton = {
            TextButton(onClick = onConfirmed) { Text(stringResource(Res.string.charge_duplicate_add_anyway)) }
        },
        dismissButton = {
            TextButton(onClick = onDismissed) { Text(stringResource(Res.string.charge_duplicate_go_back)) }
        },
        title = { Text(stringResource(Res.string.charge_duplicate_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(dialog.intro.asString())
                dialog.matches.forEach { match ->
                    Text(
                        stringResource(Res.string.charge_duplicate_line, match.date, match.by.asString(), match.amount),
                    )
                }
                Text(stringResource(Res.string.charge_duplicate_question))
            }
        },
    )
}
