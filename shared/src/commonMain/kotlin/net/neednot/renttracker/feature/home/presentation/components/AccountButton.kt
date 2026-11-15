package net.neednot.renttracker.feature.home.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.designsystem.components.InitialsAvatar
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.home_account
import org.jetbrains.compose.resources.stringResource

private val AVATAR_SIZE = 32.dp

@Composable
internal fun AccountButton(
    initial: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(Res.string.home_account)
    IconButton(onClick = onClick, modifier = modifier.size(48.dp).semantics { contentDescription = description }) {
        if (initial == null) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            InitialsAvatar(initials = initial, size = AVATAR_SIZE)
        }
    }
}
