package net.neednot.renttracker.feature.account.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import net.neednot.renttracker.core.designsystem.components.ContentLayout
import net.neednot.renttracker.core.designsystem.components.RtListCard
import net.neednot.renttracker.core.designsystem.components.RtListItem
import net.neednot.renttracker.core.designsystem.components.SectionHeader
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.account_app_lock
import net.neednot.renttracker.resources.account_app_lock_hint
import net.neednot.renttracker.resources.account_notifications_all_groups
import net.neednot.renttracker.resources.account_section_notifications
import net.neednot.renttracker.resources.account_section_security
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun UnavailableSettings(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(ContentLayout.List.spacing)) {
        SectionHeader(text = stringResource(Res.string.account_section_security))
        RtListCard(items = listOf(Unit)) {
            DisabledSwitchItem(
                text = stringResource(Res.string.account_app_lock),
                supportingText = stringResource(Res.string.account_app_lock_hint),
            )
        }
        SectionHeader(text = stringResource(Res.string.account_section_notifications))
        RtListCard(items = listOf(Unit)) {
            DisabledSwitchItem(text = stringResource(Res.string.account_notifications_all_groups))
        }
    }
}

@Composable
private fun DisabledSwitchItem(
    text: String,
    supportingText: String? = null,
) {
    RtListItem(
        text = text,
        modifier = Modifier.toggleable(value = false, enabled = false, role = Role.Switch, onValueChange = {}),
        supportingText = supportingText,
        trailing = { Switch(checked = false, onCheckedChange = null, enabled = false) },
    )
}
