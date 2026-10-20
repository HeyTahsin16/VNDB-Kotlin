package com.vndblite.dcj.ui.components

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BbCodeParserTest {

    @Test
    fun `parses nested formatting tags preserving hierarchy (U5, T1)`() {
        val input = "Start [b]bold and [i]bold-italic [u]underlined[/u][/i][/b] end"
        val nodes = BbCodeParser.parse(input)

        assertEquals(
            listOf(
                BbCodeNode.Text("Start "),
                BbCodeNode.Bold(
                    listOf(
                        BbCodeNode.Text("bold and "),
                        BbCodeNode.Italic(
                            listOf(
                                BbCodeNode.Text("bold-italic "),
                                BbCodeNode.Underline(listOf(BbCodeNode.Text("underlined")))
                            )
                        )
                    )
                ),
                BbCodeNode.Text(" end")
            ),
            nodes
        )
    }

    @Test
    fun `parses nested quote and spoiler tags and detects spoilers (U5, T1)`() {
        val input = "[quote]A wise person said: [spoiler]the butler did it[/spoiler][/quote]"
        assertTrue(BbCodeParser.containsSpoiler(input))

        val nodes = BbCodeParser.parse(input)
        assertEquals(
            listOf(
                BbCodeNode.Quote(
                    listOf(
                        BbCodeNode.Text("A wise person said: "),
                        BbCodeNode.Spoiler(listOf(BbCodeNode.Text("the butler did it")))
                    )
                )
            ),
            nodes
        )

        val hiddenAnnotated = buildAnnotatedStringFromBbCodeNodes(
            nodes = nodes,
            revealSpoilers = false,
            baseColor = Color.White,
            linkColor = Color.Cyan,
            spoilerPlaceholder = "[Spoiler Hidden]"
        )
        assertTrue(hiddenAnnotated.text.contains("[Spoiler Hidden]"))
        assertFalse(hiddenAnnotated.text.contains("the butler did it"))

        val revealedAnnotated = buildAnnotatedStringFromBbCodeNodes(
            nodes = nodes,
            revealSpoilers = true,
            baseColor = Color.White,
            linkColor = Color.Cyan,
            spoilerPlaceholder = "[Spoiler Hidden]"
        )
        assertTrue(revealedAnnotated.text.contains("the butler did it"))
        assertFalse(revealedAnnotated.text.contains("[Spoiler Hidden]"))
    }

    @Test
    fun `parses url tags with explicit target and self-target and normalizes relative vndb urls (U5, T1)`() {
        val input = "See [url=/v17][b]Ever17[/b][/url] or [url]https://vndb.org/v2[/url]"
        val nodes = BbCodeParser.parse(input)

        assertEquals(
            listOf(
                BbCodeNode.Text("See "),
                BbCodeNode.Url(
                    target = "/v17",
                    children = listOf(BbCodeNode.Bold(listOf(BbCodeNode.Text("Ever17"))))
                ),
                BbCodeNode.Text(" or "),
                BbCodeNode.Url(
                    target = "https://vndb.org/v2",
                    children = listOf(BbCodeNode.Text("https://vndb.org/v2"))
                )
            ),
            nodes
        )

        val annotated = buildAnnotatedStringFromBbCodeNodes(
            nodes = nodes,
            revealSpoilers = false,
            baseColor = Color.White,
            linkColor = Color.Cyan
        )
        val urlAnnotations = annotated.getStringAnnotations(BBCODE_URL_ANNOTATION_TAG, 0, annotated.length)
        assertEquals(2, urlAnnotations.size)
        assertEquals("https://vndb.org/v17", urlAnnotations[0].item)
        assertEquals("https://vndb.org/v2", urlAnnotations[1].item)
    }

    @Test
    fun `raw tag suppresses inner BBCode parsing until closing raw tag (U5, T1)`() {
        val input = "Before [raw][b]literal bold[/b] and [spoiler]not a spoiler[/spoiler][/raw] After"
        assertFalse(BbCodeParser.containsSpoiler(input))

        val nodes = BbCodeParser.parse(input)
        assertEquals(
            listOf(
                BbCodeNode.Text("Before [b]literal bold[/b] and [spoiler]not a spoiler[/spoiler] After")
            ),
            nodes
        )
    }

    @Test
    fun `unknown and unbalanced brackets are preserved literally instead of stripped (U5, T1)`() {
        val input = "Footnote [1] and [protagonist] with [b]unclosed bold and stray [/i] tag"
        val nodes = BbCodeParser.parse(input)
        val plain = BbCodeParser.extractPlainText(nodes)

        assertEquals(input, plain)
    }
}
