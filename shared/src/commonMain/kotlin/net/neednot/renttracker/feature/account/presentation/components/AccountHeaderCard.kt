package net.neednot.renttracker.feature.account.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.neednot.renttracker.core.designsystem.components.InitialsAvatar
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.account_edit_name
import org.jetbrains.compose.resources.stringResource

private val AVATAR_SIZE = 48.dp
private const val NAME_SIZE = 16
private const val EMAIL_SIZE = 13

@Composable
internal fun AccountHeaderCard(
    initials: String,
    displayName: String,
    email: String,
    onEditNameClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AccountCard(modifier = modifier) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(
                        min = 72.dp,
                    ).padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InitialsAvatar(initials = initials, size = AVATAR_SIZE)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = displayName, fontSize = NAME_SIZE.sp, fontWeight = FontWeight.Medium)
                Text(
                    text = email,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = EMAIL_SIZE.sp,
                )
            }
            TextButton(onClick = onEditNameClicked) { Text(stringResource(Res.string.account_edit_name)) }
        }
    }
}
