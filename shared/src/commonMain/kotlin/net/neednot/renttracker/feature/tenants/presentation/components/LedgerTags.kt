package net.neednot.renttracker.feature.tenants.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.neednot.renttracker.core.designsystem.components.Tag
import net.neednot.renttracker.core.designsystem.components.TagKind
import net.neednot.renttracker.feature.tenants.presentation.uistate.EntryChip
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingChip
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ic_smartphone
import net.neednot.renttracker.resources.ledger_chip_bounced
import net.neednot.renttracker.resources.ledger_chip_cancelled
import net.neednot.renttracker.resources.ledger_chip_edited
import net.neednot.renttracker.resources.ledger_tag_may_not_apply
import net.neednot.renttracker.resources.pending_chip_needs_confirmation
import net.neednot.renttracker.resources.pending_chip_pending
import net.neednot.renttracker.resources.pending_chip_rejected
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun EntryTag(
    chip: EntryChip,
    modifier: Modifier = Modifier,
) {
    when (chip) {
        EntryChip.Edited -> Tag(stringResource(Res.string.ledger_chip_edited), TagKind.Neutral, modifier)
        EntryChip.Bounced -> Tag(stringResource(Res.string.ledger_chip_bounced), TagKind.Error, modifier)
        EntryChip.Cancelled -> Tag(stringResource(Res.string.ledger_chip_cancelled), TagKind.Outlined, modifier)
    }
}

@Composable
internal fun PendingTag(
    chip: PendingChip,
    modifier: Modifier = Modifier,
) {
    when (chip) {
        PendingChip.Pending -> {
            Tag(
                text = stringResource(Res.string.pending_chip_pending),
                kind = TagKind.Neutral,
                modifier = modifier,
                icon = Res.drawable.ic_smartphone,
            )
        }

        PendingChip.NeedsConfirmation -> {
            Tag(stringResource(Res.string.pending_chip_needs_confirmation), TagKind.Warning, modifier)
        }

        PendingChip.Rejected -> {
            Tag(stringResource(Res.string.pending_chip_rejected), TagKind.Error, modifier)
        }
    }
}

@Composable
internal fun MayNotApplyTag(modifier: Modifier = Modifier) {
    Tag(text = stringResource(Res.string.ledger_tag_may_not_apply), kind = TagKind.Warning, modifier = modifier)
}
