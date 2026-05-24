# sync-tool Android

Native Android client for the sync-tool cross-device clipboard product.

## Scope

- Kotlin Android app with Jetpack Compose UI.
- Room-backed local clipboard history.
- Manual clipboard publish boundary.
- AccessibilityService event router for low-power realtime context.
- InputMethodService history panel for inserting saved clipboard text.
- WorkManager boundary for conservative background sync.
- SMS architecture placeholders only; the first version does not send SMS.

## Commands

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
```

Release signing is intentionally configured outside the repository. Set these
environment variables, or matching Gradle properties, before building a signed
release:

```bash
CLIPLINK_RELEASE_STORE_FILE=/absolute/path/to/release.keystore
CLIPLINK_RELEASE_STORE_PASSWORD=...
CLIPLINK_RELEASE_KEY_ALIAS=...
CLIPLINK_RELEASE_KEY_PASSWORD=...
./gradlew :app:assembleRelease
```

Release builds fail without those values so an unsigned APK is not mistaken for
a publishable artifact. For local-only unsigned verification, opt in explicitly:

```bash
CLIPLINK_ALLOW_UNSIGNED_RELEASE=true ./gradlew :app:assembleRelease
```

Instrumentation tests require a connected device or emulator:

```bash
./gradlew :app:connectedDebugAndroidTest
```
