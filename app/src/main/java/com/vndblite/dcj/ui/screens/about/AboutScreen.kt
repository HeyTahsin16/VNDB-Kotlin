package com.vndblite.dcj.ui.screens.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.core.AppInfo
import com.vndblite.dcj.core.AppLog
import com.vndblite.dcj.ui.components.AssetImage
import com.vndblite.dcj.ui.components.AssetPaths
import com.vndblite.dcj.ui.theme.LocalThemeSpec

@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeSpec = LocalThemeSpec.current
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("about_screen_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("about_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = themeSpec.accentColor
                    )
                }
                Text(
                    text = stringResource(R.string.about_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = themeSpec.accentColor
                )
            }
        }

        item {
            Surface(
                color = themeSpec.surfaceColor.copy(alpha = 0.85f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AssetImage(
                        assetPath = AssetPaths.APP_ICON,
                        contentDescription = stringResource(R.string.app_name),
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = themeSpec.accentColor
                    )
                    Text(
                        text = stringResource(
                            R.string.about_version_format,
                            AppInfo.versionName,
                            AppInfo.versionCode
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = themeSpec.contentColor.copy(alpha = 0.85f),
                        modifier = Modifier.testTag("about_version_text")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.app_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = themeSpec.contentColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(AppInfo.SOURCE_REPOSITORY_URL))
                                )
                            } catch (e: Exception) {
                                AppLog.w("AboutScreen", "Failed to open GitHub repository URL", e)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeSpec.accentColor,
                            contentColor = themeSpec.onAccentColor
                        ),
                        modifier = Modifier.testTag("about_github_button")
                    ) {
                        AssetImage(
                            assetPath = AssetPaths.GITHUB_ICON,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.about_github_button),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Surface(
                color = themeSpec.surfaceColor.copy(alpha = 0.85f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.about_credits_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = themeSpec.accentColor
                    )
                    Text(
                        text = stringResource(R.string.about_credits_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = themeSpec.contentColor
                    )
                }
            }
        }

        item {
            Surface(
                color = themeSpec.surfaceColor.copy(alpha = 0.85f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.about_license_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = themeSpec.accentColor
                    )
                    Text(
                        text = stringResource(R.string.about_license_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = themeSpec.contentColor
                    )
                }
            }
        }
    }
}
