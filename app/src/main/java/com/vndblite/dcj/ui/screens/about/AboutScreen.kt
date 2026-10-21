package com.vndblite.dcj.ui.screens.about

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.core.AppInfo
import com.vndblite.dcj.domain.model.ThemeId
import com.vndblite.dcj.ui.components.AssetImage
import com.vndblite.dcj.ui.components.AssetPaths
import com.vndblite.dcj.ui.format.openExternalUrl
import com.vndblite.dcj.ui.theme.VndbTheme
import com.vndblite.dcj.ui.theme.VndbThemeTokens

@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    AboutContent(
        onBack = onBack,
        onOpenUrl = { url -> openExternalUrl(context, url) }
    )
}

@Composable
fun AboutContent(
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val spec = VndbThemeTokens.spec

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.nav_back_cd),
                    tint = spec.accentColor
                )
            }
            Text(
                text = stringResource(R.string.about_title),
                style = MaterialTheme.typography.displayMedium,
                color = spec.accentColor,
                fontWeight = FontWeight.Bold
            )
        }

        AboutHeroCard()
        AboutFeaturesCard()
        AboutCreditsCard(onOpenUrl = onOpenUrl)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AboutHeroCard() {
    val spec = VndbThemeTokens.spec
    Surface(
        color = spec.surfaceColor.copy(alpha = 0.88f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(20.dp)
        ) {
            AssetImage(
                assetPath = AssetPaths.APP_ICON_TRANSPARENT,
                contentDescription = null,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = AppInfo.APP_NAME,
                style = MaterialTheme.typography.headlineMedium,
                color = spec.accentColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.version_format, AppInfo.VERSION_NAME) +
                    " (${AppInfo.VERSION_CODE}) • " +
                    stringResource(R.string.about_license_label, AppInfo.APP_LICENSE),
                style = MaterialTheme.typography.labelLarge,
                color = spec.onSurfaceColor.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.about_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = spec.onSurfaceColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AboutFeaturesCard() {
    val spec = VndbThemeTokens.spec
    Surface(
        color = spec.surfaceColor.copy(alpha = 0.85f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.about_features_heading),
                style = MaterialTheme.typography.titleLarge,
                color = spec.accentColor,
                fontWeight = FontWeight.Bold
            )
            Text(stringResource(R.string.about_feature_1), style = MaterialTheme.typography.bodyMedium, color = spec.onSurfaceColor)
            Text(stringResource(R.string.about_feature_2), style = MaterialTheme.typography.bodyMedium, color = spec.onSurfaceColor)
            Text(stringResource(R.string.about_feature_3), style = MaterialTheme.typography.bodyMedium, color = spec.onSurfaceColor)
            Text(stringResource(R.string.about_feature_4), style = MaterialTheme.typography.bodyMedium, color = spec.onSurfaceColor)
            Text(stringResource(R.string.about_feature_5), style = MaterialTheme.typography.bodyMedium, color = spec.onSurfaceColor)
        }
    }
}

@Composable
private fun AboutCreditsCard(
    onOpenUrl: (String) -> Unit
) {
    val spec = VndbThemeTokens.spec
    val semantic = VndbThemeTokens.colors
    Surface(
        color = spec.surfaceColor.copy(alpha = 0.85f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.about_credits_heading),
                style = MaterialTheme.typography.titleLarge,
                color = spec.accentColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.about_credits_vndb_lite),
                style = MaterialTheme.typography.bodyMedium,
                color = spec.onSurfaceColor
            )
            Text(
                text = stringResource(R.string.about_credits_vndb_org),
                style = MaterialTheme.typography.bodyMedium,
                color = spec.onSurfaceColor
            )
            Button(
                onClick = { onOpenUrl(AppInfo.GITHUB_REPO_URL) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = spec.accentColor,
                    contentColor = semantic.onAccentButton
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                AssetImage(
                    assetPath = AssetPaths.GITHUB_CONTACT_ICON,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.about_open_github), fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { onOpenUrl(AppInfo.VNDB_DATA_LICENSE_URL) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null, tint = spec.accentColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.about_open_vndb_license), color = spec.accentColor)
            }
            OutlinedButton(
                onClick = { onOpenUrl(AppInfo.UPSTREAM_FLUTTER_REPO_URL) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null, tint = spec.accentColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text(AppInfo.UPSTREAM_FLUTTER_REPO_SLUG, color = spec.accentColor)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AboutContentPreview() {
    VndbTheme(themeId = ThemeId.AGL, includeBackground = true) {
        AboutContent(onBack = {}, onOpenUrl = {})
    }
}
