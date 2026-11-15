package net.neednot.renttracker.feature.group.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.group_unarchive
import org.jetbrains.compose.resources.stringResource

@Composable
fun ArchivedBanner(
    text: UiText,
    onUnarchiveClicked: (() -> Unit)?,
    isWorking: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Inventory2,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Text(text = text.asString(), style = MaterialTheme.typography.bodyMedium)
            }
            if (onUnarchiveClicked != null) {
                Button(
                    onClick = onUnarchiveClicked,
                    enabled = !isWorking,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(stringResource(Res.string.group_unarchive))
                }
            }
        }
    }
}
