package com.vndblite.dcj.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.vndblite.dcj.core.ApiError
import com.vndblite.dcj.domain.model.ThemeId
import com.vndblite.dcj.ui.format.toUiText
import com.vndblite.dcj.ui.theme.LocalThemeSpec
import com.vndblite.dcj.ui.theme.VndbTheme

@Composable
fun LoadingStateView(
    modifier: Modifier = Modifier,
    message: String = stringResource(R.string.label_loading)
) {
    val themeSpec = LocalThemeSpec.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .testTag("loading_state_view"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = themeSpec.accentColor)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = themeSpec.contentColor.copy(alpha = 0.85f)
        )
    }
}

@Composable
fun EmptyStateView(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.SearchOff
) {
    val themeSpec = LocalThemeSpec.current
    Surface(
        color = themeSpec.surfaceColor.copy(alpha = 0.78f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("empty_state_view")
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = themeSpec.accentColor,
                modifier = Modifier.size(38.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = themeSpec.contentColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ErrorStateView(
    error: ApiError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryTestTag: String = "error_retry_button"
) {
    ErrorMessageView(
        message = error.toUiText().asString(),
        onRetry = onRetry,
        modifier = modifier,
        retryTestTag = retryTestTag
    )
}

@Composable
fun ErrorMessageView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryTestTag: String = "error_retry_button"
) {
    val themeSpec = LocalThemeSpec.current
    Surface(
        color = themeSpec.surfaceColor.copy(alpha = 0.85f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("error_state_view")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                tint = themeSpec.accentColor,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = themeSpec.contentColor,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = themeSpec.accentColor,
                    contentColor = themeSpec.onAccentColor
                ),
                modifier = Modifier.testTag(retryTestTag)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = stringResource(R.string.action_retry),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorStateViewPreview() {
    VndbTheme(themeId = ThemeId.AGL) {
        ErrorStateView(error = ApiError.Offline, onRetry = {})
    }
}
