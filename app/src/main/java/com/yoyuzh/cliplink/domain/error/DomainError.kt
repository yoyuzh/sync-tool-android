package com.yoyuzh.cliplink.domain.error

sealed class DomainError : Exception() {
    object Unauthorized : DomainError()
    object NetworkError : DomainError()
    object RecordNotFound : DomainError()
    object Conflict : DomainError()
    object ValidationFailed : DomainError()
    object StorageLimitExceeded : DomainError()
    data class ServerError(val code: String, override val message: String) : DomainError()
    data class Unknown(override val cause: Throwable?) : DomainError()
}
