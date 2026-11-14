package net.neednot.renttracker.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject

private val EdgeMargin = 8.dp
private val BottomMargin = 16.dp
private val SnackbarCorner = 8.dp

@Composable
fun AppSnackbarHost(content: @Composable () -> Unit) {
    val messageDispatcher = koinInject<MessageDispatcher>()
    val hostState = remember { SnackbarHostState() }
    val clearance = remember { SnackbarClearance() }

    LaunchedEffect(messageDispatcher) {
        messageDispatcher.messages.collect { message ->
            hostState.showSnackbar(MessageVisuals(message.text.resolve(), message.variant))
        }
    }

    CompositionLocalProvider(LocalSnackbarClearance provides clearance) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
            SnackbarHost(
                hostState = hostState,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(
                            start = EdgeMargin,
                            end = EdgeMargin,
                            bottom = if (clearance.height > 0.dp) clearance.height + EdgeMargin else BottomMargin,
                        ),
            ) { data ->
                val isError = (data.visuals as? MessageVisuals)?.variant == UiMessageVariant.Error
                val colors = MaterialTheme.colorScheme
                Snackbar(
                    shape = RoundedCornerShape(SnackbarCorner),
                    containerColor = if (isError) colors.errorContainer else colors.inverseSurface,
                    contentColor = if (isError) colors.onErrorContainer else colors.inverseOnSurface,
                ) {
                    Text(text = data.visuals.message, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

private class MessageVisuals(
    override val message: String,
    val variant: UiMessageVariant,
) : SnackbarVisuals {
    override val actionLabel: String? = null
    override val withDismissAction: Boolean = false
    override val duration: SnackbarDuration = SnackbarDuration.Short
}
