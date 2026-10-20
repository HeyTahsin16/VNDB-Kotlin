package com.vndblite.dcj.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.ThemeId
import com.vndblite.dcj.ui.format.ExternalUriOpener
import com.vndblite.dcj.ui.format.openExternalUrl
import com.vndblite.dcj.ui.theme.VndbTheme

const val BBCODE_URL_ANNOTATION_TAG: String = "URL"

/**
 * Renders VNDB BBCode text using [BbCodeParser], memoized AST + [AnnotatedString] construction,
 * modern [LinkAnnotation.Url] (replacing deprecated `ClickableText`), and the global spoiler reveal toggle (U1, U5, U6, U7).
 */
@Composable
fun BbCodeText(
    rawText: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    linkColor: Color = MaterialTheme.colorScheme.primary,
    style: TextStyle = MaterialTheme.typography.bodyMedium
) {
    val context = LocalContext.current
    val parsedNodes = remember(rawText) { BbCodeParser.parse(rawText) }
    val hasSpoilers = remember(parsedNodes) { BbCodeParser.containsSpoiler(rawText) }
    var revealSpoilers by rememberSaveable(rawText) { mutableStateOf(false) }

    val spoilerPlaceholder = stringResource(R.string.bbcode_spoiler_placeholder)
    val emptyPlaceholder = stringResource(R.string.bbcode_empty_placeholder)

    val linkListener = remember(context) {
        LinkInteractionListener { link ->
            val url = (link as? LinkAnnotation.Url)?.url
            if (!url.isNullOrBlank()) {
                context.openExternalUrl(url)
            }
        }
    }

    val annotated = remember(
        parsedNodes,
        revealSpoilers,
        textColor,
        linkColor,
        spoilerPlaceholder,
        emptyPlaceholder,
        linkListener
    ) {
        if (rawText.isBlank()) {
            AnnotatedString(emptyPlaceholder)
        } else {
            buildAnnotatedStringFromBbCodeNodes(
                nodes = parsedNodes,
                revealSpoilers = revealSpoilers,
                baseColor = textColor,
                linkColor = linkColor,
                spoilerPlaceholder = spoilerPlaceholder,
                linkInteractionListener = linkListener
            )
        }
    }

    Column(modifier = modifier) {
        if (hasSpoilers) {
            val toggleLabel = if (revealSpoilers) {
                stringResource(R.string.bbcode_hide_spoilers)
            } else {
                stringResource(R.string.bbcode_reveal_spoilers)
            }
            Surface(
                color = linkColor.copy(alpha = 0.20f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .minimumInteractiveComponentSize()
                    .semantics { role = Role.Button }
                    .clickable(role = Role.Button) { revealSpoilers = !revealSpoilers }
            ) {
                Text(
                    text = toggleLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = linkColor,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        Text(
            text = annotated,
            style = style.copy(color = textColor)
        )
    }
}

/**
 * Pure helper converting parsed [BbCodeNode] trees into a styled [AnnotatedString] (U5, U7).
 */
fun buildAnnotatedStringFromBbCodeNodes(
    nodes: List<BbCodeNode>,
    revealSpoilers: Boolean,
    baseColor: Color,
    linkColor: Color,
    spoilerPlaceholder: String = "[Spoiler Hidden]",
    linkInteractionListener: LinkInteractionListener? = null
): AnnotatedString = buildAnnotatedString {
    fun renderNodes(list: List<BbCodeNode>) {
        for (node in list) {
            when (node) {
                is BbCodeNode.Text -> append(node.text)
                is BbCodeNode.Bold -> {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    renderNodes(node.children)
                    pop()
                }
                is BbCodeNode.Italic -> {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    renderNodes(node.children)
                    pop()
                }
                is BbCodeNode.Underline -> {
                    pushStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                    renderNodes(node.children)
                    pop()
                }
                is BbCodeNode.Strikethrough -> {
                    pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                    renderNodes(node.children)
                    pop()
                }
                is BbCodeNode.Code -> {
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = baseColor.copy(alpha = 0.15f)
                        )
                    )
                    renderNodes(node.children)
                    pop()
                }
                is BbCodeNode.Quote -> {
                    append("\n“")
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic, color = baseColor.copy(alpha = 0.90f)))
                    renderNodes(node.children)
                    pop()
                    append("”\n")
                }
                is BbCodeNode.Spoiler -> {
                    if (revealSpoilers) {
                        pushStyle(SpanStyle(background = baseColor.copy(alpha = 0.14f)))
                        renderNodes(node.children)
                        pop()
                    } else {
                        pushStyle(
                            SpanStyle(
                                color = linkColor,
                                fontStyle = FontStyle.Italic,
                                background = linkColor.copy(alpha = 0.14f)
                            )
                        )
                        append(" $spoilerPlaceholder ")
                        pop()
                    }
                }
                is BbCodeNode.Url -> {
                    val resolvedUrl = ExternalUriOpener.normalizeExternalUrl(node.target)
                    val linkStyle = SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.Medium
                    )
                    val startOffset = length
                    pushLink(
                        LinkAnnotation.Url(
                            url = resolvedUrl,
                            styles = TextLinkStyles(style = linkStyle),
                            linkInteractionListener = linkInteractionListener
                        )
                    )
                    pushStyle(linkStyle)
                    renderNodes(node.children)
                    pop()
                    pop()
                    val endOffset = length
                    if (endOffset > startOffset) {
                        addStringAnnotation(
                            tag = BBCODE_URL_ANNOTATION_TAG,
                            annotation = resolvedUrl,
                            start = startOffset,
                            end = endOffset
                        )
                    }
                }
            }
        }
    }
    renderNodes(nodes)
}

@Preview(name = "BbCodeText - Dark Theme", showBackground = true)
@Composable
private fun BbCodeTextDarkPreview() {
    VndbTheme(themeId = ThemeId.AGL, includeBackground = true) {
        BbCodeText(
            rawText = "A [b]bold and [i]nested italic[/i][/b] description with [spoiler]a hidden twist[/spoiler], " +
                "literal [1] bracket, and [url=/v17]Ever17 link[/url].",
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Preview(name = "BbCodeText - Light Theme", showBackground = true)
@Composable
private fun BbCodeTextLightPreview() {
    VndbTheme(themeId = ThemeId.SEINRKN, includeBackground = true) {
        BbCodeText(
            rawText = "[quote]“Look at the sky.”[/quote]\nCode sample: [code]v17[/code]",
            modifier = Modifier.padding(12.dp)
        )
    }
}
