package com.vndblite.dcj.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

/**
 * AST node for VNDB BBCode (D5).
 */
sealed interface BbNode {
    data class Text(val text: String) : BbNode
    data class Bold(val children: List<BbNode>) : BbNode
    data class Italic(val children: List<BbNode>) : BbNode
    data class Underline(val children: List<BbNode>) : BbNode
    data class Strikethrough(val children: List<BbNode>) : BbNode
    data class Code(val children: List<BbNode>) : BbNode
    data class Quote(val children: List<BbNode>) : BbNode
    data class Spoiler(val children: List<BbNode>) : BbNode
    data class Url(val target: String, val children: List<BbNode>) : BbNode
}

/**
 * Recursive-descent parser for VNDB BBCode supporting nested tags (`b`, `i`, `u`, `s`,
 * `code`, `quote`, `spoiler`, `url`, `raw`) while preserving unknown `[brackets]` literally (D5).
 */
object BbCodeParser {

    const val URL_ANNOTATION_TAG: String = "URL"

    private val supportedTags = setOf("b", "i", "u", "s", "code", "quote", "spoiler", "url", "raw")

    fun parse(input: String): List<BbNode> {
        if (input.isEmpty()) return emptyList()
        val normalized = input.replace("\r\n", "\n")
        return ParserState(normalized).parseNodes(stopClosingTag = null)
    }

    fun hasSpoilers(nodes: List<BbNode>): Boolean = nodes.any { node ->
        when (node) {
            is BbNode.Spoiler -> true
            is BbNode.Bold -> hasSpoilers(node.children)
            is BbNode.Italic -> hasSpoilers(node.children)
            is BbNode.Underline -> hasSpoilers(node.children)
            is BbNode.Strikethrough -> hasSpoilers(node.children)
            is BbNode.Code -> hasSpoilers(node.children)
            is BbNode.Quote -> hasSpoilers(node.children)
            is BbNode.Url -> hasSpoilers(node.children)
            is BbNode.Text -> false
        }
    }

    fun hasSpoilers(input: String): Boolean = hasSpoilers(parse(input))

    fun toAnnotatedString(
        nodes: List<BbNode>,
        revealSpoilers: Boolean,
        spoilerPlaceholder: String,
        baseColor: Color,
        linkColor: Color
    ): AnnotatedString = buildAnnotatedString {
        renderNodes(
            nodes = nodes,
            revealSpoilers = revealSpoilers,
            spoilerPlaceholder = spoilerPlaceholder,
            baseColor = baseColor,
            linkColor = linkColor
        )
    }

    private fun AnnotatedString.Builder.renderNodes(
        nodes: List<BbNode>,
        revealSpoilers: Boolean,
        spoilerPlaceholder: String,
        baseColor: Color,
        linkColor: Color
    ) {
        for (node in nodes) {
            when (node) {
                is BbNode.Text -> append(node.text)
                is BbNode.Bold -> {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    renderNodes(node.children, revealSpoilers, spoilerPlaceholder, baseColor, linkColor)
                    pop()
                }
                is BbNode.Italic -> {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    renderNodes(node.children, revealSpoilers, spoilerPlaceholder, baseColor, linkColor)
                    pop()
                }
                is BbNode.Underline -> {
                    pushStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                    renderNodes(node.children, revealSpoilers, spoilerPlaceholder, baseColor, linkColor)
                    pop()
                }
                is BbNode.Strikethrough -> {
                    pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                    renderNodes(node.children, revealSpoilers, spoilerPlaceholder, baseColor, linkColor)
                    pop()
                }
                is BbNode.Code -> {
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = baseColor.copy(alpha = 0.15f)
                        )
                    )
                    renderNodes(node.children, revealSpoilers, spoilerPlaceholder, baseColor, linkColor)
                    pop()
                }
                is BbNode.Quote -> {
                    append("“")
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic, color = baseColor.copy(alpha = 0.88f)))
                    renderNodes(node.children, revealSpoilers, spoilerPlaceholder, baseColor, linkColor)
                    pop()
                    append("”")
                }
                is BbNode.Spoiler -> {
                    if (revealSpoilers) {
                        pushStyle(SpanStyle(background = baseColor.copy(alpha = 0.14f)))
                        renderNodes(node.children, true, spoilerPlaceholder, baseColor, linkColor)
                        pop()
                    } else {
                        pushStyle(
                            SpanStyle(
                                color = linkColor,
                                background = linkColor.copy(alpha = 0.16f),
                                fontWeight = FontWeight.Medium
                            )
                        )
                        append(" $spoilerPlaceholder ")
                        pop()
                    }
                }
                is BbNode.Url -> {
                    val cleanTarget = node.target.trim()
                    pushStringAnnotation(tag = URL_ANNOTATION_TAG, annotation = cleanTarget)
                    pushStyle(
                        SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    if (node.children.isEmpty()) {
                        append(cleanTarget)
                    } else {
                        renderNodes(node.children, revealSpoilers, spoilerPlaceholder, baseColor, linkColor)
                    }
                    pop()
                    pop()
                }
            }
        }
    }

    private data class ParsedTag(
        val name: String,
        val isClosing: Boolean,
        val argument: String?,
        val endIndexExclusive: Int,
        val rawText: String
    )

    private class ParserState(private val source: String) {
        private var pos: Int = 0

        fun parseNodes(stopClosingTag: String?): List<BbNode> {
            val result = mutableListOf<BbNode>()
            val textBuffer = StringBuilder()

            fun flushText() {
                if (textBuffer.isNotEmpty()) {
                    result.add(BbNode.Text(textBuffer.toString()))
                    textBuffer.clear()
                }
            }

            while (pos < source.length) {
                val bracketIdx = source.indexOf('[', pos)
                if (bracketIdx < 0) {
                    textBuffer.append(source.substring(pos))
                    pos = source.length
                    break
                }
                if (bracketIdx > pos) {
                    textBuffer.append(source.substring(pos, bracketIdx))
                    pos = bracketIdx
                }

                val tag = tryParseKnownTagAt(pos)
                if (tag == null) {
                    textBuffer.append(source[pos])
                    pos++
                    continue
                }

                if (tag.isClosing) {
                    if (stopClosingTag != null && tag.name == stopClosingTag) {
                        pos = tag.endIndexExclusive
                        flushText()
                        return result
                    } else {
                        // Unmatched closing tag: preserve literally
                        textBuffer.append(tag.rawText)
                        pos = tag.endIndexExclusive
                        continue
                    }
                }

                // Opening tag
                flushText()
                pos = tag.endIndexExclusive
                when (tag.name) {
                    "raw" -> {
                        val closeRawIdx = findClosingTagCaseInsensitive("raw", pos)
                        if (closeRawIdx >= 0) {
                            val rawContent = source.substring(pos, closeRawIdx)
                            if (rawContent.isNotEmpty()) {
                                result.add(BbNode.Text(rawContent))
                            }
                            pos = closeRawIdx + "[/raw]".length
                        } else {
                            val remaining = source.substring(pos)
                            if (remaining.isNotEmpty()) {
                                result.add(BbNode.Text(remaining))
                            }
                            pos = source.length
                        }
                    }
                    "code" -> {
                        val closeCodeIdx = findClosingTagCaseInsensitive("code", pos)
                        if (closeCodeIdx >= 0) {
                            val codeContent = source.substring(pos, closeCodeIdx)
                            result.add(BbNode.Code(listOf(BbNode.Text(codeContent))))
                            pos = closeCodeIdx + "[/code]".length
                        } else {
                            val children = parseNodes(stopClosingTag = "code")
                            result.add(BbNode.Code(children))
                        }
                    }
                    "url" -> {
                        val children = parseNodes(stopClosingTag = "url")
                        val fallbackTarget = flattenPlainText(children).trim()
                        val target = tag.argument?.trim()?.takeIf { it.isNotEmpty() } ?: fallbackTarget
                        result.add(BbNode.Url(target = target, children = children))
                    }
                    "b" -> result.add(BbNode.Bold(parseNodes(stopClosingTag = "b")))
                    "i" -> result.add(BbNode.Italic(parseNodes(stopClosingTag = "i")))
                    "u" -> result.add(BbNode.Underline(parseNodes(stopClosingTag = "u")))
                    "s" -> result.add(BbNode.Strikethrough(parseNodes(stopClosingTag = "s")))
                    "quote" -> result.add(BbNode.Quote(parseNodes(stopClosingTag = "quote")))
                    "spoiler" -> result.add(BbNode.Spoiler(parseNodes(stopClosingTag = "spoiler")))
                }
            }

            flushText()
            return result
        }

        private fun tryParseKnownTagAt(startIndex: Int): ParsedTag? {
            if (startIndex >= source.length || source[startIndex] != '[') return null
            val closeBracket = source.indexOf(']', startIndex + 1)
            if (closeBracket < 0) return null
            val rawInside = source.substring(startIndex + 1, closeBracket)
            if (rawInside.isEmpty() || rawInside.contains('\n')) return null

            val isClosing = rawInside.startsWith("/")
            val body = if (isClosing) rawInside.substring(1) else rawInside
            val eqIdx = body.indexOf('=')
            val rawName = if (eqIdx >= 0) body.substring(0, eqIdx) else body
            val name = rawName.trim().lowercase()
            if (name !in supportedTags) return null
            if (isClosing && eqIdx >= 0) return null
            if (!isClosing && eqIdx >= 0 && name != "url" && name != "quote" && name != "spoiler") {
                return null
            }
            val arg = if (eqIdx >= 0) body.substring(eqIdx + 1) else null
            return ParsedTag(
                name = name,
                isClosing = isClosing,
                argument = arg,
                endIndexExclusive = closeBracket + 1,
                rawText = source.substring(startIndex, closeBracket + 1)
            )
        }

        private fun findClosingTagCaseInsensitive(tagName: String, fromIndex: Int): Int {
            val needle = "[/$tagName]"
            return source.indexOf(needle, startIndex = fromIndex, ignoreCase = true)
        }

        private fun flattenPlainText(nodes: List<BbNode>): String = buildString {
            nodes.forEach { node ->
                when (node) {
                    is BbNode.Text -> append(node.text)
                    is BbNode.Bold -> append(flattenPlainText(node.children))
                    is BbNode.Italic -> append(flattenPlainText(node.children))
                    is BbNode.Underline -> append(flattenPlainText(node.children))
                    is BbNode.Strikethrough -> append(flattenPlainText(node.children))
                    is BbNode.Code -> append(flattenPlainText(node.children))
                    is BbNode.Quote -> append(flattenPlainText(node.children))
                    is BbNode.Spoiler -> append(flattenPlainText(node.children))
                    is BbNode.Url -> append(flattenPlainText(node.children))
                }
            }
        }
    }
}
