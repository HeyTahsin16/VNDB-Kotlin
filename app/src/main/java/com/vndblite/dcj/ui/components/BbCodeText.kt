package com.vndblite.dcj.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.core.AppLog
import com.vndblite.dcj.domain.model.ThemeId
import com.vndblite.dcj.ui.theme.LocalThemeSpec
import com.vndblite.dcj.ui.theme.VndbTheme

@Suppress("DEPRECATION")
@Composable
fun BbCodeText(
    rawText: String,
    modifier: Modifier = Modifier,
    textColor: Color = LocalThemeSpec.current.contentColor,
    linkColor: Color = LocalThemeSpec.current.accentColor,
    style: TextStyle = MaterialTheme.typography.bodyMedium
) {
    val context = LocalContext.current
    val parsedNodes = remember(rawText) { BbCodeParser.parse(rawText) }
    val hasSpoilers = remember(parsedNodes) { BbCodeParser.hasSpoilers(parsedNodes) }
    var revealSpoilers by rememberSaveable(rawText) { mutableStateOf(false) }

    val spoilerPlaceholder = stringResource(R.string.spoiler_hidden_inline)
    val notAvailableLabel = stringResource(R.string.label_not_available)

    val annotated = remember(parsedNodes, revealSpoilers, spoilerPlaceholder, textColor, linkColor, notAvailableLabel) {
        if (rawText.isBlank()) {
            AnnotatedString(notAvailableLabel)
        } else {
            BbCodeParser.toAnnotatedString(
                nodes = parsedNodes,
                revealSpoilers = revealSpoilers,
                spoilerPlaceholder = spoilerPlaceholder,
                baseColor = textColor,
                linkColor = linkColor
            )
        }
    }

    Column(modifier = modifier) {
        if (hasSpoilers) {
            Surface(
                color = linkColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .semantics { role = Role.Button }
                    .clickable { revealSpoilers = !revealSpoilers }
            ) {
                Text(
                    text = if (revealSpoilers) {
                        stringResource(R.string.spoiler_hide_action)
                    } else {
                        stringResource(R.string.spoiler_reveal_action)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = linkColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        ClickableText(
            text = annotated,
            style = style.copy(color = textColor),
            onClick = { offset ->
                annotated.getStringAnnotations(
                    tag = BbCodeParser.URL_ANNOTATION_TAG,
                    start = offset,
                    end = offset
                ).firstOrNull()?.let { annotation ->
                    val url = annotation.item
                    try {
                        val fullUrl = when {
                            url.startsWith("/") -> "https://vndb.org$url"
                            !url.startsWith("http://") && !url.startsWith("https://") -> "https://$url"
                            else -> url
                        }
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl)))
                    } catch (e: Exception) {
                        AppLog.w("BbCodeText", "Failed to open URL annotation: $url", e)
                    }
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BbCodeTextPreview() {
    VndbTheme(themeId = ThemeId.AGL) {
        BbCodeText(
            rawText = "A [b]bold [i]and italic[/i][/b] synopsis with [sic] note and [spoiler]secret ending[/spoiler].",
            modifier = Modifier.padding(12.dp)
        )
    }
}
