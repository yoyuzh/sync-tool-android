package com.yoyuzh.cliplink.data.remote.ws

import com.yoyuzh.cliplink.data.remote.dto.MessageType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class SyncWebSocketProtocolTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `presence snapshot derives online count from devices payload`() {
        val payload = buildJsonObject {
            put(
                "devices",
                kotlinx.serialization.json.buildJsonArray {
                    add(buildJsonObject { put("online", true) })
                    add(buildJsonObject { put("online", false) })
                    add(buildJsonObject { put("online", true) })
                }
            )
        }

        assertEquals(2, SyncWebSocketClient.onlineDeviceCountFromPayload(payload, json))
    }

    @Test
    fun `presence changed adjusts current online count instead of replacing it`() {
        val onlinePayload = buildJsonObject {
            put("online", true)
        }
        val offlinePayload = buildJsonObject {
            put("online", false)
        }

        assertEquals(
            3,
            SyncWebSocketClient.nextOnlineDeviceCount(2, MessageType.PRESENCE_CHANGED, onlinePayload, json)
        )
        assertEquals(
            1,
            SyncWebSocketClient.nextOnlineDeviceCount(2, MessageType.PRESENCE_CHANGED, offlinePayload, json)
        )
        assertEquals(
            0,
            SyncWebSocketClient.nextOnlineDeviceCount(0, MessageType.PRESENCE_CHANGED, offlinePayload, json)
        )
    }

    @Test
    fun `record published unwraps server record payload envelope`() {
        val payload = buildJsonObject {
            putJsonObject("record") {
                put("id", "record-1")
                put("createdAt", "2026-05-24T00:00:00Z")
                put("updatedAt", "2026-05-24T00:00:01Z")
                put("sourceDeviceId", "desktop-1")
                put("kind", "text")
                put("title", "Hello")
                put("textPreview", "Hello")
                put("textContent", "Hello")
                put("mimeType", "text/plain")
                put("sizeBytes", 5)
                put("storageMode", "metadata_only")
                put("publishState", "published")
                put("contentHash", "hash")
            }
        }

        assertEquals("record-1", SyncWebSocketClient.recordFromPayload(payload, json)?.id)
    }

    @Test
    fun `server error unauthorized maps to authentication error state`() {
        val payload = buildJsonObject {
            put("code", "unauthorized")
            put("message", "Unauthorized")
            put("retryable", false)
        }

        assertEquals(
            SessionState.Error("Unauthorized"),
            SyncWebSocketClient.errorStateFromPayload(MessageType.SERVER_ERROR, payload, json)
        )
    }

    @Test
    fun `client ping includes required client time payload`() {
        val ping = SyncWebSocketClient.buildClientPing(json, Instant.parse("2026-05-24T00:00:00Z"))
        val root = json.parseToJsonElement(ping).jsonObject

        assertEquals(MessageType.CLIENT_PING, root["type"]!!.jsonPrimitive.content)
        assertEquals(
            "2026-05-24T00:00:00Z",
            root["payload"]!!.jsonObject["clientTime"]!!.jsonPrimitive.content
        )
    }
}
