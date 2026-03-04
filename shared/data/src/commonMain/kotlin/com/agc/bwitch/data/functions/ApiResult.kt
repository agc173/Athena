package com.agc.bwitch.data.functions

sealed class ApiResult<out T> {
    data class Ok<T>(val value: T) : ApiResult<T>()
    data class Err(val error: ApiError) : ApiResult<Nothing>()
}

sealed class ApiError(open val message: String?) {
    data class Unauthenticated(override val message: String? = null) : ApiError(message)
    data class PermissionDenied(override val message: String? = null) : ApiError(message)
    data class ResourceExhausted(override val message: String? = null) : ApiError(message)
    data class FailedPrecondition(override val message: String? = null) : ApiError(message)
    data class InvalidArgument(override val message: String? = null) : ApiError(message)
    data class Internal(override val message: String? = null) : ApiError(message)
    data class Unknown(override val message: String? = null) : ApiError(message)
}
