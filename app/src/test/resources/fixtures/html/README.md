# HTML Scraper Test Fixtures (`N5`, `T1`)

These files are used by `JsoupReviewSourceTest` to verify review scraping, entity decoding, spoiler formatting, and structural error detection without hitting `vndb.org` during unit tests.

To replace the synthetic fixtures with captured production HTML snapshots from `vndb.org`:
1. **`vn_reviews_page.html`**: Save a VN page that has multiple reviews (including at least one long truncated review and spoiler tags), e.g.:
   - `curl -A "VNDBKotlin/3.6.0 (+https://github.com/HeyTahsin16/VNDB-Kotlin)" https://vndb.org/v17 -o app/src/test/resources/fixtures/html/vn_reviews_page.html`
2. **`vn_no_reviews_page.html`**: Save a valid VN page that has no `<div class="reviews">` block (a newly listed or unreviewed VN).
3. **`vn_full_review_page.html`**: Save an individual review page (`w<id>`), e.g.:
   - `curl -A "VNDBKotlin/3.6.0 (+https://github.com/HeyTahsin16/VNDB-Kotlin)" https://vndb.org/w1 -o app/src/test/resources/fixtures/html/vn_full_review_page.html`
