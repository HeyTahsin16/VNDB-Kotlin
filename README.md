<div align="center">
  <img src="assets/icons/launcher_icon.png" width="128" height="128" alt="VNDB Kotlin Icon" />
  <h1>VNDB Kotlin</h1>
  <p><b>A modern, fast, free, and open-source native Android client for <a href="https://vndb.org">VNDB.org</a> built with Kotlin, Jetpack Compose, and Material 3.</b></p>
  <p><a href="https://github.com/HeyTahsin16/VNDB-Kotlin">GitHub Repository</a> • <a href="https://github.com/HeyTahsin16/VNDB-Kotlin/issues">Issue Tracker</a></p>
</div>

---

## Overview

**VNDB Kotlin** is a complete native Android visual novel database client built from the ground up with **Kotlin** and **Jetpack Compose** using the **VNDB Kana API v2** (`https://api.vndb.org/kana`). Inspired by `vndb-lite`, it provides a rich, responsive mobile and tablet interface for discovering visual novels, exploring characters and traits, reading community reviews and memorable quotes, and managing your personal visual novel collection with two-way cloud synchronization.

---

## Key Features

### 1. Rich Home Dashboard
- **Curated Visual Novel Carousels**:
  - **My Collection** quick-access carousel (when your collection has entries)
  - **Top Rated** visual novels sorted by Bayesian rating
  - **Top Voted** visual novels sorted by community vote count
  - **Popular Ongoing** visual novels currently in development
  - **Latest Releases** showcasing recently released visual novels
  - **Upcoming Releases** highlighting scheduled future releases
- **Quick Genre Discovery**: One-tap genre cards (*Action, Comedy, Drama, Fantasy, Horror, Mystery, Romance, Sci-Fi*) with custom artwork.
- **VNDB Live Database Statistics**: Interactive bar chart displaying live counts of Visual Novels, Releases, Characters, Producers, Staff, Tags, and Traits.
- **Memorable & Random VNDB Quotes (`/kana/quote`)**: Dedicated section at the bottom of the Home tab displaying random and highest-voted quotes with character attribution and one-tap navigation to the source visual novel, plus a **Shuffle** button for fresh random quotes.

### 2. Dual-Mode Search (Visual Novels + Characters & Traits)
- **Visual Novels Search**:
  - Full-text search by title or alias with endless pagination.
  - **Sort & Filter Bottom Sheet** with:
    - Sort by *Bayesian Rating, Vote Count, Release Date, Title,* or *None* (ascending or descending).
    - **Combine Mode (`AND` / `OR`)** across filter categories.
    - Live autocomplete search for **Tags** (`/kana/tag`), **Developers/Producers** (`/kana/producer`), and **Character Traits** (`/kana/trait`) with an independent `AND` / `OR` toggle.
    - **Character Role** (*Protagonist, Main character, Side character, Makes an appearance*) and **Character Sex** (*Female, Male, Both, Sexless*) filters with independent `AND` / `OR` toggles.
    - **Available Languages**, **Original Language**, **Platforms**, **Development Status** (*Finished, In Development, Cancelled*), **Release Window** (*All, Latest Releases, Upcoming Releases*), and **Minimum Age** slider.
- **Characters & Traits Explorer**:
  - Dedicated character search mode querying `/kana/character`.
  - Filter characters by **Role** (*Protagonist, Main character, Side character, Makes an appearance*) with `AND` / `OR` logic.
  - Filter characters by **Sex** (*Female, Male, Both, Sexless*) with `AND` / `OR` logic.
  - Filter characters by **Character Traits** (including quick popular presets such as *Tsundere, Kuudere, Yandere, Dandere, Childhood Friend, Maid, Glasses, Twin Tails*, plus live trait search) with `AND` / `OR` logic.
  - Overall `Combine: AND / OR` toggle across Role, Sex, and Traits.
  - Tap any character card to view their full biography, physical stats (age, birthday, height, weight, bust-waist-hips, blood type, cup size), grouped traits, and visual novel appearances.

### 3. Comprehensive Visual Novel Detail View
Organized into five scrollable tabs with a hero cover/header card:
- **General**:
  - Full BBCode-rendered **Synopsis** (`[url]`, `[b]`, `[i]`, `[u]`, `[s]`, `[spoiler]`, `[quote]`, `[code]`).
  - **Screenshots Carousel** with default NSFW blur protection, per-image reveal toggle, and fullscreen viewer.
  - **Tags** with spoiler filtering (`Hide Spoilers` / `Show Spoilers`) and one-tap tag search.
  - **Memorable Quotes** from the visual novel with character attribution.
  - **Reviews & Personal Notes Preview** with quick access to the full Reviews tab.
- **Characters**:
  - All characters appearing in the visual novel with role badges, trait chips, and interactive character profile dialogs.
- **Reviews**:
  - **My Personal Review & Notes**: Write or edit your own personal review/notes and rating (`1–10`), saved locally and synced directly with your VNDB `ulist` notes.
  - **VNDB Community Reviews**: Browse published community reviews from `vndb.org/{vnId}/reviews` with filter chips (*All, Long, Medium, Short*), reviewer metadata, star ratings, helpfulness points, comment counts, and an inline **Read Full Review** expander that fetches complete long-form reviews on demand.
- **Releases**:
  - Minimum age rating, first release date, available languages (with machine translation indicators), supported platforms with custom platform icons, and developer/producer chips.
- **Relations**:
  - External store and database links (*Official Website, Steam, PlayStation, Nintendo Switch, GOG, J-List, Fakku, Wikipedia, Wikidata, VNDB*) and related visual novels (*Sequel, Prequel, Fandisc, Same Setting, Alternative Version, etc.*).

### 4. Offline-First Collection & Two-Way VNDB Cloud Sync
- Categorize visual novels into **Playing**, **Finished**, **Stalled**, **Dropped**, **Wishlist**, and **Blacklist**.
- Record personal votes (`1–10`), start/finish dates, and personal notes/reviews.
- Local search, filtering, sorting, and multi-select batch updates/deletions.
- **Two-Way VNDB Account Sync**: Authenticate with your personal VNDB API Token (generated from `vndb.org` -> *My Profile -> Applications*) to synchronize your local collection, ratings, statuses, and notes with your cloud VNDB `ulist`.

### 5. 11 Visual Novel Inspired Themes, Adaptive Layout & Default NSFW Blur
- **11 Built-in Themes**:
  - *Dark Sky (Default), Clairvoyance (Light), Everlasting Summer, Fate/stay night, Nekopara, Rose Nier, Steins;Gate, Tsukihime, Umineko, White Album 2,* and *Yuzusoft*.
- **Adaptive UI**: Supports both portrait handhelds (`NavigationBar`) and landscape/tablet screens (`NavigationRail` and multi-column grids).
- **NSFW Content Blurred by Default**: Automatically blurs covers, screenshots, and character images flagged for sexual or violent content across all Android versions, with one-tap per-item reveal (`NSFW 18+` / `Hide`) and a global toggle in Settings.

---

## Project Structure

```text
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── assets/images/           # Bundled platform, status, genre, and background assets
│       │   ├── java/com/vndblite/dcj/
│       │   │   ├── MainActivity.kt      # Adaptive NavigationBar / NavigationRail host & routing
│       │   │   ├── data/
│       │   │   │   ├── local/
│       │   │   │   │   └── LocalStorageManager.kt # SharedPreferences JSON cache for P1/P2/P3, collection, quotes, stats
│       │   │   │   ├── model/
│       │   │   │   │   └── Models.kt    # Data models, FilterData, SortData, VnCharacter, VnQuote, VnReview, ThemeCode
│       │   │   │   └── remote/
│       │   │   │       └── VndbApiService.kt      # OkHttp + Coroutines client for VNDB Kana API v2 & reviews parser
│       │   │   └── ui/
│       │   │       ├── components/      # Reusable Compose cards, NsfwAsyncImage, BBCode parser, dialogs, asset loaders
│       │   │       ├── screens/         # HomeScreen, SearchScreen, CollectionScreen, VnDetailScreen, Settings, Auth, About
│       │   │       ├── theme/           # 11 custom visual novel themes & Material 3 color schemes
│       │   │       └── viewmodel/       # VndbViewModel (StateFlow state management & cloud sync)
│       │   └── res/                     # Adaptive launcher icons, strings, and themes
│       └── test/
│           └── java/com/vndblite/dcj/
│               └── VndbModelsTest.kt    # Unit tests for models, filters, quotes, and review parsing
├── build.gradle.kts
├── gradle/libs.versions.toml
└── settings.gradle.kts
```

---

## Building & Running

### Prerequisites
- **Android Studio Ladybug (or newer)** / Android SDK 35+
- **JDK 17+**

### Clone the Repository
```bash
git clone https://github.com/HeyTahsin16/VNDB-Kotlin.git
cd VNDB-Kotlin
```

### Build Debug APK
```bash
gradle :app:assembleDebug
```

### Run Unit Tests
```bash
gradle :app:testDebugUnitTest
```

---

## Credits & License

- **Repository**: [HeyTahsin16/VNDB-Kotlin](https://github.com/HeyTahsin16/VNDB-Kotlin)
- **Database & API**: Powered by [VNDB.org](https://vndb.org) and the [VNDB Kana API v2](https://api.vndb.org/kana).
- **Original Inspiration**: Inspired by `vndb-lite` by Daniel C.J.
- **License**: Distributed under the [GPL-3.0 License](LICENSE).
