package com.vndblite.dcj.ui.screens.others

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.core.AppInfo
import com.vndblite.dcj.core.AppLog
import com.vndblite.dcj.domain.model.UserAccount
import com.vndblite.dcj.ui.components.AssetImage
import com.vndblite.dcj.ui.components.AssetPaths
import com.vndblite.dcj.ui.theme.LocalThemeSpec

@Composable
fun OthersScreen(
    activeAccount: UserAccount?,
    onOpenAuthSync: () -> Unit,
    onOpenRandomVn: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeSpec = LocalThemeSpec.current
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("others_screen_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                color = themeSpec.surfaceColor.copy(alpha = 0.85f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssetImage(
                        assetPath = AssetPaths.APP_ICON,
                        contentDescription = stringResource(R.string.app_name),
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = themeSpec.accentColor
                        )
                        Text(
                            text = stringResource(R.string.others_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = themeSpec.contentColor.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        item {
            val authSubtitle = if (activeAccount != null) {
                stringResource(R.string.auth_connected_as, activeAccount.username, activeAccount.id)
            } else {
                stringResource(R.string.others_auth_desc)
            }
            OthersNavTile(
                icon = Icons.Default.CloudSync,
                title = stringResource(R.string.user_authentication),
                subtitle = authSubtitle,
                testTag = "others_auth_sync_tile",
                onClick = onOpenAuthSync
            )
        }

        item {
            OthersNavTile(
                icon = Icons.Default.Casino,
                title = stringResource(R.string.random_vn_title),
                subtitle = stringResource(R.string.others_random_desc),
                testTag = "others_random_vn_tile",
                onClick = onOpenRandomVn
            )
        }

        item {
            OthersNavTile(
                icon = Icons.Default.Palette,
                title = stringResource(R.string.settings_title),
                subtitle = stringResource(R.string.others_settings_desc),
                testTag = "others_settings_tile",
                onClick = onOpenSettings
            )
        }

        item {
            OthersNavTile(
                icon = Icons.Default.Info,
                title = stringResource(R.string.about_title),
                subtitle = stringResource(R.string.others_about_desc),
                testTag = "others_about_tile",
                onClick = onOpenAbout
            )
        }

        item {
            OthersNavTile(
                icon = Icons.Default.BugReport,
                title = stringResource(R.string.others_bug_report_title),
                subtitle = stringResource(R.string.others_bug_report_desc),
                testTag = "others_bug_report_tile",
                onClick = {
                    val url = "${AppInfo.SOURCE_REPOSITORY_URL}/issues"
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (e: Exception) {
                        AppLog.w("OthersScreen", "Failed to open GitHub issues URL", e)
                    }
                }
            )
        }
    }
}

@Composable
private fun OthersNavTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    Surface(
        color = themeSpec.surfaceColor.copy(alpha = 0.85f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { role = Role.Button }
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = themeSpec.accentColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = themeSpec.contentColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = themeSpec.contentColor.copy(alpha = 0.8f)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = themeSpec.accentColor
            )
        }
    }
}
