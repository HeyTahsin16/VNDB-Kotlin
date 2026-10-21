package com.vndblite.dcj.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.core.UiText
import com.vndblite.dcj.domain.model.ThemeId
import com.vndblite.dcj.ui.theme.VndbTheme
import com.vndblite.dcj.ui.theme.VndbThemeTokens

/**
 * Sealed per-section UI state distinguishing `Loading`, `Content`, `Empty`, and `Error`
 * so network or parsing failures never masquerade as an empty list (F5, Rule 5).
 */
@Immutable
sealed interface SectionUiState<out T> {
    data object Loading : SectionUiState<Nothing>
    data class Content<out T>(val data: T) : SectionUiState<T>
    data object Empty : SectionUiState<Nothing>
    data class Error(val message: UiText) : SectionUiState<Nothing>
}

typealias SectionLoadState<T> = SectionUiState<T>

@Composable
fun <T> SectionStateContainer(
    state: SectionUiState<T>,
    emptyMessage: UiText,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    Box(modifier = modifier) {
        when (state) {
            SectionUiState.Loading -> LoadingStateView()
            SectionUiState.Empty -> EmptyStateView(title = emptyMessage.asString())
            is SectionUiState.Error -> InlineSectionErrorCard(
                message = state.message,
                onRetry = onRetry
            )
            is SectionUiState.Content -> content(state.data)
        }
    }
}

@Composable
fun LoadingStateView(
    modifier: Modifier = Modifier,
    testTag: String = "loading_state_view"
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun EmptyStateView(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector = Icons.Default.Inbox,
    testTag: String = "empty_state_view"
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 28.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ErrorStateView(
    message: UiText,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryTestTag: String = "error_retry_button"
) {
    val semanticColors = VndbThemeTokens.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = null,
            tint = semanticColors.danger,
            modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message.asString(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = semanticColors.onAccentButton
            ),
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag(retryTestTag)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.dialog_retry),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Compact inline section error card with a retry button for individual carousels or VN detail sections (F5).
 */
@Composable
fun InlineSectionErrorCard(
    message: UiText,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryTestTag: String = "inline_section_retry_button"
) {
    val semanticColors = VndbThemeTokens.colors
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = semanticColors.danger,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = message.asString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag(retryTestTag)
            ) {
                Text(stringResource(R.string.dialog_retry))
            }
        }
    }
}

@Preview(name = "ErrorStateView - Dark Theme", showBackground = true)
@Composable
private fun ErrorStateViewPreview() {
    VndbTheme(themeId = ThemeId.AGL, includeBackground = true) {
        ErrorStateView(
            message = UiText.StringResource(R.string.connection_error),
            onRetry = {}
        )
    }
}

@Preview(name = "InlineSectionErrorCard - Light Theme", showBackground = true)
@Composable
private fun InlineSectionErrorCardPreview() {
    VndbTheme(themeId = ThemeId.SEINRKN, includeBackground = true) {
        InlineSectionErrorCard(
            message = UiText.StringResource(R.string.detail_reviews_unavailable),
            onRetry = {},
            modifier = Modifier.padding(12.dp)
        )
    }
}
