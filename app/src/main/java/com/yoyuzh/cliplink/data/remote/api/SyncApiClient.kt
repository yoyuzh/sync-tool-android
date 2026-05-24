package com.yoyuzh.cliplink.data.remote.api

import com.yoyuzh.cliplink.data.remote.dto.ApiErrorCode
import com.yoyuzh.cliplink.data.remote.dto.ApiErrorResponse
import com.yoyuzh.cliplink.data.remote.dto.ClipboardRecordDto
import com.yoyuzh.cliplink.data.remote.dto.HistoryResponse
import com.yoyuzh.cliplink.data.remote.dto.PublishRecordRequest
import com.yoyuzh.cliplink.data.remote.dto.PublishRecordResponse
import com.yoyuzh.cliplink.data.remote.dto.RecordResponse
import com.yoyuzh.cliplink.data.remote.dto.RegisterDeviceRequest
import com.yoyuzh.cliplink.data.remote.dto.RegisterDeviceResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

@Singleton
class SyncApiClient @Inject constructor(
    private val httpClient: OkHttpClient,
    private val json: Json
) {
    /** POST /api/v1/devices/register — does not require auth */
    suspend fun registerDevice(
        serverUrl: String,
        request: RegisterDeviceRequest
    ): ApiResult<RegisterDeviceResponse> =
        safeCall {
            val body = json.encodeToString(request).toRequestBody(JSON_MEDIA_TYPE)
            val req = Request.Builder()
                .url("${normalizedBaseUrl(serverUrl)}/api/v1/devices/register")
                .post(body)
                .build()
            httpClient.newCall(req).execute().use { response ->
                parseResponse<RegisterDeviceResponse>(response)
            }
        }

    /** GET /api/v1/history */
    suspend fun fetchHistory(
        serverUrl: String,
        token: String,
        days: Int,
        limit: Int,
        cursor: String? = null
    ): ApiResult<HistoryResponse> =
        safeCall {
            val url = "${normalizedBaseUrl(serverUrl)}/api/v1/history".toHttpUrl().newBuilder()
                .addQueryParameter("days", days.toString())
                .addQueryParameter("limit", limit.toString())
                .apply {
                    if (!cursor.isNullOrBlank()) addQueryParameter("cursor", cursor)
                }
                .build()
            val req = authenticatedRequest(token).url(url).get().build()
            httpClient.newCall(req).execute().use { response ->
                parseResponse<HistoryResponse>(response)
            }
        }

    /** POST /api/v1/records/publish */
    suspend fun publishRecord(
        serverUrl: String,
        token: String,
        request: PublishRecordRequest
    ): ApiResult<PublishRecordResponse> =
        safeCall {
            val body = json.encodeToString(request).toRequestBody(JSON_MEDIA_TYPE)
            val req = authenticatedRequest(token)
                .url("${normalizedBaseUrl(serverUrl)}/api/v1/records/publish")
                .post(body)
                .build()
            httpClient.newCall(req).execute().use { response ->
                parseResponse<PublishRecordResponse>(response)
            }
        }

    /** GET /api/v1/records/:recordId */
    suspend fun fetchRecord(
        serverUrl: String,
        token: String,
        recordId: String
    ): ApiResult<ClipboardRecordDto> =
        safeCall {
            val req = authenticatedRequest(token)
                .url("${normalizedBaseUrl(serverUrl)}/api/v1/records/$recordId")
                .get()
                .build()
            httpClient.newCall(req).execute().use { response ->
                when (val result = parseResponse<RecordResponse>(response)) {
                    is ApiResult.Success -> ApiResult.Success(result.data.record)
                    is ApiResult.Failure -> result
                }
            }
        }

    /** GET /health */
    suspend fun checkHealth(serverUrl: String): ApiResult<Unit> =
        safeCall {
            val req = Request.Builder().url("${normalizedBaseUrl(serverUrl)}/health").get().build()
            httpClient.newCall(req).execute().use { response ->
                if (response.isSuccessful) ApiResult.Success(Unit)
                else ApiResult.Failure(ApiResult.ApiError.INTERNAL_ERROR)
            }
        }

    // -----------------------------------------------------------------------

    private fun authenticatedRequest(token: String) = Request.Builder()
        .header("Authorization", "Bearer $token")

    private fun normalizedBaseUrl(serverUrl: String) = serverUrl.trimEnd('/')

    private suspend fun <T> safeCall(block: suspend () -> ApiResult<T>): ApiResult<T> {
        return try {
            withContext(Dispatchers.IO) { block() }
        } catch (e: Exception) {
            ApiResult.Failure(ApiResult.ApiError.NETWORK_ERROR)
        }
    }

    private inline fun <reified T> parseResponse(response: Response): ApiResult<T> {
        val body = response.body?.string() ?: ""
        return when {
            response.isSuccessful -> {
                try {
                    ApiResult.Success(json.decodeFromString<T>(body))
                } catch (e: Exception) {
                    ApiResult.Failure(ApiResult.ApiError.UNKNOWN)
                }
            }
            response.code == 401 -> ApiResult.Failure(ApiResult.ApiError.UNAUTHORIZED)
            response.code == 404 -> ApiResult.Failure(ApiResult.ApiError.NOT_FOUND)
            response.code == 409 -> ApiResult.Failure(ApiResult.ApiError.CONFLICT)
            response.code == 422 -> ApiResult.Failure(ApiResult.ApiError.VALIDATION_FAILED)
            else -> {
                try {
                    val err = json.decodeFromString<ApiErrorResponse>(body)
                    when (err.error.code) {
                        ApiErrorCode.UNAUTHORIZED -> ApiResult.Failure(ApiResult.ApiError.UNAUTHORIZED)
                        ApiErrorCode.CONFLICT -> ApiResult.Failure(ApiResult.ApiError.CONFLICT)
                        ApiErrorCode.RECORD_NOT_FOUND -> ApiResult.Failure(ApiResult.ApiError.NOT_FOUND)
                        else -> ApiResult.Failure(ApiResult.ApiError.INTERNAL_ERROR)
                    }
                } catch (e: Exception) {
                    ApiResult.Failure(ApiResult.ApiError.UNKNOWN)
                }
            }
        }
    }
}
