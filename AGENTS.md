# AGENTS.md

This repository is `sync-tool-android`, the native Android client for the sync-tool cross-device clipboard product.

## Repository Role

This is an independent Android repository.

- Local path: `/Users/mac/Documents/sync-tool/sync-tool-android`
- Remote: `https://github.com/yoyuzh/sync-tool-android.git`
- Parent folder: `/Users/mac/Documents/sync-tool`

The server, desktop client, shared TypeScript package, and frontend prototypes live in a separate repository:

- Local path: `/Users/mac/Documents/sync-tool/sync-tool-cs`
- Remote: `https://github.com/yoyuzh/sync-tool.git`

Do not scaffold Android production code inside `sync-tool-cs`.
Do not copy server or desktop implementation files into this repository unless the user explicitly asks for a migration.

## Product Scope

The Android app is a native Kotlin client focused on low-power realtime clipboard assistance.

Required first-version capabilities:

- Main Android app shell
- Local clipboard history list
- Manual clipboard publish flow
- Accessibility permission onboarding
- Core `AccessibilityService` event router
- `InputMethodService` panel that reads local history and inserts selected records
- `WorkManager` low-frequency sync interface and scheduling
- SMS task architecture placeholders only

Not in first version:

- Electron, React Native, or Flutter
- Always-on Android WebSocket
- High-frequency background polling
- Real SMS sending
- Accessibility-driven content harvesting
- Background upload of all clipboard content

## Architecture Rules

- Use Kotlin for all production code.
- Use Jetpack Compose for app UI.
- Use `AccessibilityService` as the required realtime system-event entry.
- Use `InputMethodService` as the user text insertion surface.
- Use `WorkManager` for low-frequency background sync and retry.
- Use Room for local history persistence.
- Use a repository/use-case boundary so UI, IME, Accessibility, and workers depend on domain interfaces rather than storage or network details.
- Keep IME and Accessibility lightweight: read local cache, route events, and schedule work; do not run heavy networking inside those services.

## Privacy And Power Rules

- Clipboard data is uploaded only through explicit user actions or clearly enabled user settings.
- Do not silently read and upload all user content through Accessibility.
- Do not read password fields.
- Provide visible settings switches for sensitive capabilities.
- Support an app exclusion/denylist for Accessibility features.
- SMS behavior requires explicit user authorization before real sending is implemented.
- Prefer event-driven triggers, local cache reads, WorkManager batching, and future push wakeups over constant background connections.

## Expected Commands

After the Android project is scaffolded, prefer:

```bash
./gradlew test
./gradlew lint
./gradlew assembleDebug
```

Use Gradle Wrapper from this repository. Do not rely on a globally installed `gradle` command.

## Planning References

Implementation plans live in:

```text
docs/superpowers/plans/
```

Architecture specs live in:

```text
docs/superpowers/specs/
```

