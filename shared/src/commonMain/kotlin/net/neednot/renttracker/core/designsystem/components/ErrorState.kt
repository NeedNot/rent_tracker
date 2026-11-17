package net.neednot.renttracker.core.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.common_try_again
import net.neednot.renttracker.resources.ic_error
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ErrorState(
    title: String,
    onRetryClicked: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
) {
    EmptyState(
        icon = Res.drawable.ic_error,
        title = title,
        modifier = modifier,
        text = text,
        action = {
            RtButton(
                text = stringResource(Res.string.common_try_again),
                onClick = onRetryClicked,
                variant = ButtonVariant.Outlined,
            )
        },
    )
}
