package com.vndblite.dcj.ui.screens.others

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.core.AppInfo
import com.vndblite.dcj.domain.model.AppSettings
import com.vndblite.dcj.domain.model.ThemeId
import com.vndblite.dcj.domain.model.UserAccount
import com.vndblite.dcj.ui.format.displayNameResId
import com.vndblite.dcj.ui.format.openExternalUrl
import com.vndblite.dcj.ui.theme.VndbTheme
import com.vndblite.dcj.ui.theme.VndbThemeTokens

@Immutable
data class OthersUiState(
    val activeAccount: UserAccount? = null,
    val isAccountSynced: Boolean = false,
    val settings: AppSettings = AppSettings()
)

sealed interface OthersAction {
    data object OpenAuthSync : OthersAction
    data object OpenRandomVn : OthersAction
    data object OpenSettings : OthersAction
    data object OpenAbout : OthersAction
}

@Composable
fun OthersScreen(
    state: OthersUiState,
    onAction: (OthersAction) -> Unit
) {
    val context = LocalContext.current
    OthersContent(
        state = state,
        onAction = onAction,
        onOpenIssuesInBrowser = {
            openExternalUrl(context, AppInfo.GITHUB_ISSUES_URL)
        }
    )
}

@Composable
fun OthersContent(
    state: OthersUiState,
    onAction: (OthersAction) -> Unit,
    onOpenIssuesInBrowser: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spec = VndbThemeTokens.spec
    val account = state.activeAccount
    val themeName = stringResource(state.settings.themeId.displayNameResId())

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = stringResource(R.string.others_title),
            style = MaterialTheme.typography.displayMedium,
            color = spec.accentColor,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.others_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = spec.onSurfaceColor.copy(alpha = 0.8f)
        )

        OthersActionCard(
            icon = Icons.Default.CloudSync,
            title = if (account != null) {
                stringResource(R.string.others_auth_title_signed_in, account.username)
            } else {
                stringResource(R.string.others_auth_title_signed_out)
            },
            subtitle = when {
                account != null && state.isAccountSynced -> stringResource(R.string.others_auth_subtitle_synced)
                account != null -> stringResource(R.string.others_auth_subtitle_pending)
                else -> stringResource(R.string.others_auth_subtitle_signed_out)
            },
            onClick = { onAction(OthersAction.OpenAuthSync) },
            testTag = "others_card_auth"
        )

        OthersActionCard(
            icon = Icons.Default.Casino,
            title = stringResource(R.string.others_random_title),
            subtitle = stringResource(R.string.others_random_subtitle),
            onClick = { onAction(OthersAction.OpenRandomVn) },
            testTag = "others_card_random"
        )

        OthersActionCard(
            icon = Icons.Default.Settings,
            title = stringResource(R.string.others_settings_title),
            subtitle = stringResource(R.string.others_settings_subtitle, themeName),
            onClick = { onAction(OthersAction.OpenSettings) },
            testTag = "others_card_settings"
        )

        OthersActionCard(
            icon = Icons.AutoMirrored.Filled.HelpOutline,
            title = stringResource(R.string.about_title),
            subtitle = stringResource(R.string.about_subtitle),
            onClick = { onAction(OthersAction.OpenAbout) },
            testTag = "others_card_about"
        )

        OthersActionCard(
            icon = Icons.Default.BugReport,
            title = stringResource(R.string.others_report_issue_title),
            subtitle = stringResource(R.string.others_report_issue_subtitle),
            onClick = onOpenIssuesInBrowser,
            testTag = "others_card_issues"
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun OthersActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    val spec = VndbThemeTokens.spec
    Surface(
        color = spec.surfaceColor.copy(alpha = 0.85f),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Surface(
                color = spec.accentColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(46.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = spec.accentColor,
                    modifier = Modifier.padding(11.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = spec.accentColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = spec.onSurfaceColor.copy(alpha = 0.82f)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = spec.onSurfaceColor.copy(alpha = 0.6f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OthersContentPreview() {
    VndbTheme(themeId = ThemeId.AGL, includeBackground = true) {
        OthersContent(
            state = OthersUiState(),
            onAction = {},
            onOpenIssuesInBrowser = {}
        )
    }
}
