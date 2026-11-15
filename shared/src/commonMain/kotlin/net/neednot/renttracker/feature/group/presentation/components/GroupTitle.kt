package net.neednot.renttracker.feature.group.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.group_archived_subtitle
import net.neednot.renttracker.resources.group_view_only
import org.jetbrains.compose.resources.stringResource

@Composable
fun GroupTitle(
    name: String,
    isViewOnly: Boolean,
    isArchived: Boolean,
    modifier: Modifier = Modifier,
) {
    if (isArchived) {
        Column(modifier = modifier) {
            Text(text = name, style = MaterialTheme.typography.titleLarge)
            Text(
                text = stringResource(Res.string.group_archived_subtitle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        return
    }
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = name, style = MaterialTheme.typography.titleLarge)
        if (isViewOnly) ViewOnlyLabel()
    }
}

@Composable
private fun ViewOnlyLabel() {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.heightIn(min = 24.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(imageVector = Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
            Text(text = stringResource(Res.string.group_view_only), style = MaterialTheme.typography.labelMedium)
        }
    }
}
