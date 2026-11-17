package net.neednot.renttracker.core.designsystem.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.neednot.renttracker.core.designsystem.theme.Spacing
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.common_close_without_saving
import net.neednot.renttracker.resources.common_save
import net.neednot.renttracker.resources.common_saving
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun EditorTopBar(
    title: String,
    isSaveEnabled: Boolean,
    onSaveClicked: () -> Unit,
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier,
    isSaving: Boolean = false,
    saveLabel: String = stringResource(Res.string.common_save),
    savingLabel: String = stringResource(Res.string.common_saving),
) {
    RtTopAppBar(
        title = title,
        modifier = modifier,
        navigation = TopBarNavigation.Close,
        navigationDescription = stringResource(Res.string.common_close_without_saving),
        onNavigationClicked = onCloseClicked,
        actions = {
            RtButton(
                text = if (isSaving) savingLabel else saveLabel,
                onClick = onSaveClicked,
                modifier = Modifier.padding(end = Spacing.sm),
                size = ButtonSize.Small,
                enabled = isSaveEnabled || isSaving,
                isWorking = isSaving,
            )
        },
    )
}
