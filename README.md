<div align="center">

<img src="app/src/main/assets/images/icon/app_icon.png" alt="VNDB Lite-Kotlin App Icon" width="110" />

# VNDB Lite-Kotlin

**A free, open-source native Android client for [The Visual Novel Database (vndb.org)](https://vndb.org), written in Kotlin and Jetpack Compose.**

[![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B%20(API%2024%2B)-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20(Material%203)-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![VNDB API](https://img.shields.io/badge/API-VNDB%20Kana%20v2-0A7E8C)](https://api.vndb.org/kana)
[![License](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](LICENSE)

</div>

---

## Overview

**VNDB Kotlin** is a native Android application for browsing, discovering, and managing visual novels from [vndb.org](https://vndb.org). Inspired by [**vndb-lite**](https://github.com/Daniel-C-J/vndb-lite) by Daniel C.J. (the original Flutter application from which the visual novel theme artwork, status/platform/flag icons, and layout concepts were carried over), this project re-implements the client in **Kotlin** and **Jetpack Compose** and adds a **Character & Trait Explorer**, **Memorable Quotes**, and **Community Reviews**.

---

## Screenshots

> **Placeholder (`docs/screenshots/`)**: Screenshots are being captured for the Kotlin & Jetpack Compose UI. See [`docs/screenshots/README.md`](docs/screenshots/README.md) for the list of 8 target screens (`<= 250 KB` WebP each).

<!--
| Home & Previews | VN Search & Filters | Character & Trait Explorer | VN Detail & Reviews |
| :---: | :---: | :---: | :---: |
| ![Home](docs/screenshots/01-home.webp) | ![VN Search](docs/screenshots/02-vn-search-filters.webp) | ![Characters](docs/screenshots/03-character-trait-explorer.webp) | ![Reviews](docs/screenshots/05-vn-detail-reviews.webp) |
-->

---

## Features

### 1. Visual Novel Discovery & Home Dashboard
- **Customizable Home Carousels**: Browse up to 6 configurable home sections (*Top Rated*, *Most Voted*, *Popular Ongoing*, *Latest Releases*, *Upcoming Releases*, and *My Collection*).
- **Quick Genre & Trait Presets**: One-tap chips on the Home screen jump straight into Visual Novel genre searches or Character trait searches.
- **Random High-Rated VN**: Roll a random visual novel from VNDB's catalog with quick access to its full detail page.
- **Memorable Quotes & Live Database Stats**: Read curated quotes with one-tap navigation to the source visual novel, and view live VNDB database counts (Visual Novels, Releases, Producers, Characters, Staff, Tags, and Traits).

### 2. Visual Novel Search & Multi-Faceted Filtering
- **Remote Filter Builder**: Combine title/keyword queries with multi-select filters for **Tags**, **Character Traits**, **Developers/Producers**, **Available Languages**, **Original Language**, **Platforms**, **Development Status**, **Character Role**, **Character Sex**, and **Release Window**.
- **Boolean Logic (`AND` / `OR`)**: Toggle between `AND` and `OR` matching for tags, traits, developers, roles, and sexes.
- **Flexible Sorting**: Sort results by *Rating*, *Vote Count*, *Release Date*, *Title*, or *ID* in ascending or descending order.

### 3. Character & Trait Explorer
- **Dedicated Characters & Traits Mode**: Switch the Search screen into **Characters & Traits** mode to search characters directly across VNDB.
- **Curated Trait Chips & Live Trait Search**: Filter characters by personality, hair, eyes, role, or body traits, as well as character role (*Protagonist*, *Main character*, *Side character*, *Makes an appearance*) and sex.
- **Rich Character Bio Dialog**: Inspect character portraits, Japanese/original names, aliases, measurements, birthday, blood type, BBCode biography (with spoiler toggle), traits grouped by category, and linked visual novel appearances.

### 4. Comprehensive Visual Novel Details & Community Reviews
- **Three-Tab Detail View**:
  - **Overview & Tags**: Cover art, rating, vote count, play length, development status, minimum age, languages (with machine-translation indicators), platforms, BBCode description, spoiler-adjustable tags (`None`, `Minor`, `Major`), and a screenshots carousel with fullscreen viewer.
  - **Characters, Quotes & Reviews**: Cast cards with trait chips, VN-specific quotes, and **Community Reviews** fetched from `vndb.org` with reviewer ratings, vote scores, dates, comment counts, spoiler support, and expandable full-text reading.
  - **Releases, Relations & Links**: Chronological release list, official/unofficial related visual novels (sequels, prequels, fandiscs, same-setting works), and external store/wiki links.

### 5. Local Collection & Cloud Synchronization
- **Six Collection Categories**: Organize visual novels under *Playing*, *Finished*, *Stalled*, *Dropped*, *Wishlist*, and *Blacklist*.
- **Detailed Record Tracking**: Log your personal score (`1`–`10`), start/finish dates, and personal notes. Supports multi-selection batch updates and local collection filtering/sorting.
- **Cached Offline Viewing**: Visual novels in your collection remain accessible offline with cached metadata.
- **VNDB Cloud Sync**: Authenticate with a VNDB **Personal Access Token** (`listread` and `listwrite`) to synchronize your collection statuses, votes, dates, and notes with your `vndb.org` `ulist`.

### 6. Default NSFW Blur & 11 Visual Novel Themes
- **NSFW Content Blurred by Default**: Suggestive and explicit covers, screenshots, character portraits, and quote thumbnails are automatically blurred by default (`showCoverCensor = true`) with an **NSFW 18+** tap-to-reveal badge and a global toggle in Settings.
- **11 Classic Visual Novel Themes**: Every theme includes custom palette colors and background artwork:

| Code | Theme Name | Background Asset |
| :--- | :--- | :--- |
| `agl` | **Angel Serenade** *(Default)* | `assets/images/bg/bgAngel.png` |
| `air` | **AIR** | `assets/images/bg/bgAIR.png` |
| `ev17` | **Ever17** | `assets/images/bg/bgEver17.png` |
| `fate` | **Fate/Stay Night** | `assets/images/bg/bgFate.png` |
| `carnvl` | **Gekkou No Carnevale** | `assets/images/bg/bgCarnevale.png` |
| `hgrshi` | **Higurashi No Naku Koro Ni** | `assets/images/bg/bgHigurashi.png` |
| `tsukhme` | **Tsukihime** | `assets/images/bg/bgTsukihime.png` |
| `toho` | **Touhou** | `assets/images/bg/bgTouhou.png` |
| `seinrkn` | **Seinarukana** | `assets/images/bg/bgSeinaru.png` |
| `lilbusts` | **Little Busters!** | `assets/images/bg/bgLittleBusters.png` |
| `saya` | **Saya No Uta** | `assets/images/bg/bgSaya.png` |

---

## Installation

1. Download the latest APK from the [GitHub Releases](https://github.com/HeyTahsin16/VNDB-Kotlin/releases) page *(or build from source using the instructions below)*.
2. Install the APK on a device running **Android 7.0 (API 24)** or newer.
3. *(Optional)* To sync with your `vndb.org` account, open **Others → VNDB Account & Sync** and paste a Personal Access Token with `listread` and `listwrite` permissions enabled.

---

## Building from Source

### Prerequisites
- **JDK 21** (e.g., Eclipse Temurin 21; configured via `JavaVersion.VERSION_21` in `app/build.gradle.kts`)
- **Android SDK**: `compileSdk = 36`, `targetSdk = 36`, `minSdk = 24`
- **Android Gradle Plugin (AGP)**: `9.1.1` / **Gradle Wrapper**: `9.3.1` / **Kotlin**: `2.2.10`
- **Android Studio**: An Android Studio release supporting AGP `9.1.1` *(VERIFY exact Studio channel/version on your workstation)*

### Commands

Run all commands from the repository root using the Gradle wrapper:

```bash
# 1. Clone the repository
git clone https://github.com/HeyTahsin16/VNDB-Kotlin.git
cd VNDB-Kotlin

# 2. Assemble the debug APK
./gradlew assembleDebug

# 3. Run unit tests
./gradlew testDebugUnitTest

# 4. Run Android Lint checks
./gradlew lintDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

### Release Signing (`keystore.properties` or CI Secrets)

Release builds enable R8 code shrinking (`isMinifyEnabled = true`) and resource shrinking (`isShrinkResources = true`).

- **Local Release Build**: Copy [`keystore.properties.example`](keystore.properties.example) to `keystore.properties` (which is git-ignored), point `storeFile` to your `.jks` / `.keystore` file, and run:
  ```bash
  ./gradlew assembleRelease
  ```
- **CI Release Workflow (`.github/workflows/release.yml`)**: When pushing a `v*.*.*` tag, GitHub Actions builds and attaches a signed APK if the following repository secrets are configured under **Settings → Secrets and variables → Actions**:
  - `KEYSTORE_BASE64`: Base64-encoded release keystore (`base64 -w 0 release-keystore.jks`)
  - `KEYSTORE_PASSWORD`: Keystore password
  - `KEY_ALIAS`: Key alias
  - `KEY_PASSWORD`: Key password
- **Unsigned Fallback**: If neither `keystore.properties` nor `KEYSTORE_*` environment variables are present, `assembleRelease` produces an unsigned APK and logs a clear warning.

### Bumping Version & Tagging a Release

[`version.properties`](version.properties) is the single source of truth for both `versionName` and `versionCode`:

1. Edit `version.properties` to update `VERSION_NAME` (e.g. `3.7.0`) and increment `VERSION_CODE` (e.g. `30700`).
2. Update [`CHANGELOG.md`](CHANGELOG.md) under the new version heading.
3. Commit and tag:
   ```bash
   git commit -am "chore(release): v3.7.0"
   git tag -a v3.7.0 -m "VNDB Kotlin v3.7.0"
   git push origin main --tags
   ```

---

## Project Structure

```text
.
├── .github/
│   ├── ISSUE_TEMPLATE/             # Bug report & feature request YAML forms
│   ├── workflows/                  # GitHub Actions CI (ci.yml) & Release (release.yml)
│   └── PULL_REQUEST_TEMPLATE.md    # Pull request verification checklist
├── app/
│   ├── build.gradle.kts            # App module Gradle configuration (SDK 36, Java 21, R8)
│   ├── proguard-rules.pro          # Focused R8 rules for Serialization, Room, OkHttp, Jsoup
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/vndblite/dcj/
│       │   │   ├── MainActivity.kt
│       │   │   ├── core/           # AppInfo (single-source metadata & User-Agent)
│       │   │   ├── data/
│       │   │   │   ├── local/      # LocalStorageManager
│       │   │   │   ├── model/      # Domain & DTO models
│       │   │   │   └── remote/     # VndbApiService (VNDB Kana API v2 + HTML reviews)
│       │   │   └── ui/
│       │   │       ├── components/ # Reusable Compose components & NsfwAsyncImage
│       │   │       ├── screens/    # Home, Search, Detail, Collection, Settings/About
│       │   │       ├── theme/      # VndbTheme & Material 3 color/typography helpers
│       │   │       └── viewmodel/  # VndbViewModel
│       │   └── res/                # Adaptive launcher icons, backup rules, strings, themes
│       └── test/
│           └── java/com/vndblite/dcj/
│               └── VndbModelsTest.kt
├── assets/
│   └── images/                     # Theme backgrounds (11), flags (35), OS icons (32), status (6), search (8), tutorial (3), icon (1), contact (1)
├── docs/
│   ├── screenshots/                # Screenshot placeholders & capture checklist
│   └── UPSTREAM-FLUTTER-CHANGELOG.md
├── gradle/
│   ├── libs.versions.toml          # Centralized Gradle Version Catalog
│   └── wrapper/                    # Gradle 9.3.1 wrapper configuration & JAR
├── .editorconfig
├── .gitattributes
├── .gitignore
├── build.gradle.kts
├── CHANGELOG.md
├── CONTRIBUTING.md
├── gradlew
├── gradlew.bat
├── keystore.properties.example
├── LICENSE
├── README.md
├── settings.gradle.kts
└── version.properties              # Single source of truth for versionName & versionCode
```

---

## Privacy & API Rate Limits

### Privacy
- **No Analytics or Third-Party Trackers**: VNDB Kotlin contains no telemetry, analytics SDKs, or advertising libraries.
- **Personal Access Token**: Your VNDB Personal Access Token is stored locally on your device and is transmitted **exclusively** to `https://api.vndb.org/kana` via the `Authorization: token <token>` header when authenticating or synchronizing your collection.
- **Network Requests**:
  - `https://api.vndb.org/kana` — Official VNDB Kana API v2 (visual novels, characters, traits, tags, producers, quotes, database stats, and user list sync).
  - `https://t.vndb.org` — Official VNDB image CDN for covers, screenshots, and character portraits.
  - `https://vndb.org` — Read-only HTML requests to `https://vndb.org/<vnId>/reviews` and `https://vndb.org/<reviewId>` for community reviews (as the Kana API v2 does not yet expose a reviews endpoint).
- **Honest Identification**: All HTTP requests identify the client with `User-Agent: VNDBKotlin/<versionName> (+https://github.com/HeyTahsin16/VNDB-Kotlin)`.

### VNDB API Rate Limits
Per the [VNDB API v2 documentation](https://api.vndb.org/kana):
- **Request Limit**: 200 requests per 5 minutes.
- **Execution Time Limit**: 1 second of server execution time per minute (individual queries taking longer than 3 seconds are aborted).

---

## Credits & Attribution

- **[vndb-lite](https://github.com/Daniel-C-J/vndb-lite) by Daniel C.J.**: Thank you to Daniel C.J. for creating the original `vndb-lite` Flutter application, which inspired this project and from which the theme background artwork, flag/platform/status icons, and core UX concepts were carried over. The upstream Flutter release history is preserved in [`docs/UPSTREAM-FLUTTER-CHANGELOG.md`](docs/UPSTREAM-FLUTTER-CHANGELOG.md).
- **[The Visual Novel Database (VNDB.org)](https://vndb.org)**: All visual novel metadata, character entries, traits, tags, quotes, and reviews are provided by VNDB.org. Database contents are subject to the [VNDB.org Data License (`https://vndb.org/d17#4`)](https://vndb.org/d17#4).

---

## License

This project is licensed under the **Apache-2.0** License — see the [`LICENSE`](LICENSE) file for details.
