# VNDB Kotlin Refactor Progress

## Legacy Feature & Setting Inventory (Nothing May Be Lost)

### 1. Settings & Preferences (`AppSettings` / `SettingsStore`)
- `themeId` (`ThemeId`): 11 visual novel themes (`agl` Angel Serenade default, `air` AIR, `ev17` Ever17, `fate` Fate/Stay Night, `carnvl` Gekkou No Carnevale, `hgrshi` Higurashi No Naku Koro Ni, `tsukhme` Tsukihime, `toho` Touhou, `seinrkn` Seinarukana, `lilbusts` Little Busters!, `saya` Saya No Uta), each with custom primary/secondary/tertiary palette and bundled background artwork.
- `showCoverCensor` (`Boolean`, default `true`): NSFW blur enabled by default for suggestive/explicit covers, screenshots, character portraits, and quote thumbnails, with per-item tap-to-reveal (`NSFW 18+` badge) preserved across scrolling/recomposition.
- `showChart` (`Boolean`, default `true`): Collection distribution radar/bar chart on the Home screen.
- `fontSizeOffset` (`Float`, default `0.0f`, range `-4.0f..8.0f`): User-configurable font scale offset applied across the 15-style Material 3 typography.
- `maxPreviewItem` (`Int`, default `20`, range `5..50`): Maximum items per Home screen horizontal preview carousel.
- `maxItemPerRowPortrait` (`Int`, default `2`, range `1..6`): Grid columns in portrait orientation.
- `maxItemPerRowLandscape` (`Int`, default `4`, range `2..10`): Grid columns in landscape/wide orientation.
- `collectionStatusTabArrangement` (`List<CollectionStatus>`): Reorderable collection status tabs (*Playing*, *Finished*, *Stalled*, *Dropped*, *Wishlist*, *Blacklist*).
- Settings actions: Reset settings to defaults (with confirmation), clear transient cache (preserving collection entries), clear local collection (with option to also queue remote deletions when synced).

### 2. Home Screen (`HomeScreen`)
- Greeting banner with app icon, title/subtitle, refresh button (`home_refresh_button`), and quick-action buttons:
  - Character Trait Search (`home_trait_finder_button`)
  - Random High-Rated VN (`home_random_vn_button`)
- 6 Home preview carousels (`HomeSection`):
  - *My Collection* (`COLLECTION`, ordered deterministically by `lastModifiedAt DESC`, "See more" navigates to Collection tab)
  - *Top Rated* (`TOP_RATED`, `see_more_top_rated`)
  - *Most Voted* (`TOP_VOTED`, `see_more_top_voted`)
  - *Popular Ongoing* (`POPULAR_ONGOING`, `see_more_popular_ongoing`)
  - *Latest Releases* (`LATEST_RELEASES`, `see_more_latest_releases`)
  - *Upcoming Releases* (`UPCOMING_RELEASES`, `see_more_upcoming_releases`)
  - Per-section stale-while-revalidate (1 h TTL for Latest/Upcoming, 12 h TTL for others) and per-section error state with retry.
- Quick Genre Presets (8 illustrated cards: Action, Comedy, Drama, Fantasy, Horror, Mystery, Romance, Sci-Fi -> opens VN Search with that tag).
- Quick Character Trait Presets (curated trait chips -> opens Character & Trait Explorer with that trait).
- Memorable Quotes section with shuffle/refresh action, NSFW cover thumbnail handling, character attribution, and one-tap navigation to the source VN.
- Collection Status Radar/Distribution Chart (when `showChart == true`).
- Live VNDB Database Stats (Visual Novels, Releases, Producers, Characters, Staff, Tags, Traits) with last-updated timestamp or neutral placeholder (never fake fallback numbers).

### 3. Search Screen & Character Trait Explorer (`SearchScreen` / `CharacterSearchScreen`)
- Mode toggle between **Visual Novels** (`VN_SEARCH`) and **Characters & Traits** (`CHARACTER_SEARCH`).
- Debounced search input (`search_query_input`), clear button, filter sheet button (`search_filter_button`) with active-filter badge, sort sheet/controls.
- Latest request wins; in-flight HTTP call is cancelled on new query/filter.
- **VN Search Filters**:
  - Tag autocomplete & multi-select + curated genre presets (`AND` / `OR` toggle)
  - Character Trait autocomplete & multi-select (`AND` / `OR` toggle)
  - Developer/Producer autocomplete & multi-select (`AND` / `OR` toggle)
  - Available Languages multi-select with flag icons
  - Original Language multi-select with flag icons
  - Platforms multi-select with OS icons
  - Development Status multi-select (*Finished*, *In development*, *Cancelled*)
  - Character Role filter (*Protagonist*, *Main character*, *Side character*, *Makes an appearance*) with `AND` / `OR`
  - Character Sex filter (*Male*, *Female*, *Both*, *Sexless*) with `AND` / `OR`
  - Release Window (*All*, *Latest / Released*, *Upcoming / TBA*)
  - Remote Sort (*Search Rank*, *Rating*, *Vote Count*, *Release Date*, *Title*, *ID*; ascending/descending)
- **Character & Trait Explorer**:
  - Curated trait category chips + live trait search autocomplete (`AND` / `OR` toggle)
  - Character role and sex filters (`AND` / `OR` toggles)
  - Explicit Apply/Search or debounced execution so tapping multiple filter chips does not fire redundant overlapping network requests
  - Character grid/list cards with portrait (NSFW blur support), Japanese/original name, role/VN badges
  - Rich Character Bio Dialog: portrait, original name, aliases, birthday, blood type, height/weight/bust-waist-hips/cup size, BBCode description with spoiler toggle, traits grouped by category (clickable to filter), and linked VN appearances (clickable to open VN Detail).

### 4. Collection Screen (`CollectionScreen`)
- Top bar with collection count & sync status subtitle, local search toggle (`collection_search_toggle_button`) & input (`collection_search_input`), local filter/sort sheet button (`collection_filter_button`) with active badge, and sync button (`collection_sync_button`).
- Reorderable scrollable status tabs (`collection_tab_<status>`) with per-status counts and status icons.
- Multi-selection mode: long-press card to select, count header, Select All in tab (`collection_select_all_button`), Batch Edit status/vote (`collection_batch_edit_button`), Batch Delete (`collection_batch_delete_button`) with confirmation.
- Local SQL filtering (query, status, minimum age, available languages, development statuses, original languages, platforms) and local SQL sorting (*Title*, *Added*, *Started*, *Length*, *Rating*, *Released*, *Vote Count*, *My Vote*; ascending/descending).
- Collection Entry Dialog:
  - Status selector (*Playing*, *Finished*, *Stalled*, *Dropped*, *Wishlist*, *Blacklist*)
  - Vote slider/selector (`1.0`–`10.0` / `10`–`100` or unvoted `null`)
  - Started & Finished dates using Material 3 `DatePicker` with Clear button, `finishedDate >= startedDate` validation, and no "Started" prefill for Wishlist/Blacklist (prefill today's date only for a brand-new `PLAYING` entry)
  - Personal notes text field
  - Remove entry button with confirmation dialog (explaining that removing a synced entry also removes it from the remote VNDB list on next sync).

### 5. Visual Novel Detail Screen (`VnDetailScreen`)
- Scoped per navigation entry (`vnId`) with no global detail map.
- Header with cover art (NSFW blur + tap-to-reveal persisted in ViewModel), title, original language flag, release date, length bucket & hours/minutes, rating, vote count, development status, minimum age, and Add/Edit Collection FAB/button (`vn_detail_collection_fab`).
- 3 Tabs:
  1. **Overview & Tags**: Aliases, developers/producers, available languages (with machine-translation indicator), platforms (with OS icons), BBCode description, spoiler level selector (`None`, `Minor`, `Major`), tag chips with rating & click-to-search, screenshots carousel using `thumbnailUrl` and fullscreen screenshot viewer using full `url`.
  2. **Characters, Quotes & Reviews**: Cast cards with spoiler filter & click to open Character Bio Dialog, VN-specific quotes, and Community Reviews scraped from `vndb.org` with reviewer username, rating, vote points, date, comment count, spoiler support, and expandable full review text (`fetchFullReviewText`).
  3. **Releases, Relations & Links**: Related visual novels (sequels, prequels, fandiscs, same-setting, etc., official/unofficial badge, clickable to navigate to related VN detail) and external links.

### 6. Others, Auth/Sync, Random VN & About Screens
- **Others Screen**: Overview card, navigation tiles for *VNDB Account & Sync* (`others_auth_sync_tile`), *Random High-Rated VN* (`others_random_vn_tile`), *Settings & Themes* (`others_settings_tile`), *About* (`others_about_tile`), and *Report a Bug / Feedback* (`others_bug_report_tile`).
- **Auth & Sync Screen**: Personal Access Token input (masked with show/hide toggle), link to VNDB token creation page, permission check (`listread` + `listwrite` required, clear message when missing), live `SyncProgress` stages & counts, honest `SyncReport` summary (complete success, partial failure with failed count & retry, or failure), Sync Now button, and Logout dialog with Keep vs Clear local collection choice.
- **Random High-Rated VN Screen**: Uses `VndbApi.fetchRandomVn` (2-step uniform ID discovery), displays cover, rating, vote count, release date, length, BBCode synopsis, "Roll Another" button, and "View Full Details" button.
- **About Screen**: App icon, version name/code from `AppInfo`, description, credits to upstream `vndb-lite` by Daniel C.J. and `vndb.org` Data License, GitHub repository link, and Apache-2.0 license summary.

---

## Work Order Checklist

- [x] **1. ASSETS MUST BE PROVABLY IN THE APK AND LOAD**
  - [x] 1a. Move `assets` to `app/src/main/assets` (workspace updated + `MANUAL STEPS` shell commands + `README.md` header icon path updated).
  - [x] 1b. `app/build.gradle.kts`: Conditional `sourceSets` adding `../assets` only if `src/main/assets` does not exist.
  - [x] 1c. `.github/workflows/ci.yml`: Post-`assembleDebug` verification step unzipping APK, checking total count against source assets, verifying non-empty `bg`, `flags`, `os`, `status`, `search`, and writing counts to `$GITHUB_STEP_SUMMARY`.
  - [x] 1d. `AssetImage.kt`: Never silent (`AppLog.w` once per failing path), neutral placeholder, size-aware `inSampleSize` decoding, byte-bounded `LruCache` (16 MiB), and single-mechanism `NsfwAsyncImage` blur (`Modifier.blur` on API 31+, single-pass downsample on API < 31).
- [x] **2. WORKFLOWS**
  - [x] 2a. Fix `.github/workflows/release.yml` (map secrets into job-level `env`, test `env.KEYSTORE_BASE64 != ''`, preserve unsigned fallback, keep `ci.yml` green).
- [x] **3. DEFECTS IN THE NEW LAYERS**
  - [x] 3a. `VndbApi.fetchUserList` / `VndbApiImpl.fetchUserList` preserves `PaginatedList.hasMore`; `SyncPlanner` and `SyncEngine` refuse to plan/apply `DELETED_ON_REMOTE` local deletions when the remote list is truncated.
  - [x] 3b. `EncryptedTokenStore.getToken(accountId)` no longer falls back to `KEY_ACTIVE_TOKEN` when the requested account's own key is missing.
  - [x] 3c. `VndbApiImpl` and `JsoupReviewSource` require shared `OkHttpClient` and `RateLimiter` instances constructed once in `AppContainer`.
  - [x] 3d. `SyncEngineTest` expanded to 8 scenarios (partial push failure keeps `DIRTY` + delete queue, truncated list, account switch, 429 backoff, missing `listwrite`, overlapping sync rejected with `ApiError.SyncInProgress`, logout keep, logout clear).
- [x] **4. COMPOSITION ROOT AND REPOSITORIES (additive)**
  - [x] 4a. `VndbApplication` + `di/AppContainer.kt` (`DefaultAppContainer` with lazy singletons sharing one `OkHttpClient` and one `RateLimiter`).
  - [x] 4b. `VnRepository` & `VnRepositoryImpl` (per-section TTL, stale-while-revalidate, parallel home refresh under rate limiter, LRU cap preserving collection rows).
  - [x] 4c. `CollectionRepository` & `CollectionRepositoryImpl` (Room CRUD, SQL filter/sort, single-transaction batch updates, `DIRTY` / `PENDING_DELETE` sync state management, deterministic `lastModifiedAt DESC` order).
  - [x] 4d. `SettingsRepository` & `SettingsRepositoryImpl` (DataStore).
  - [x] 4e. `SyncRepository` & `SyncRepositoryImpl` (`listread` + `listwrite` auth check, `SyncEngine`, logout keep/clear choice).
  - [x] 4f. Repository tests (`VnRepositoryImplTest`, `CollectionRepositoryImplTest`, `SettingsAndSyncRepositoryTest`).
- [ ] **5. REBUILD THE UI AS PER-FEATURE SCREENS (additive, one feature at a time)**
  - [ ] 5a. UI Foundation & Theme Infrastructure:
    - `ui/theme/{ColorTokens.kt, ThemeSpecs.kt, Typography.kt, AppBackground.kt, Theme.kt}` (built from `ThemeId`, luminance-based contrast labels, system bar appearance)
    - `ui/components/AssetPaths.kt` + unit test `AssetPathsTest.kt` verifying every bundled asset exists on disk
    - `ui/format/{UiText.kt, ApiErrorUiText.kt, VnFormatters.kt}` + comprehensive `res/values/strings.xml`
    - `ui/components/{BbCodeParser.kt, BbCodeText.kt, ConfirmDialog.kt, DatePickerField.kt, LoadStateViews.kt, VnGridCard.kt, CollectionEntryDialog.kt}`
    - `ui/navigation/{Routes.kt, AppNavHost.kt}`
  - [ ] 5b. Home Feature (`ui/screens/home/{HomeUiState.kt, HomeViewModel.kt, HomeScreen.kt, HomeSections.kt}`)
  - [ ] 5c. Search & Character Explorer Feature (`ui/screens/search/{SearchUiState.kt, SearchViewModel.kt, SearchScreen.kt, SearchFilterSheet.kt}`, `ui/screens/characters/{CharacterSearchUiState.kt, CharacterSearchViewModel.kt, CharacterSearchScreen.kt, CharacterBioDialog.kt}`)
  - [ ] 5d. Collection Feature (`ui/screens/collection/{CollectionUiState.kt, CollectionViewModel.kt, CollectionScreen.kt, CollectionFilterSheet.kt}`)
  - [ ] 5e. VN Detail Feature (`ui/screens/detail/{VnDetailUiState.kt, VnDetailViewModel.kt, VnDetailScreen.kt, VnDetailTabs.kt}`)
  - [ ] 5f. Others, Settings, Auth/Sync, Random VN & About Features (`ui/screens/others/`, `ui/screens/settings/`, `ui/screens/auth/`, `ui/screens/random/`, `ui/screens/about/`)
- [ ] **6. SWITCH-OVER AND DELETE LEGACY (one reply, only when every feature in item 5 is done)**
  - [ ] 6a. Wire `android:name=".VndbApplication"` in `AndroidManifest.xml` and `AppNavHost` in `MainActivity.kt` with one-time off-main-thread `LegacyPrefsMigrator` progress state.
  - [ ] 6b. Update `res/xml/backup_rules.xml` and `res/xml/data_extraction_rules.xml` to include Room DB and DataStore settings while excluding `vndb_secure_token_prefs`, caches, and legacy prefs.
  - [ ] 6c. Delete legacy files (`MANUAL STEPS` `git rm`):
    - [ ] `app/src/main/java/com/vndblite/dcj/ui/viewmodel/VndbViewModel.kt`
    - [ ] `app/src/main/java/com/vndblite/dcj/data/local/LocalStorageManager.kt`
    - [ ] `app/src/main/java/com/vndblite/dcj/data/remote/VndbApiService.kt`
    - [ ] `app/src/main/java/com/vndblite/dcj/data/model/Models.kt`
    - [ ] `app/src/main/java/com/vndblite/dcj/ui/components/VnItemGridCard.kt`
    - [ ] `app/src/main/java/com/vndblite/dcj/ui/components/CollectionRecordDialog.kt`
    - [ ] `app/src/main/java/com/vndblite/dcj/ui/screens/HomeScreen.kt`
    - [ ] `app/src/main/java/com/vndblite/dcj/ui/screens/SearchScreen.kt`
    - [ ] `app/src/main/java/com/vndblite/dcj/ui/screens/CollectionScreen.kt`
    - [ ] `app/src/main/java/com/vndblite/dcj/ui/screens/VnDetailScreen.kt`
    - [ ] `app/src/main/java/com/vndblite/dcj/ui/screens/OthersAndSettingsScreens.kt`
    - [ ] `app/src/test/java/com/vndblite/dcj/VndbModelsTest.kt`
- [ ] **7. TESTS AND CI**
  - [ ] 7a. ViewModel tests (fake repositories + Turbine: latest-search-wins, error then retry, state restoration)
  - [ ] 7b. Navigation semantics, BBCode parser, theme contrast, and formatter tests
- [ ] **FINAL STEP (after CI build is confirmed green)**
  - [ ] Remove any blocking halts/gates on optional artifacts
  - [ ] Unify app icon references to a single master drawable + `docs/APP_ICON.md`
  - [ ] Sync `README.md` and `CHANGELOG.md`

## Next Action
Start **Item 5a**: Build the UI foundation (`ColorTokens`, `ThemeSpecs`, `Typography`, `AppBackground`, `AssetPaths` + `AssetPathsTest`, `UiText` + `strings.xml`, `BbCodeParser`, `ConfirmDialog`, `DatePickerField`, `LoadStateViews`, `VnGridCard`, `CollectionEntryDialog`, and `Routes`) and **Item 5b** (`HomeScreen` + `HomeViewModel`).

## Notes
- Items 1, 2, 3, and 4 are complete and verified. Legacy code remains untouched at runtime until Item 6 switch-over.
