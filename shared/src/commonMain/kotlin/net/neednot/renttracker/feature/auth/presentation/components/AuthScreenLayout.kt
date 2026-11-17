package net.neednot.renttracker.feature.auth.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.designsystem.components.ContentLayout
import net.neednot.renttracker.core.designsystem.components.OfflineBanner

private val MaxBodyWidth = 480.dp

@Composable
internal fun AuthScreenLayout(
    modifier: Modifier = Modifier,
    offlineBanner: String? = null,
    topBar: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(modifier = modifier, topBar = topBar) { contentPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .consumeWindowInsets(contentPadding)
                    .imePadding(),
        ) {
            offlineBanner?.let { OfflineBanner(text = it) }
            BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(
                    modifier =
                        Modifier
                            .widthIn(max = MaxBodyWidth)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .heightIn(min = maxHeight)
                            .padding(ContentLayout.Auth.padding),
                    verticalArrangement = Arrangement.spacedBy(ContentLayout.Auth.spacing),
                    content = content,
                )
            }
        }
    }
}
