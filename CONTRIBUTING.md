# Contributing to VNDB Lite-Kotlin

Thank you for your interest in contributing to **VNDB Lite-Kotlin**! This document outlines the development setup, coding standards, and pull request workflow.

## Prerequisites

- **JDK 21** (e.g., Eclipse Temurin 21)
- **Android SDK** with `compileSdk = 36` (`minSdk = 24`, `targetSdk = 36`)
- **Android Studio** supporting Android Gradle Plugin (AGP) `9.1.1`

## Building & Testing

Use the Gradle wrapper from the repository root:

```bash
# Assemble debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest

# Run Android Lint
./gradlew lint
```

## Coding Standards

1. **Kotlin Official Style**: Follow the [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) and `.editorconfig`.
2. **Imports & Null Safety**:
   - No wildcard imports (`import foo.*`) and no unused imports.
   - Do not use the non-null assertion operator (`!!`); use safe calls (`?.`), `?:`, or structured smart casts.
3. **Architecture & Size Limits**:
   - Follow Clean Architecture / MVVM (`domain`, `data`, `sync`, `ui`).
   - Aim for functions `<= 60` lines, composables `<= 80` lines, files `<= 400` lines, and `<= 8` parameters (group into state objects or sealed action types).
   - Separate stateful `XScreen` composables from stateless `XContent` composables and include `@Preview` functions.
4. **Coroutines & Error Handling**:
   - Never swallow `CancellationException`—always rethrow it.
   - Inject dispatchers (`DispatcherProvider`) and `Clock` so business logic is deterministically testable.
   - Log handled errors via `AppLog` and surface typed errors (`ApiError`, `UiText`). Never log or display the user's VNDB API token or raw HTTP response bodies.
5. **Strings & Accessibility**:
   - Place all user-visible text in `app/src/main/res/values/strings.xml` (use `UiText` outside composables).
   - Provide meaningful `contentDescription`s, semantic roles, and `>= 48.dp` touch targets. Keep existing `Modifier.testTag(...)` identifiers intact.

## Commit Messages & Pull Requests

- Follow [Conventional Commits](https://www.conventionalcommits.org/) (e.g., `feat(search): ...`, `fix(sync): ...`, `chore(repo): ...`, `refactor(ui): ...`, `test(api): ...`).
- Ensure `./gradlew testDebugUnitTest lint assembleDebug` passes before opening a Pull Request.
