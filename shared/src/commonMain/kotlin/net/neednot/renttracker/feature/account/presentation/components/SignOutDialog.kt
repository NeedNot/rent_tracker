package net.neednot.renttracker.feature.account.presentation.components

import androidx.compose.runtime.Composable
import net.neednot.renttracker.core.designsystem.components.ConfirmDialog
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.account_sign_out
import net.neednot.renttracker.resources.account_sign_out_message
import net.neednot.renttracker.resources.account_sign_out_pending_message
import net.neednot.renttracker.resources.account_sign_out_pending_stay
import net.neednot.renttracker.resources.account_sign_out_pending_title
import net.neednot.renttracker.resources.common_cancel
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SignOutDialog(
    unsyncedCount: Int,
    onConfirmed: () -> Unit,
    onDismissed: () -> Unit,
) {
    ConfirmDialog(
        title = stringResource(Res.string.account_sign_out_pending_title),
        text =
            if (unsyncedCount == 0) {
                stringResource(Res.string.account_sign_out_message)
            } else {
                // The design's copy; sign-out still discards the queue (see docs/unimplemented-design-features.md).
                pluralStringResource(Res.plurals.account_sign_out_pending_message, unsyncedCount, unsyncedCount)
            },
        confirmLabel = stringResource(Res.string.account_sign_out),
        onConfirmed = onConfirmed,
        onDismissed = onDismissed,
        dismissLabel =
            stringResource(
                if (unsyncedCount == 0) Res.string.common_cancel else Res.string.account_sign_out_pending_stay,
            ),
    )
}
