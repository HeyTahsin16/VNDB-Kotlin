# Changelog

All notable changes to **VNDB Kotlin** (`HeyTahsin16/VNDB-Kotlin`) will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

> For the historical changelog of the upstream Flutter application (`Daniel-C-J/vndb-lite`) that inspired this project, see [`docs/UPSTREAM-FLUTTER-CHANGELOG.md`](docs/UPSTREAM-FLUTTER-CHANGELOG.md).

## [Unreleased]

### Added

- Repository hygiene, contributor guidelines (`CONTRIBUTING.md`), GitHub issue and pull request templates, `.editorconfig`, and `.gitattributes`.
- Credits & Attribution section in both `README.md` and the in-app **About** screen acknowledging `vndb-lite` by Daniel C.J. and the [VNDB.org Data License](https://vndb.org/d17#4).
- Single-source application metadata (`AppInfo`) for app name, version, license (`Apache-2.0`), repository links, and an honest `User-Agent` header (`VNDBKotlin/<versionName> (+https://github.com/HeyTahsin16/VNDB-Kotlin)`).

### Changed

- Unified all internal and user-facing product naming to **VNDB Kotlin** (`VndbApp`, `VndbTheme`, `Theme.VndbKotlin`).
- Enabled NSFW/suggestive image blur (`showCoverCensor = true`) by default across visual novel covers, screenshots, character portraits, and quote thumbnails with tap-to-reveal badges.
- Moved the inherited Flutter release history to `docs/UPSTREAM-FLUTTER-CHANGELOG.md`.

### Removed

- Unused Secrets Gradle Plugin configuration, legacy Flutter `bump.py` script, upstream funding/redirect files, and unused bundled font/splash/contact assets.

## [3.6.0] - 2025-08-18

### Added

- Initial Kotlin & Jetpack Compose Android port inspired by `vndb-lite` v3.6.0.
- Home dashboard with customizable visual novel preview carousels, random high-rated VN discovery, memorable quotes, and live VNDB database statistics.
- Visual Novel Search and Character & Trait Explorer powered by the VNDB Kana API v2.
- Visual Novel Detail view with overview, tags (with spoiler levels), screenshots, characters, releases, relations, quotes, and community reviews from `vndb.org`.
- Local collection management (Playing, Finished, Stalled, Dropped, Wishlist, Blacklist) with multi-select batch editing and Personal Access Token cloud synchronization.
- 11 classic visual novel themes (`Angel Serenade`, `AIR`, `Ever17`, `Fate/Stay Night`, `Gekkou No Carnevale`, `Higurashi No Naku Koro Ni`, `Tsukihime`, `Touhou`, `Seinarukana`, `Little Busters!`, `Saya No Uta`).

[Unreleased]: https://github.com/HeyTahsin16/VNDB-Kotlin/compare/v3.6.0...HEAD
[3.6.0]: https://github.com/HeyTahsin16/VNDB-Kotlin/releases/tag/v3.6.0
