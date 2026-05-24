package com.yoyuzh.cliplink.data.remote.api

import com.yoyuzh.cliplink.data.remote.dto.RegisterDeviceRequest
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncApiClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: SyncApiClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = SyncApiClient(
            httpClient = OkHttpClient(),
            json = Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `fetch history uses the explicit token passed to the call`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"records":[],"serverTime":"2026-05-24T00:00:00Z"}""")
        )

        val result = client.fetchHistory(
            serverUrl = server.url("/").toString(),
            token = "persisted-token",
            days = 7,
            limit = 20
        )

        assertTrue(result is ApiResult.Success)
        assertEquals("Bearer persisted-token", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `fetch record unwraps the server record response envelope`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "record": {
                        "id": "record-1",
                        "createdAt": "2026-05-24T00:00:00Z",
                        "updatedAt": "2026-05-24T00:00:01Z",
                        "sourceDeviceId": "desktop-1",
                        "kind": "text",
                        "title": "Hello",
                        "textPreview": "Hello",
                        "textContent": "Hello",
                        "mimeType": "text/plain",
                        "sizeBytes": 5,
                        "storageMode": "metadata_only",
                        "publishState": "published",
                        "contentHash": "hash"
                      }
                    }
                    """.trimIndent()
                )
        )

        val result = client.fetchRecord(
            serverUrl = server.url("/").toString(),
            token = "persisted-token",
            recordId = "record-1"
        )

        assertEquals("record-1", result.getOrNull()?.id)
    }

    @Test
    fun `register device does not attach stale authorization header`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "device": {
                        "deviceId": "device-1",
                        "deviceName": "Phone",
                        "deviceType": "android",
                        "capabilities": ["clipboard.read.text"],
                        "online": false,
                        "lastSeenAt": "2026-05-24T00:00:00Z"
                      },
                      "token": "new-token",
                      "protocolVersion": 1
                    }
                    """.trimIndent()
                )
        )

        val result = client.registerDevice(
            serverUrl = server.url("/").toString(),
            request = RegisterDeviceRequest(deviceName = "Phone")
        )

        assertEquals("new-token", result.getOrNull()?.token)
        val request = server.takeRequest()
        assertEquals(null, request.getHeader("Authorization"))
        val body = request.body.readUtf8()
        assertTrue(body.contains(""""deviceType":"android""""))
        assertTrue(body.contains(""""capabilities":["""))
    }
}
