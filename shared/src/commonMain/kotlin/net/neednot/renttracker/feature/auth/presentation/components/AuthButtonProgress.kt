package net.neednot.renttracker.feature.auth.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.auth_working
import org.jetbrains.compose.resources.stringResource

@Composable
fun AuthButtonProgress(modifier: Modifier = Modifier) {
    val description = stringResource(Res.string.auth_working)
    CircularProgressIndicator(
        modifier = modifier.padding(end = 8.dp).size(18.dp).semantics { contentDescription = description },
        color = LocalContentColor.current,
        strokeWidth = 2.dp,
    )
}
