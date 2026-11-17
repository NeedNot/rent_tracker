package net.neednot.renttracker.core.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.common_back
import net.neednot.renttracker.resources.common_close
import net.neednot.renttracker.resources.ic_arrow_back
import net.neednot.renttracker.resources.ic_close
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RtTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigation: TopBarNavigation = TopBarNavigation.Back,
    navigationDescription: String? = null,
    onNavigationClicked: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    TopAppBar(
        title = {
            Column {
                Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                subtitle?.let {
                    Text(
                        text = it,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = scheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        modifier = modifier,
        navigationIcon = { NavigationButton(navigation, navigationDescription, onNavigationClicked) },
        actions = actions,
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = scheme.surface,
                scrolledContainerColor = scheme.surface,
            ),
    )
}

@Composable
private fun NavigationButton(
    navigation: TopBarNavigation,
    description: String?,
    onClick: () -> Unit,
) {
    val icon =
        when (navigation) {
            TopBarNavigation.None -> return
            TopBarNavigation.Back -> Res.drawable.ic_arrow_back
            TopBarNavigation.Close -> Res.drawable.ic_close
        }
    val defaultDescription =
        stringResource(if (navigation == TopBarNavigation.Back) Res.string.common_back else Res.string.common_close)
    IconButton(onClick = onClick) {
        AppIcon(icon = icon, contentDescription = description ?: defaultDescription)
    }
}
