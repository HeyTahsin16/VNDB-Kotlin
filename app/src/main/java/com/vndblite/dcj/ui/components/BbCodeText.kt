package com.vndblite.dcj.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.ui.theme.LocalAppThemeCode

@Composable
fun BbCodeText(
    rawText: String,
    modifier: Modifier = Modifier,
    textColor: Color = LocalAppThemeCode.current.tertiary,
    linkColor: Color = LocalAppThemeCode.current.secondary,
    style: TextStyle = MaterialTheme.typography.bodyMedium
) {
    val context = LocalContext.current
    val hasSpoilers = remember(rawText) { rawText.contains("[spoiler]", ignoreCase = true) }
    var revealSpoilers by remember(rawText) { mutableStateOf(false) }

    val annotated = remember(rawText, revealSpoilers, textColor, linkColor) {
        parseBbCodeToAnnotatedString(
            input = rawText,
            revealSpoilers = revealSpoilers,
            baseColor = textColor,
            linkColor = linkColor
        )
    }

    Column(modifier = modifier) {
        if (hasSpoilers) {
            Surface(
                color = linkColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .clickable { revealSpoilers = !revealSpoilers }
            ) {
                Text(
                    text = if (revealSpoilers) "Hide Spoilers" else "Tap to Reveal Spoilers",
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
                annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        val url = annotation.item
                        runCatching {
                            val fullUrl = if (url.startsWith("/")) {
                                "https://vndb.org$url"
                            } else if (!url.startsWith("http")) {
                                "https://$url"
                            } else {
                                url
                            }
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl)))
                        }
                    }
            }
        )
    }
}

private fun parseBbCodeToAnnotatedString(
    input: String,
    revealSpoilers: Boolean,
    baseColor: Color,
    linkColor: Color
): AnnotatedString {
    if (input.isBlank()) return AnnotatedString("--")

    var working = input
        .replace("\r\n", "\n")
        .replace(Regex("\\[raw\\]([\\s\\S]*?)\\[/raw\\]", RegexOption.IGNORE_CASE), "$1")
        .replace(Regex("\\[quote\\]([\\s\\S]*?)\\[/quote\\]", RegexOption.IGNORE_CASE), "\n“$1”\n")

    if (!revealSpoilers) {
        working = working.replace(
            Regex("\\[spoiler\\]([\\s\\S]*?)\\[/spoiler\\]", RegexOption.IGNORE_CASE),
            " [Spoiler Hidden] "
        )
    } else {
        working = working.replace(
            Regex("\\[/?spoiler\\]", RegexOption.IGNORE_CASE),
            ""
        )
    }

    return buildAnnotatedString {
        var idx = 0
        val tokenRegex = Regex(
            "\\[(b|i|u|s|code)\\]([\\s\\S]*?)\\[/\\1\\]|\\[url=([^\\]]+)\\]([\\s\\S]*?)\\[/url\\]|\\[url\\]([\\s\\S]*?)\\[/url\\]",
            RegexOption.IGNORE_CASE
        )

        val matches = tokenRegex.findAll(working).toList()
        if (matches.isEmpty()) {
            val cleaned = working.replace(Regex("\\[/?[a-zA-Z0-9=]+\\]"), "")
            append(cleaned)
            return@buildAnnotatedString
        }

        for (match in matches) {
            if (match.range.first > idx) {
                val plain = working.substring(idx, match.range.first)
                    .replace(Regex("\\[/?[a-zA-Z0-9=]+\\]"), "")
                append(plain)
            }

            val tagType = match.groups[1]?.value?.lowercase()
            val tagContent = match.groups[2]?.value
            val urlTarget = match.groups[3]?.value
            val urlLabel = match.groups[4]?.value
            val bareUrl = match.groups[5]?.value

            when {
                tagType != null && tagContent != null -> {
                    val cleanInner = tagContent.replace(Regex("\\[/?[a-zA-Z0-9=]+\\]"), "")
                    val span = when (tagType) {
                        "b" -> SpanStyle(fontWeight = FontWeight.Bold)
                        "i" -> SpanStyle(fontStyle = FontStyle.Italic)
                        "u" -> SpanStyle(textDecoration = TextDecoration.Underline)
                        "s" -> SpanStyle(textDecoration = TextDecoration.LineThrough)
                        "code" -> SpanStyle(fontFamily = FontFamily.Monospace, background = baseColor.copy(alpha = 0.15f))
                        else -> SpanStyle()
                    }
                    pushStyle(span)
                    append(cleanInner)
                    pop()
                }
                urlTarget != null && urlLabel != null -> {
                    val cleanLabel = urlLabel.replace(Regex("\\[/?[a-zA-Z0-9=]+\\]"), "")
                    pushStringAnnotation(tag = "URL", annotation = urlTarget.trim())
                    pushStyle(
                        SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    append(cleanLabel)
                    pop()
                    pop()
                }
                bareUrl != null -> {
                    pushStringAnnotation(tag = "URL", annotation = bareUrl.trim())
                    pushStyle(
                        SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    append(bareUrl.trim())
                    pop()
                    pop()
                }
            }
            idx = match.range.last + 1
        }

        if (idx < working.length) {
            val tail = working.substring(idx).replace(Regex("\\[/?[a-zA-Z0-9=]+\\]"), "")
            append(tail)
        }
    }
}
