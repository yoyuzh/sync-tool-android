package com.yoyuzh.cliplink.data.remote.ws

import com.yoyuzh.cliplink.data.local.dao.ClipboardRecordDao
import com.yoyuzh.cliplink.data.local.entity.ClipboardRecordEntity
import com.yoyuzh.cliplink.data.remote.dto.ClipboardRecordDto
import com.yoyuzh.cliplink.data.remote.dto.MessageType
import com.yoyuzh.cliplink.data.remote.dto.RecordMessagePayload
import com.yoyuzh.cliplink.data.remote.dto.SYNC_PROTOCOL_VERSION
import com.yoyuzh.cliplink.data.remote.dto.ServerErrorPayload
import com.yoyuzh.cliplink.data.remote.dto.SyncMessage
import com.yoyuzh.cliplink.data.remote.dto.parseProtocolMillis
import com.yoyuzh.cliplink.data.remote.dto.toDomainKind
import com.yoyuzh.cliplink.data.remote.dto.toDomainPublishState
import com.yoyuzh.cliplink.data.remote.dto.toDomainStorageMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.booleanOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncWebSocketClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val dao: ClipboardRecordDao,
    private val json: Json
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Disconnected)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private var webSocket: WebSocket? = null
    private var pingJob: Job? = null
    private var currentToken: String = ""
    private var currentServerUrl: String = ""

    fun connect(serverUrl: String, token: String) {
        if (_sessionState.value is SessionState.Online || _sessionState.value is SessionState.Connecting) {
            return
        }
        if (serverUrl.isBlank() || token.isBlank()) {
            _sessionState.value = SessionState.Error("Device token is not available")
            return
        }
        currentToken = token
        currentServerUrl = serverUrl.trimEnd('/')

        _sessionState.value = SessionState.Connecting

        val wsUrl = currentServerUrl
            .replace("http://", "ws://")
            .replace("https://", "wss://")
            .plus("/ws?token=$token")

        val request = Request.Builder().url(wsUrl).build()
        webSocket = okHttpClient.newWebSocket(request, createListener())
    }

    fun disconnect() {
        pingJob?.cancel()
        webSocket?.close(1000, "App going to background")
        webSocket = null
        _sessionState.value = SessionState.Disconnected
    }

    private fun createListener() = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            _sessionState.value = SessionState.Online()
            startPingLoop(webSocket)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            handleMessage(text)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            pingJob?.cancel()
            _sessionState.value = SessionState.Error(t.message ?: "Connection failed")
            scheduleReconnect()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            pingJob?.cancel()
            _sessionState.value = SessionState.Disconnected
        }
    }

    private fun handleMessage(text: String) {
        val msg = runCatching { json.decodeFromString<SyncMessage>(text) }.getOrNull() ?: return

        if (msg.protocolVersion != SYNC_PROTOCOL_VERSION) return

        when (msg.type) {
            MessageType.SERVER_HELLO -> {
                // Already handled via onOpen
            }

            MessageType.PRESENCE_SNAPSHOT, MessageType.PRESENCE_CHANGED -> {
                val current = _sessionState.value
                if (current is SessionState.Online) {
                    _sessionState.value = current.copy(
                        onlineDeviceCount = nextOnlineDeviceCount(
                            current.onlineDeviceCount,
                            msg.type,
                            msg.payload,
                            json
                        )
                    )
                }
            }

            MessageType.RECORD_PUBLISHED, MessageType.RECORD_UPDATED -> {
                // Merge published record into Room
                scope.launch {
                    val dto = recordFromPayload(msg.payload, json) ?: return@launch

                    val entity = dtoToEntity(dto)
                    dao.upsert(entity)
                }
            }

            MessageType.SERVER_ERROR -> {
                _sessionState.value = errorStateFromPayload(msg.type, msg.payload, json)
            }
        }
    }

    private fun startPingLoop(ws: WebSocket) {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (true) {
                delay(30_000L)
                val ping = buildClientPing(json)
                ws.send(ping)
            }
        }
    }

    private fun scheduleReconnect() {
        scope.launch {
            delay(5_000L)
            val current = _sessionState.value
            if (current is SessionState.Error &&
                current.message != "Unauthorized" &&
                currentToken.isNotBlank()
            ) {
                connect(currentServerUrl, currentToken)
            }
        }
    }

    private fun dtoToEntity(dto: ClipboardRecordDto) = ClipboardRecordEntity(
        id = dto.id,
        createdAtMillis = parseProtocolMillis(dto.createdAt),
        updatedAtMillis = parseProtocolMillis(dto.updatedAt),
        sourceDeviceId = dto.sourceDeviceId,
        kind = dto.kind.toDomainKind().name,
        title = dto.title,
        textPreview = dto.textPreview,
        textContent = dto.textContent,
        mimeType = dto.mimeType,
        sizeBytes = dto.sizeBytes,
        storageMode = dto.toDomainStorageMode().name,
        publishState = dto.toDomainPublishState().name,
        contentHash = dto.contentHash
    )

    companion object {
        fun onlineDeviceCountFromPayload(payload: JsonElement?, json: Json): Int {
            if (payload == null) return 0
            return runCatching {
                val root = payload.jsonObject
                root["devices"]?.jsonArray?.count {
                    it.jsonObject["online"]?.jsonPrimitive?.booleanOrNull == true
                } ?: if (root["online"]?.jsonPrimitive?.booleanOrNull == true) 1 else 0
            }.getOrElse { 0 }
        }

        fun nextOnlineDeviceCount(
            currentCount: Int,
            messageType: String,
            payload: JsonElement?,
            json: Json
        ): Int {
            if (messageType == MessageType.PRESENCE_SNAPSHOT) {
                return onlineDeviceCountFromPayload(payload, json)
            }
            if (messageType != MessageType.PRESENCE_CHANGED || payload == null) {
                return currentCount
            }

            return runCatching {
                val isOnline = payload.jsonObject["online"]?.jsonPrimitive?.booleanOrNull
                when (isOnline) {
                    true -> currentCount + 1
                    false -> (currentCount - 1).coerceAtLeast(0)
                    null -> currentCount
                }
            }.getOrElse { currentCount }
        }

        fun recordFromPayload(payload: JsonElement?, json: Json): ClipboardRecordDto? {
            if (payload == null) return null
            return runCatching {
                json.decodeFromJsonElement<RecordMessagePayload>(payload).record
            }.getOrNull()
        }

        fun errorStateFromPayload(type: String, payload: JsonElement?, json: Json): SessionState.Error {
            if (type != MessageType.SERVER_ERROR || payload == null) {
                return SessionState.Error("Connection failed")
            }
            val error = runCatching {
                json.decodeFromJsonElement<ServerErrorPayload>(payload)
            }.getOrNull()
            return SessionState.Error(error?.message ?: "Connection failed")
        }

        fun buildClientPing(json: Json, now: Instant = Instant.now()): String {
            val clientTime = now.toString()
            return json.encodeToString(
                SyncMessage.serializer(),
                SyncMessage(
                    protocolVersion = SYNC_PROTOCOL_VERSION,
                    type = MessageType.CLIENT_PING,
                    messageId = UUID.randomUUID().toString(),
                    sentAt = clientTime,
                    payload = buildJsonObject {
                        put("clientTime", clientTime)
                    }
                )
            )
        }
    }
}
