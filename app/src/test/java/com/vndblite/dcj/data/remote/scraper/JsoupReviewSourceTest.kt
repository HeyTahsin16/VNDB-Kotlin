package com.vndblite.dcj.data.remote.scraper

import com.vndblite.dcj.core.ApiError
import com.vndblite.dcj.core.AppResult
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JsoupReviewSourceTest {

    private fun readFixture(path: String): String {
        val stream = checkNotNull(javaClass.classLoader?.getResourceAsStream(path)) {
            "Missing test fixture: $path"
        }
        return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    @Test
    fun `parseReviewsHtml extracts reviews decodes all HTML entities and formats spoilers`() {
        val html = readFixture("fixtures/html/vn_reviews_page.html")
        val result = JsoupReviewSource.parseReviewsHtml(html, vnId = "v17")
        assertTrue(result is AppResult.Success)
        val reviews = (result as AppResult.Success).data.reviews
        assertEquals(2, reviews.size)

        val first = reviews[0]
        assertEquals("w501", first.id)
        assertEquals("v17", first.vnId)
        assertEquals("u99", first.userId)
        assertEquals("Takeshi & Blick", first.username)
        assertEquals("Long", first.reviewType)
        assertEquals(LocalDate.of(2024, 3, 15), first.date)
        assertEquals("9.5/10", first.ratingText)
        assertEquals("+14 points", first.pointsText)
        assertEquals("6 comments", first.commentsCountText)
        assertTrue(first.hasReadMore)
        assertTrue(first.text.contains("First paragraph with — em-dash & “curly quotes”."))
        assertTrue(first.text.contains("[Spoiler: Blick Winkel is the player]"))

        val second = reviews[1]
        assertEquals("w502", second.id)
        assertFalse(second.hasReadMore)
    }

    @Test
    fun `parseReviewsHtml returns empty list for valid VN page with no reviews section`() {
        val html = readFixture("fixtures/html/vn_no_reviews_page.html")
        val result = JsoupReviewSource.parseReviewsHtml(html, vnId = "v999")
        assertTrue(result is AppResult.Success)
        assertTrue((result as AppResult.Success).data.reviews.isEmpty())
    }

    @Test
    fun `parseReviewsHtml returns ApiError Parse when review articles fail to parse due to markup change`() {
        val brokenMarkupHtml = """
            <html><body><main>
              <div class="reviews">
                <article><div class="changed-markup">Broken article without review link</div></article>
              </div>
            </main></body></html>
        """.trimIndent()
        val result = JsoupReviewSource.parseReviewsHtml(brokenMarkupHtml, vnId = "v17")
        assertEquals(AppResult.Failure(ApiError.Parse), result)
    }

    @Test
    fun `parseFullReviewHtml extracts full review text and decodes numeric entities`() {
        val html = readFixture("fixtures/html/vn_full_review_page.html")
        val result = JsoupReviewSource.parseFullReviewHtml(html)
        assertTrue(result is AppResult.Success)
        val text = (result as AppResult.Success).data
        assertTrue(text.contains("Complete full review text with ’ apostrophe."))
        assertTrue(text.contains("[Spoiler: hidden twist]"))
    }
}
