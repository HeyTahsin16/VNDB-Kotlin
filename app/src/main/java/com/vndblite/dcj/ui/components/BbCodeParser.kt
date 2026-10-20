package com.vndblite.dcj.ui.components

/**
 * AST node produced by [BbCodeParser] supporting nested BBCode tags and literal fallback
 * for unknown or unbalanced brackets (U5).
 */
sealed interface BbCodeNode {
    data class Text(val text: String) : BbCodeNode
    data class Bold(val children: List<BbCodeNode>) : BbCodeNode
    data class Italic(val children: List<BbCodeNode>) : BbCodeNode
    data class Underline(val children: List<BbCodeNode>) : BbCodeNode
    data class Strikethrough(val children: List<BbCodeNode>) : BbCodeNode
    data class Code(val children: List<BbCodeNode>) : BbCodeNode
    data class Quote(val children: List<BbCodeNode>) : BbCodeNode
    data class Spoiler(val children: List<BbCodeNode>) : BbCodeNode
    data class Url(val target: String, val children: List<BbCodeNode>) : BbCodeNode
}

/**
 * Pure, zero-regex-in-loop tokenizer and recursive/stack-based parser for VNDB BBCode (U5, T1).
 *
 * Supported tags (case-insensitive):
 * - `[b]`, `[i]`, `[u]`, `[s]`, `[code]`, `[quote]`, `[spoiler]`, `[url]`, `[url=...]`, `[raw]`
 *
 * Guarantees:
 * - Nested tags (`[b]bold [i]italic[/i][/b]`, `[quote][spoiler]secret[/spoiler][/quote]`) preserve hierarchy.
 * - `[raw]...[/raw]` suppresses parsing of any inner tags until the matching `[/raw]`.
 * - Unknown brackets (e.g. `[protagonist]`, `[1]`, `[foo=bar]`) and unbalanced tags (e.g. `[b]unclosed` or
 *   stray `[/i]`) are preserved **literally** in the output rather than stripped.
 */
object BbCodeParser {

    private val KNOWN_SIMPLE_TAGS: Set<String> = setOf(
        "b", "i", "u", "s", "code", "quote", "spoiler", "url", "raw"
    )

    fun containsSpoiler(input: String): Boolean {
        if (input.isEmpty()) return false
        val nodes = parse(input)
        return containsSpoilerNode(nodes)
    }

    private fun containsSpoilerNode(nodes: List<BbCodeNode>): Boolean {
        for (node in nodes) {
            when (node) {
                is BbCodeNode.Spoiler -> return true
                is BbCodeNode.Bold -> if (containsSpoilerNode(node.children)) return true
                is BbCodeNode.Italic -> if (containsSpoilerNode(node.children)) return true
                is BbCodeNode.Underline -> if (containsSpoilerNode(node.children)) return true
                is BbCodeNode.Strikethrough -> if (containsSpoilerNode(node.children)) return true
                is BbCodeNode.Code -> if (containsSpoilerNode(node.children)) return true
                is BbCodeNode.Quote -> if (containsSpoilerNode(node.children)) return true
                is BbCodeNode.Url -> if (containsSpoilerNode(node.children)) return true
                is BbCodeNode.Text -> Unit
            }
        }
        return false
    }

    fun parse(input: String): List<BbCodeNode> {
        if (input.isEmpty()) return emptyList()
        val normalized = input.replace("\r\n", "\n")
        val tokens = tokenize(normalized)
        return mergeAdjacentTextNodes(parseTokens(tokens))
    }

    private sealed interface Token {
        data class Literal(val text: String) : Token
        data class OpenTag(
            val name: String,
            val arg: String?,
            val rawBracketText: String
        ) : Token
        data class CloseTag(
            val name: String,
            val rawBracketText: String
        ) : Token
    }

    private fun tokenize(source: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var index = 0
        val length = source.length

        while (index < length) {
            val openBracket = source.indexOf('[', startIndex = index)
            if (openBracket < 0) {
                tokens += Token.Literal(source.substring(index))
                break
            }
            if (openBracket > index) {
                tokens += Token.Literal(source.substring(index, openBracket))
            }

            val closeBracket = source.indexOf(']', startIndex = openBracket + 1)
            val nextOpenBracket = source.indexOf('[', startIndex = openBracket + 1)
            if (closeBracket < 0 || (nextOpenBracket in (openBracket + 1) until closeBracket)) {
                // Unclosed '[' before another '[' or end of string -> keep '[' literal
                tokens += Token.Literal("[")
                index = openBracket + 1
                continue
            }

            val rawTagToken = source.substring(openBracket, closeBracket + 1)
            val inner = source.substring(openBracket + 1, closeBracket)
            val parsedTag = classifyBracketContent(inner, rawTagToken)

            if (parsedTag is Token.OpenTag && parsedTag.name == "raw") {
                val closeRawRange = findClosingRawTag(source, startSearchIndex = closeBracket + 1)
                if (closeRawRange != null) {
                    val rawInner = source.substring(closeBracket + 1, closeRawRange.first)
                    if (rawInner.isNotEmpty()) {
                        tokens += Token.Literal(rawInner)
                    }
                    index = closeRawRange.last + 1
                    continue
                } else {
                    // Unclosed [raw] -> keep "[raw]" literally
                    tokens += Token.Literal(rawTagToken)
                    index = closeBracket + 1
                    continue
                }
            }

            tokens += parsedTag
            index = closeBracket + 1
        }
        return tokens
    }

    private fun classifyBracketContent(inner: String, rawBracketText: String): Token {
        if (inner.isEmpty()) return Token.Literal(rawBracketText)
        if (inner.startsWith("/")) {
            val tagName = inner.substring(1).lowercase()
            return if (tagName in KNOWN_SIMPLE_TAGS) {
                Token.CloseTag(name = tagName, rawBracketText = rawBracketText)
            } else {
                Token.Literal(rawBracketText)
            }
        }
        val equalsIndex = inner.indexOf('=')
        if (equalsIndex > 0) {
            val tagName = inner.substring(0, equalsIndex).lowercase()
            val argValue = inner.substring(equalsIndex + 1).trim()
            return if (tagName == "url" && argValue.isNotEmpty()) {
                Token.OpenTag(name = "url", arg = argValue, rawBracketText = rawBracketText)
            } else {
                Token.Literal(rawBracketText)
            }
        }
        val lower = inner.lowercase()
        return if (lower in KNOWN_SIMPLE_TAGS) {
            Token.OpenTag(name = lower, arg = null, rawBracketText = rawBracketText)
        } else {
            Token.Literal(rawBracketText)
        }
    }

    private fun findClosingRawTag(source: String, startSearchIndex: Int): IntRange? {
        var cursor = startSearchIndex
        val target = "[/raw]"
        while (cursor <= source.length - target.length) {
            val found = source.indexOf(target, startIndex = cursor, ignoreCase = true)
            if (found < 0) return null
            return found until (found + target.length)
        }
        return null
    }

    private class OpenFrame(
        val openTag: Token.OpenTag,
        val children: MutableList<BbCodeNode> = mutableListOf()
    )

    private fun parseTokens(tokens: List<Token>): List<BbCodeNode> {
        val rootChildren = mutableListOf<BbCodeNode>()
        val stack = ArrayDeque<OpenFrame>()

        fun currentTargetList(): MutableList<BbCodeNode> =
            stack.lastOrNull()?.children ?: rootChildren

        for (token in tokens) {
            when (token) {
                is Token.Literal -> {
                    currentTargetList() += BbCodeNode.Text(token.text)
                }
                is Token.OpenTag -> {
                    stack.addLast(OpenFrame(token))
                }
                is Token.CloseTag -> {
                    val matchIndex = stack.indexOfLast { it.openTag.name == token.name }
                    if (matchIndex < 0) {
                        // Unmatched closing tag -> keep literal (U5)
                        currentTargetList() += BbCodeNode.Text(token.rawBracketText)
                    } else {
                        // Unwind any inner unclosed tags above matchIndex as literals first (U5)
                        while (stack.size - 1 > matchIndex) {
                            val unclosed = stack.removeLast()
                            val parentList = currentTargetList()
                            parentList += BbCodeNode.Text(unclosed.openTag.rawBracketText)
                            parentList.addAll(unclosed.children)
                        }
                        val matchedFrame = stack.removeLast()
                        val node = buildNode(matchedFrame.openTag, mergeAdjacentTextNodes(matchedFrame.children))
                        currentTargetList() += node
                    }
                }
            }
        }

        // Any remaining unclosed tags on the stack become literal text followed by their parsed children (U5)
        while (stack.isNotEmpty()) {
            val unclosed = stack.removeLast()
            val parentList = currentTargetList()
            parentList += BbCodeNode.Text(unclosed.openTag.rawBracketText)
            parentList.addAll(unclosed.children)
        }

        return rootChildren
    }

    private fun buildNode(openTag: Token.OpenTag, children: List<BbCodeNode>): BbCodeNode =
        when (openTag.name) {
            "b" -> BbCodeNode.Bold(children)
            "i" -> BbCodeNode.Italic(children)
            "u" -> BbCodeNode.Underline(children)
            "s" -> BbCodeNode.Strikethrough(children)
            "code" -> BbCodeNode.Code(children)
            "quote" -> BbCodeNode.Quote(children)
            "spoiler" -> BbCodeNode.Spoiler(children)
            "url" -> {
                val explicitTarget = openTag.arg?.takeIf { it.isNotBlank() }
                val target = explicitTarget ?: extractPlainText(children).trim()
                BbCodeNode.Url(target = target, children = children)
            }
            else -> BbCodeNode.Text(openTag.rawBracketText + extractPlainText(children))
        }

    fun extractPlainText(nodes: List<BbCodeNode>): String = buildString {
        for (node in nodes) {
            when (node) {
                is BbCodeNode.Text -> append(node.text)
                is BbCodeNode.Bold -> append(extractPlainText(node.children))
                is BbCodeNode.Italic -> append(extractPlainText(node.children))
                is BbCodeNode.Underline -> append(extractPlainText(node.children))
                is BbCodeNode.Strikethrough -> append(extractPlainText(node.children))
                is BbCodeNode.Code -> append(extractPlainText(node.children))
                is BbCodeNode.Quote -> append(extractPlainText(node.children))
                is BbCodeNode.Spoiler -> append(extractPlainText(node.children))
                is BbCodeNode.Url -> append(extractPlainText(node.children))
            }
        }
    }

    private fun mergeAdjacentTextNodes(nodes: List<BbCodeNode>): List<BbCodeNode> {
        if (nodes.size <= 1) return nodes
        val merged = mutableListOf<BbCodeNode>()
        val textBuffer = StringBuilder()

        fun flushBuffer() {
            if (textBuffer.isNotEmpty()) {
                merged += BbCodeNode.Text(textBuffer.toString())
                textBuffer.setLength(0)
            }
        }

        for (node in nodes) {
            if (node is BbCodeNode.Text) {
                textBuffer.append(node.text)
            } else {
                flushBuffer()
                merged += node
            }
        }
        flushBuffer()
        return merged
    }
}
