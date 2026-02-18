package com.agc.bwitch.domain.model

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Failure(val error: NetworkError) : ApiResult<Nothing>()
}

sealed class NetworkError {
    data object NetworkUnavailable : NetworkError()
    data object Unauthorized : NetworkError()
    data object Forbidden : NetworkError()
    data object NotFound : NetworkError()
    data class Server(val code: Int? = null) : NetworkError()
    data object Unknown : NetworkError()
}
