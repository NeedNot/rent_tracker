package net.neednot.renttracker.core.designsystem.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.common_close_without_saving
import net.neednot.renttracker.resources.common_save
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorTopBar(
    title: String,
    isSaveEnabled: Boolean,
    onSaveClicked: () -> Unit,
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = { Text(title) },
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onCloseClicked) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(Res.string.common_close_without_saving),
                )
            }
        },
        actions = {
            Button(onClick = onSaveClicked, modifier = Modifier.padding(end = 8.dp), enabled = isSaveEnabled) {
                Text(stringResource(Res.string.common_save))
            }
        },
    )
}
