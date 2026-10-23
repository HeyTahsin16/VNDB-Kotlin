package com.vndblite.dcj.ui.components

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BbCodeParserTest {

    @Test
    fun `nested bold and italic tags preserve hierarchy`() {
        val nodes = BbCodeParser.parse("Hello [b]bold [i]and italic[/i][/b] world")
        assertEquals(
            listOf(
                BbNode.Text("Hello "),
                BbNode.Bold(
                    listOf(
                        BbNode.Text("bold "),
                        BbNode.Italic(listOf(BbNode.Text("and italic")))
                    )
                ),
                BbNode.Text(" world")
            ),
            nodes
        )
    }

    @Test
    fun `unknown bracket tokens such as sic and Note 1 are preserved literally`() {
        val input = "She said [sic] that [Note 1] and [unknown_tag] remain intact."
        val nodes = BbCodeParser.parse(input)
        val rendered = BbCodeParser.toAnnotatedString(
            nodes = nodes,
            revealSpoilers = true,
            spoilerPlaceholder = "Spoiler",
            baseColor = Color.White,
            linkColor = Color.Cyan
        )
        assertEquals(input, rendered.text)
    }

    @Test
    fun `spoiler tag is detected and masked until revealed`() {
        val input = "Ending: [spoiler]True Heroine survives[/spoiler]!"
        val nodes = BbCodeParser.parse(input)
        assertTrue(BbCodeParser.hasSpoilers(nodes))

        val hidden = BbCodeParser.toAnnotatedString(
            nodes = nodes,
            revealSpoilers = false,
            spoilerPlaceholder = "[Spoiler]",
            baseColor = Color.White,
            linkColor = Color.Cyan
        )
        assertFalse(hidden.text.contains("True Heroine survives"))
        assertTrue(hidden.text.contains("[Spoiler]"))

        val revealed = BbCodeParser.toAnnotatedString(
            nodes = nodes,
            revealSpoilers = true,
            spoilerPlaceholder = "[Spoiler]",
            baseColor = Color.White,
            linkColor = Color.Cyan
        )
        assertEquals("Ending: True Heroine survives!", revealed.text)
    }

    @Test
    fun `url tag with explicit target and inner text attaches URL annotation`() {
        val input = "Visit [url=https://vndb.org/v17]Ever17 on VNDB[/url] now."
        val nodes = BbCodeParser.parse(input)
        val annotated = BbCodeParser.toAnnotatedString(
            nodes = nodes,
            revealSpoilers = false,
            spoilerPlaceholder = "Spoiler",
            baseColor = Color.White,
            linkColor = Color.Cyan
        )
        assertEquals("Visit Ever17 on VNDB now.", annotated.text)
        val urlAnnotations = annotated.getStringAnnotations(
            tag = BbCodeParser.URL_ANNOTATION_TAG,
            start = 0,
            end = annotated.length
        )
        assertEquals(1, urlAnnotations.size)
        assertEquals("https://vndb.org/v17", urlAnnotations.first().item)
    }

    @Test
    fun `raw tag prevents inner bbcode from being parsed`() {
        val input = "Literal: [raw][b]not bold[/b] [spoiler]visible[/spoiler][/raw]"
        val nodes = BbCodeParser.parse(input)
        assertFalse(BbCodeParser.hasSpoilers(nodes))
        val rendered = BbCodeParser.toAnnotatedString(
            nodes = nodes,
            revealSpoilers = false,
            spoilerPlaceholder = "Spoiler",
            baseColor = Color.White,
            linkColor = Color.Cyan
        )
        assertEquals("Literal: [b]not bold[/b] [spoiler]visible[/spoiler]", rendered.text)
    }

    @Test
    fun `quote and code and underline and strikethrough tags render cleanly`() {
        val input = "[quote]A [u]underlined[/u] and [s]struck[/s] and [code]x = 1[/code][/quote]"
        val nodes = BbCodeParser.parse(input)
        val rendered = BbCodeParser.toAnnotatedString(
            nodes = nodes,
            revealSpoilers = true,
            spoilerPlaceholder = "Spoiler",
            baseColor = Color.White,
            linkColor = Color.Cyan
        )
        assertEquals("“A underlined and struck and x = 1”", rendered.text)
    }
}
