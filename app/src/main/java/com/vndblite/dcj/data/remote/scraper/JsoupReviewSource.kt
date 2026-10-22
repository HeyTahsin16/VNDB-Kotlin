package com.vndblite.dcj.data.remote.scraper

import com.vndblite.dcj.core.ApiError
import com.vndblite.dcj.core.AppInfo
import com.vndblite.dcj.core.AppLog
import com.vndblite.dcj.core.AppResult
import com.vndblite.dcj.core.DefaultDispatcherProvider
import com.vndblite.dcj.core.DispatcherProvider
import com.vndblite.dcj.core.runSuspendCatching
import com.vndblite.dcj.data.remote.http.RateLimiter
import com.vndblite.dcj.data.remote.http.await
import com.vndblite.dcj.data.remote.http.mapHttpStatusToApiError
import com.vndblite.dcj.data.remote.http.mapThrowableToApiError
import com.vndblite.dcj.data.remote.http.parseRetryAfterSeconds
import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.PaginatedList
import com.vndblite.dcj.domain.model.Review
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/**
 * Jsoup-based implementation of [ReviewSource] that scrapes `vndb.org` review HTML
 * using the honest `VNDBKotlin/<version>` User-Agent, precompiled patterns, full HTML entity decoding,
 * and structural markup validation so broken HTML surfaces as [ApiError.Parse] instead of silent emptiness (N2, N5, R10).
 */
class JsoupReviewSource(
    private val httpClient: OkHttpClient,
    private val rateLimiter: RateLimiter,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
    private val baseWebUrl: String = AppInfo.VNDB_URL,
    private val maxPages: Int = DEFAULT_MAX_PAGES
) : ReviewSource {

    override suspend fun fetchReviewsForVn(vnId: String): AppResult<PaginatedList<Review>> =
        withContext(dispatchers.io) {
            val cleanVnId = vnId.trim()
            if (cleanVnId.isEmpty()) {
                return@withContext AppResult.Failure(ApiError.BadRequest())
            }
            val collected = linkedMapOf<String, Review>()
            var currentUrl: String? = "${baseWebUrl.trimEnd('/')}/$cleanVnId"
            var pagesFetched = 0
            var truncatedByCap = false

            while (currentUrl != null && pagesFetched < maxPages) {
                val pageResult = fetchHtmlDocument(currentUrl)
                val document = when (pageResult) {
                    is AppResult.Success -> pageResult.data
                    is AppResult.Failure -> return@withContext pageResult
                }
                pagesFetched++

                val parseResult = parseReviewsDocument(document, cleanVnId)
                val parsedPage = when (parseResult) {
                    is AppResult.Success -> parseResult.data
                    is AppResult.Failure -> return@withContext parseResult
                }

                parsedPage.reviews.forEach { review ->
                    collected.putIfAbsent(review.id, review)
                }

                val nextRelativeOrAbs = parsedPage.nextPagePath
                if (nextRelativeOrAbs != null) {
                    if (pagesFetched >= maxPages) {
                        truncatedByCap = true
                        currentUrl = null
                    } else {
                        currentUrl = resolveUrl(nextRelativeOrAbs)
                    }
                } else {
                    currentUrl = null
                }
            }

            AppResult.Success(
                PaginatedList(
                    items = collected.values.toList(),
                    hasMore = truncatedByCap,
                    page = pagesFetched.coerceAtLeast(1)
                )
            )
        }

    override suspend fun fetchFullReviewText(reviewId: String): AppResult<String> =
        withContext(dispatchers.io) {
            val cleanId = reviewId.trim()
            if (cleanId.isEmpty()) {
                return@withContext AppResult.Failure(ApiError.BadRequest())
            }
            val url = "${baseWebUrl.trimEnd('/')}/$cleanId"
            when (val docResult = fetchHtmlDocument(url)) {
                is AppResult.Failure -> docResult
                is AppResult.Success -> parseFullReviewDocument(docResult.data)
            }
        }

    private suspend fun fetchHtmlDocument(url: String): AppResult<Document> =
        runSuspendCatching(
            tag = TAG,
            operation = "GET $url",
            errorMapper = ::mapThrowableToApiError
        ) {
            val outcome = rateLimiter.executeWithRetry(
                isRetryableStatus = { httpOutcome ->
                    if (httpOutcome.statusCode in RETRYABLE_STATUS_CODES) {
                        RateLimiter.RetryDirective(
                            statusCode = httpOutcome.statusCode,
                            retryAfterSeconds = httpOutcome.retryAfterSeconds
                        )
                    } else {
                        null
                    }
                }
            ) {
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()
                httpClient.newCall(request).await().use { response ->
                    val status = response.code
                    val retryAfter = parseRetryAfterSeconds(response.header(HEADER_RETRY_AFTER))
                    val html = if (response.isSuccessful) response.body?.string().orEmpty() else ""
                    RawHtmlResponse(statusCode = status, retryAfterSeconds = retryAfter, html = html)
                }
            }

            if (outcome.statusCode !in 200..299) {
                val apiError = mapHttpStatusToApiError(
                    statusCode = outcome.statusCode,
                    retryAfterHeader = outcome.retryAfterSeconds?.toString()
                )
                AppLog.w(TAG, "Non-2xx response (${outcome.statusCode}) fetching review HTML from $url")
                throw com.vndblite.dcj.data.remote.http.ApiErrorException(apiError)
            }
            if (outcome.html.isBlank()) {
                throw com.vndblite.dcj.data.remote.http.ApiErrorException(ApiError.Parse)
            }
            Jsoup.parse(outcome.html, baseWebUrl)
        }

    private fun resolveUrl(href: String): String {
        val trimmed = href.trim()
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "${baseWebUrl.trimEnd('/')}/${trimmed.trimStart('/')}"
        }
    }

    data class ParsedReviewPage(
        val reviews: List<Review>,
        val nextPagePath: String? = null
    )

    private data class RawHtmlResponse(
        val statusCode: Int,
        val retryAfterSeconds: Long?,
        val html: String
    )

    companion object {
        private const val TAG = "JsoupReviewSource"
        private const val HEADER_RETRY_AFTER = "Retry-After"
        const val DEFAULT_MAX_PAGES: Int = 10
        private val RETRYABLE_STATUS_CODES = setOf(429, 502, 503)

        // Precompiled patterns so zero Regex instances are allocated per call or loop iteration (N5).
        private val REVIEW_ID_REGEX = Regex("""^/?(w\d+)(?:[#?].*)?$""")
        private val HEADER_TYPE_DATE_REGEX =
            Regex("""\b(Short|Medium|Long)\b.*?\bon\s+(\d{4}-\d{2}-\d{2})""", RegexOption.DOT_MATCHES_ALL)
        private val USER_ID_HREF_REGEX = Regex("""^/?(u\d+)$""")
        private val RATING_TEXT_REGEX = Regex("""^(\d+(?:\.\d+)?/10)$""")
        private val COMMENTS_TEXT_REGEX = Regex("""(\d+\s+comments?)""", RegexOption.IGNORE_CASE)
        private val POINTS_TEXT_REGEX = Regex("""([+-]?\d+\s+points?)""", RegexOption.IGNORE_CASE)
        private val EXCESS_NEWLINES_REGEX = Regex("""\n{3,}""")

        /**
         * Parses a VNDB HTML string into a [PaginatedList] of [Review]s for unit testing and production use (N5).
         */
        fun parseReviewsHtml(html: String, vnId: String = ""): AppResult<ParsedReviewPage> {
            if (html.isBlank()) {
                return AppResult.Failure(ApiError.Parse)
            }
            val doc = Jsoup.parse(html)
            return parseReviewsDocument(doc, vnId)
        }

        internal fun parseReviewsDocument(doc: Document, vnId: String): AppResult<ParsedReviewPage> {
            // Verify that the page is recognizable VNDB HTML rather than an error gateway or unrelated page.
            val hasVndbStructure = doc.selectFirst("div.reviews, main, div.mainbox, body") != null &&
                (doc.selectFirst("div.reviews, main, div.mainbox, article, h1") != null)
            if (!hasVndbStructure) {
                return AppResult.Failure(ApiError.Parse)
            }

            val reviewsContainer = doc.selectFirst("div.reviews")
                ?: return AppResult.Success(ParsedReviewPage(reviews = emptyList(), nextPagePath = null))

            val articleElements = reviewsContainer.select("article")
            if (articleElements.isEmpty()) {
                // If div.reviews exists and has non-trivial child elements but no <article>, markup may have changed.
                val hasSuspiciousContent = reviewsContainer.children().isNotEmpty() &&
                    reviewsContainer.text().length > 80 &&
                    !reviewsContainer.text().contains("no reviews", ignoreCase = true)
                return if (hasSuspiciousContent) {
                    AppResult.Failure(ApiError.Parse)
                } else {
                    AppResult.Success(ParsedReviewPage(reviews = emptyList(), nextPagePath = null))
                }
            }

            val parsedReviews = mutableListOf<Review>()
            for (article in articleElements) {
                val parsed = parseArticleElement(article, vnId)
                if (parsed != null) {
                    parsedReviews.add(parsed)
                }
            }

            // If articles existed in div.reviews yet none could be parsed, flag a markup/parse failure (N5).
            if (parsedReviews.isEmpty()) {
                return AppResult.Failure(ApiError.Parse)
            }

            val nextLink = reviewsContainer
                .select("a[href*=/reviews], a[rel=next]")
                .firstOrNull { anchor ->
                    val text = anchor.text().trim()
                    text.contains("next", ignoreCase = true) ||
                        text.contains("more reviews", ignoreCase = true) ||
                        text == "»" ||
                        text == ">"
                }
                ?.attr("href")
                ?.takeIf { it.isNotBlank() }

            return AppResult.Success(
                ParsedReviewPage(
                    reviews = parsedReviews,
                    nextPagePath = nextLink
                )
            )
        }

        private fun parseArticleElement(article: Element, vnId: String): Review? {
            val reviewId = article.select("a[href]")
                .mapNotNull { a ->
                    REVIEW_ID_REGEX.matchEntire(a.attr("href").trim())?.groupValues?.getOrNull(1)
                }
                .firstOrNull()
                ?: return null

            val userAnchor = article.select("a[href]").firstOrNull { a ->
                USER_ID_HREF_REGEX.matches(a.attr("href").trim())
            }
            val userId = userAnchor?.attr("href")?.trim()?.removePrefix("/")?.takeIf { it.isNotEmpty() }
            val username = userAnchor?.text()?.trim()?.takeIf { it.isNotEmpty() } ?: "VNDB User"

            val articleText = article.text()
            val headerMatch = HEADER_TYPE_DATE_REGEX.find(articleText)
            val reviewType = headerMatch?.groupValues?.getOrNull(1) ?: "Review"
            val rawDate = headerMatch?.groupValues?.getOrNull(2)?.takeIf { it.isNotEmpty() }
            val parsedDate = CollectionEntry.parseLocalDateOrNull(rawDate)

            val ratingText = article.select("span")
                .map { it.text().trim() }
                .firstOrNull { RATING_TEXT_REGEX.matches(it) }

            val pointsText = article.select("small")
                .mapNotNull { small -> POINTS_TEXT_REGEX.find(small.text())?.groupValues?.getOrNull(1)?.trim() }
                .firstOrNull()

            val commentsText = article.select("a[href*=#threadstart], a[href]")
                .mapNotNull { a -> COMMENTS_TEXT_REGEX.find(a.text())?.groupValues?.getOrNull(1)?.trim() }
                .firstOrNull()

            val readMoreAnchor = article.select("a[href*=#review]").firstOrNull { a ->
                a.text().contains("Read more", ignoreCase = true)
            }
            val hasReadMore = readMoreAnchor != null

            val bodyText = extractArticleBodyText(article)
            if (bodyText.isBlank()) return null

            return Review(
                id = reviewId,
                vnId = vnId,
                username = username,
                userId = userId,
                reviewType = reviewType,
                date = parsedDate,
                rawDate = rawDate,
                ratingText = ratingText,
                text = bodyText,
                pointsText = pointsText,
                commentsCountText = commentsText,
                hasReadMore = hasReadMore
            )
        }

        private fun extractArticleBodyText(article: Element): String {
            // Clone the article so we can strip metadata chrome before extracting structured review text.
            val clone = article.clone()
            clone.select("a[href*=#review]").forEach { a ->
                if (a.text().contains("Read more", ignoreCase = true)) {
                    a.remove()
                }
            }
            clone.select("a[href*=#threadstart]").forEach { a ->
                a.parent()?.takeIf { it.tagName() == "div" && it.childrenSize() <= 2 }?.remove() ?: a.remove()
            }

            // Prefer the div that follows the header span containing the "report" link if present.
            val reportAnchor = clone.select("a").firstOrNull {
                it.text().equals("report", ignoreCase = true)
            }
            val headerSpan = reportAnchor?.parents()?.firstOrNull { it.tagName() == "span" }
            if (headerSpan != null) {
                val containerDiv = headerSpan.parent()
                headerSpan.remove()
                if (containerDiv != null) {
                    val extracted = renderElementPreservingSpoilersAndBreaks(containerDiv)
                    if (extracted.isNotBlank()) return extracted
                }
            }

            // Fallback: look for a dedicated review body container or paragraph inside the article.
            val bodyCandidate = clone.selectFirst("div.reviewbody, div.body, p") ?: clone
            return renderElementPreservingSpoilersAndBreaks(bodyCandidate)
        }

        fun parseFullReviewHtml(html: String): AppResult<String> {
            if (html.isBlank()) return AppResult.Failure(ApiError.Parse)
            return parseFullReviewDocument(Jsoup.parse(html))
        }

        internal fun parseFullReviewDocument(doc: Document): AppResult<String> {
            val reviewCell = doc.select("tr").firstOrNull { row ->
                val firstTd = row.selectFirst("td")
                firstTd != null && firstTd.text().trim().equals("Review", ignoreCase = true)
            }?.select("td")?.getOrNull(1)
                ?: doc.selectFirst("div.reviewbody, td.review")

            if (reviewCell == null) {
                return AppResult.Failure(ApiError.Parse)
            }
            val formatted = renderElementPreservingSpoilersAndBreaks(reviewCell)
            return if (formatted.isBlank()) {
                AppResult.Failure(ApiError.Parse)
            } else {
                AppResult.Success(formatted)
            }
        }

        internal fun renderElementPreservingSpoilersAndBreaks(root: Element): String {
            val builder = StringBuilder()
            appendNodeFormatted(root, builder)
            return builder.toString()
                .replace(EXCESS_NEWLINES_REGEX, "\n\n")
                .trim()
        }

        private fun appendNodeFormatted(node: Node, out: StringBuilder) {
            when (node) {
                is TextNode -> out.append(node.wholeText)
                is Element -> {
                    val tag = node.tagName().lowercase()
                    if (tag == "br") {
                        out.append('\n')
                        return
                    }
                    if (tag == "span" && node.hasClass("spoiler")) {
                        out.append("[Spoiler: ")
                        node.childNodes().forEach { child -> appendNodeFormatted(child, out) }
                        out.append(']')
                        return
                    }
                    node.childNodes().forEach { child -> appendNodeFormatted(child, out) }
                    if (tag == "p" || tag == "div") {
                        out.append("\n\n")
                    }
                }
            }
        }
    }
}
