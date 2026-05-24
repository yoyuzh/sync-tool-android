package com.yoyuzh.cliplink.data.remote.api

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Failure(val error: ApiError) : ApiResult<Nothing>()

    enum class ApiError {
        UNAUTHORIZED,
        VALIDATION_FAILED,
        NOT_FOUND,
        CONFLICT,
        NETWORK_ERROR,
        INTERNAL_ERROR,
        UNKNOWN
    }

    val isSuccess get() = this is Success
    fun getOrNull() = (this as? Success)?.data
    fun errorOrNull() = (this as? Failure)?.error
}
