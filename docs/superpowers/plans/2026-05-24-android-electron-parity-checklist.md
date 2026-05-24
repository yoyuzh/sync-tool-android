# Android Electron Parity Checklist

目标：让 `sync-tool-android` 补齐当前 Electron 桌面端已经验证过的核心能力，同时遵守 Android 仓库的隐私、电量和系统能力边界。

参考能力来源：

- Electron 原生能力设计：`/Users/mac/Documents/sync-tool/sync-tool-cs/docs/superpowers/specs/2026-05-23-electron-native-development-design.md`
- 服务端/桌面协议设计：`/Users/mac/Documents/sync-tool/sync-tool-cs/docs/superpowers/specs/2026-05-23-server-desktop-protocol-integration-design.md`
- 当前 Android 骨架：Room 本地历史、Compose UI、AccessibilityService、InputMethodService、WorkManager、Repository/UseCase 边界。

## Android 等价原则

- 不照搬 Electron 的常驻能力。Electron 的常驻窗口、托盘、全局快捷键、剪贴板轮询，在 Android 上分别映射为前台 Activity、通知入口、IME 面板、Accessibility 事件入口和 WorkManager 同步。
- 不默认后台上传剪贴板。所有上传必须来自明确用户动作，或来自未来有显式开关的自动发布设置。
- 不把网络放进 IME/Accessibility 重逻辑里。IME 和 Accessibility 只读本地缓存、记录轻量事件、调度工作。
- 服务端协议保持一致：HTTP 负责注册、历史、发布、blob；WebSocket 负责在线 session、presence、广播通知。
- 第一版 Android 不做高频后台 WebSocket。只在前台/用户明确连接时保持实时通道；后台用 WorkManager 低频 backfill，未来可接 push 唤醒。

## 已有 Android 基础

- [x] Compose app shell
- [x] Room-backed local history skeleton
- [x] `ClipboardRepository` / use case boundary
- [x] Manual publish use case skeleton
- [x] Accessibility permission onboarding and settings row
- [x] AccessibilityService event router skeleton
- [x] InputMethodService history panel skeleton
- [x] WorkManager sync worker skeleton
- [x] SMS task placeholders only

## P0 - 与 Electron 同步闭环对齐

### 1. Shared protocol model parity

- [ ] Add Android DTOs matching server protocol:
  - `RegisterDeviceRequest`
  - `RegisterDeviceResponse`
  - `HistoryResponse`
  - `PublishRecordRequest`
  - `PublishRecordResponse`
  - `SyncMessage`
  - `ServerMessage`
  - `ClientMessage`
  - `ApiErrorResponse`
- [ ] Add protocol constants matching `packages/shared`:
  - `SYNC_PROTOCOL_VERSION = 1`
  - `API_V1_PREFIX = "/api/v1"`
  - message types: `server.hello`, `presence.snapshot`, `presence.changed`, `record.published`, `record.updated`, `server.error`, `client.ping`, `record.ack`.
- [ ] Align Android `ClipboardRecord` domain model with server record fields:
  - `id`
  - `createdAt`
  - `updatedAt`
  - `sourceDeviceId`
  - `kind`
  - `title`
  - `textPreview`
  - `textContent`
  - `mimeType`
  - `sizeBytes`
  - `storageMode`
  - `publishState`
  - `contentHash`
- [ ] Keep UI-only display fields out of protocol DTOs.
- [ ] Add mapper tests for DTO <-> domain <-> Room entity conversion.

### 2. Device registration and secure settings

- [ ] Add a persistent Android settings store:
  - `serverUrl`
  - `deviceName`
  - `deviceId`
  - `deviceToken`
  - `deviceTokenServerUrl`
  - `manualPublishEnabled`
  - `notificationPreviewEnabled`
  - `maxLocalHistoryItems`
  - `syncWindowDays`
  - `accessibilityEnabledObserved`
  - `excludedApps`
- [ ] Store `deviceToken` outside UI state; prefer encrypted storage if available.
- [ ] Register Android as `deviceType = "android"` with capabilities:
  - `clipboard.read.text`
  - `clipboard.write.text`
  - `history.query`
  - `record.publish`
- [ ] Clear stored token when `serverUrl` changes.
- [ ] On HTTP 401 or WebSocket `server.error/unauthorized`, clear token, register again, and retry once.
- [ ] Add unit tests for stale-token recovery, matching the Electron fix.

### 3. HTTP client parity

- [ ] Add remote API boundary under `data/remote/`.
- [ ] Implement `GET /health`.
- [ ] Implement `POST /api/v1/devices/register`.
- [ ] Implement `GET /api/v1/history?days=1|3|7|15&limit=20&cursor=...`.
- [ ] Implement `POST /api/v1/records/publish` with generated `clientRequestId`.
- [ ] Implement `GET /api/v1/records/:recordId`.
- [ ] Implement blob upload/download endpoints as phase-ready functions, even if UI only supports text first.
- [ ] Normalize API errors into domain errors:
  - unauthorized
  - validation_failed
  - record_not_found
  - blob_too_large
  - conflict
  - internal_error
- [ ] Add API client tests using fake server or mocked engine.

### 4. Local history parity

- [ ] Extend Room entity/schema to store full text content, storage mode, content hash, source device id, created/updated timestamps, and publish state.
- [ ] Add deterministic local id generation equivalent to desktop `local-*` ids or document Android-specific id prefix.
- [ ] Hash normalized text content and dedupe local captures.
- [ ] Preserve full text for copy/IME insertion; only normalize preview/title.
- [ ] Merge remote records into local history by `id`.
- [ ] Preserve local-only records when server sync fails.
- [ ] Trim local history by `maxLocalHistoryItems`.
- [ ] Add DAO/repository tests for merge, dedupe, trim, publish-state transition, and corrupted/partial data handling.

### 5. Manual capture and publish parity

- [ ] Add a "capture current clipboard" use case:
  - read current text from Android clipboard while app is foreground or through a user action
  - ignore empty/whitespace-only text
  - compute content hash
  - save as local record with `publishState = LOCAL`
- [ ] Add a "publish selected record" use case:
  - load local record
  - send HTTP publish request
  - mark local record as `PUBLISHED`
  - keep record local and show error if publish fails
- [ ] Add a "publish current clipboard" use case matching Electron shortcut behavior:
  - capture current clipboard
  - publish it
  - show notification/toast result
- [ ] Keep automatic publish disabled unless a future setting explicitly enables it.
- [ ] Add tests for empty clipboard, duplicate clipboard, publish success, publish conflict, offline publish failure.

### 6. Foreground realtime session parity

- [ ] Add a WebSocket/session client for foreground app use:
  - connect to `/ws?token=...`
  - receive `server.hello`
  - receive `presence.snapshot`
  - receive `presence.changed`
  - receive `record.published`
  - receive `record.updated`
  - send `client.ping`
  - send `record.ack`
- [ ] Merge remote `record.published` into Room.
- [ ] Expose connection state to UI:
  - offline
  - connecting
  - online
  - error
  - online device count
  - last connected time
  - last error
- [ ] Reconnect only while app is foreground or while a user-enabled foreground service exists.
- [ ] On reconnect, run HTTP history backfill.
- [ ] Add stale-token recovery for WebSocket unauthorized.
- [ ] Add unit tests for message parsing, record merge, presence count, ping/pong, unauthorized recovery.

### 7. WorkManager backfill parity

- [ ] Implement `ClipboardRepository.sync()` as retained history backfill.
- [ ] Use WorkManager for low-frequency sync:
  - network-connected constraint
  - exponential backoff
  - no high-frequency polling
  - respect battery/background restrictions
- [ ] Schedule sync after:
  - app launch
  - server URL/token changes
  - foreground session reconnect
  - local publish success
  - notification/user manual refresh
- [ ] Add tests for scheduling policy and retry behavior.

## P1 - Android-native surfaces matching Electron UX

### 8. Main UI parity

- [ ] Add history list states matching desktop:
  - all
  - local
  - synced
  - files
  - images
  - failed
- [ ] Add search over title, preview, and source device.
- [ ] Add time range selector: 1, 3, 7, 15 days.
- [ ] Add connection header:
  - connection label
  - online device count
  - reconnect action
- [ ] Add settings screen fields:
  - server URL
  - device name
  - manual publish switch
  - notification previews
  - max local history
  - accessibility status
  - IME status
  - excluded apps
- [ ] Rename actions precisely:
  - "捕获" only saves local clipboard.
  - "发布" sends to server.
  - "插入" writes through IME.
- [ ] Add UI tests for labels and state transitions.

### 9. Notification parity

- [ ] Show native notification for remote records when allowed.
- [ ] Do not show sensitive content in notification body unless `notificationPreviewEnabled` is true.
- [ ] Notification tap opens the app and selects related record when possible.
- [ ] Show status notifications/toasts for:
  - publish success
  - publish failure
  - reconnect failure
  - stale-token recovery failure
- [ ] Add notification channel setup and tests where practical.

### 10. IME parity for "copy/paste latest online"

- [ ] IME panel reads recent local Room history.
- [ ] User can insert selected text record into current input connection.
- [ ] Support latest synced text quick insert.
- [ ] Do not insert into password fields or unsupported input types.
- [ ] Handle missing/invalid input connection gracefully.
- [ ] Add tests around `InputConnectionInserter`.

### 11. Accessibility parity for shortcut-like behavior

- [ ] Use Accessibility only as an event/context router.
- [ ] Track focused package/class/input type.
- [ ] Maintain excluded-app denylist.
- [ ] Do not read password fields.
- [ ] Do not harvest text from focused controls.
- [ ] Use events to:
  - update context for IME/panel behavior
  - optionally prompt user to open ClipLink controls
  - schedule conservative sync
- [ ] Add tests for event routing, denied app filtering, password-field filtering.

### 12. Android "shortcut" equivalents

- [ ] Add launcher shortcut or quick settings tile for opening ClipLink.
- [ ] Add notification action for "发布当前剪贴板".
- [ ] Add IME toolbar action for "插入最近线上内容".
- [ ] Consider Android App Shortcuts for:
  - Open history
  - Capture clipboard
  - Publish clipboard
- [ ] Do not rely on unavailable global keyboard shortcuts as a first-version requirement.

## P2 - Binary/blob and richer cross-device support

### 13. Image and file records

- [ ] Add Android DTO/domain support for image/document metadata.
- [ ] Add Android content URI handling for file/blob publish.
- [ ] Enforce server `INLINE_FILE_MAX_BYTES`.
- [ ] Upload source files via `POST /api/v1/records/:recordId/blob`.
- [ ] Download blob via `GET /api/v1/records/:recordId/blob`.
- [ ] Store downloaded files through scoped storage-safe APIs.
- [ ] Add tests for too-large blob, missing record, MIME type, and storage failure.

### 14. Conflict and idempotency handling

- [ ] Persist client request ids for in-flight publish operations.
- [ ] Retry publish with the same `clientRequestId`.
- [ ] Treat server replay success as success.
- [ ] Surface conflict errors as failed state without deleting local record.
- [ ] Add tests for duplicate publish, conflict, retry after process restart.

### 15. Offline and recovery UX

- [ ] Keep local history visible when server is offline.
- [ ] Queue failed publish attempts as local failed/pending state only after user action.
- [ ] Provide manual retry.
- [ ] Run history backfill after reconnect.
- [ ] Do not block IME insertion when server is offline.
- [ ] Add tests for offline app start, reconnect, backfill merge, and failed publish retry.

## Verification Checklist

Run before marking Android parity complete:

- [ ] `./gradlew :app:testDebugUnitTest`
- [ ] `./gradlew :app:lintDebug`
- [ ] `./gradlew :app:assembleDebug`
- [ ] `./gradlew :app:connectedDebugAndroidTest` on emulator/device when instrumentation is added.
- [ ] Start local server from `sync-tool-cs`.
- [ ] Register Android device as `deviceType = android`.
- [ ] Register Electron desktop device as `deviceType = desktop`.
- [ ] Publish text from Android; verify Electron receives `record.published`.
- [ ] Publish text from Electron; verify Android foreground session receives/merges it.
- [ ] Kill/restart Android app; verify retained history backfill works.
- [ ] Change server storage/token; verify stale token is cleared and Android re-registers.
- [ ] Disable network; verify local history/IME still works and publish failure is visible.
- [ ] Enable Accessibility; verify settings row reflects system state.
- [ ] Use IME panel to insert a synced text record into a normal field.
- [ ] Verify no insertion/read behavior in password fields.

## Suggested Implementation Order

1. Protocol DTOs, settings store, and remote API client.
2. Room/domain parity with full record fields.
3. Register/auth/history/publish HTTP flow.
4. Manual capture and publish use cases.
5. Foreground WebSocket session and presence/record merge.
6. WorkManager retained-history backfill.
7. Compose UI parity for history, filters, settings, connection status.
8. Notification actions and IME insertion polish.
9. Accessibility context routing and exclusion settings.
10. Blob/image/document support.

## Definition Of Done

- Android can manually capture local text, store it locally, publish it to the server, and keep it visible if publish fails.
- Android can register with the same server protocol, authenticate, query retained history, and recover from stale tokens.
- Android can receive a foreground realtime `record.published` event from Electron and merge it into Room.
- Electron can receive Android-published records through the existing server broadcast path.
- Android background work uses WorkManager, not a constant background WebSocket.
- Sensitive clipboard content is never uploaded silently and is not shown in notifications unless the user enables previews.
