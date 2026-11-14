package net.neednot.renttracker.feature.payments.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.payments.presentation.uistate.DuplicateDialogUiState
import net.neednot.renttracker.feature.payments.presentation.uistate.DuplicateMatchUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.payment_duplicate_go_back
import net.neednot.renttracker.resources.payment_duplicate_line
import net.neednot.renttracker.resources.payment_duplicate_save_anyway
import org.jetbrains.compose.resources.stringResource

private const val PART_SEPARATOR = " · "

@Composable
internal fun DuplicateDialog(
    dialog: DuplicateDialogUiState,
    onConfirmed: () -> Unit,
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismissed,
        modifier = modifier,
        confirmButton = {
            TextButton(onClick = onConfirmed) { Text(stringResource(Res.string.payment_duplicate_save_anyway)) }
        },
        dismissButton = {
            TextButton(onClick = onDismissed) { Text(stringResource(Res.string.payment_duplicate_go_back)) }
        },
        title = { Text(dialog.title.asString()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(dialog.intro.asString())
                dialog.matches.forEach { match -> Text(match.line()) }
                Text(dialog.question.asString())
            }
        },
    )
}

@Composable
private fun DuplicateMatchUiState.line(): String {
    val parts = listOfNotNull(kind?.asString(), date, method?.let { stringResource(it.label) }, by.asString())
    return stringResource(Res.string.payment_duplicate_line, parts.joinToString(PART_SEPARATOR), amount)
}
