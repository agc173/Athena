package com.agc.bwitch.data.functions

import com.agc.bwitch.data.connectivity.ConnectivityChecker
import com.agc.bwitch.domain.shared.ApiError
import com.agc.bwitch.domain.shared.ApiResult
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.functions.FirebaseFunctionsException
import dev.gitlive.firebase.functions.FunctionsExceptionCode
import dev.gitlive.firebase.functions.code
import dev.gitlive.firebase.functions.details
import dev.gitlive.firebase.functions.functions
import kotlinx.serialization.KSerializer

class GitLiveFunctionsClient(
    private val connectivityChecker: ConnectivityChecker,
    region: String = DEFAULT_REGION
) : FunctionsClient {

    private val functions = Firebase.functions(region)

    override suspend fun <Req : Any, Res : Any> call(
        name: String,
        data: Req,
        requestSerializer: KSerializer<Req>,
        responseSerializer: KSerializer<Res>,
    ): ApiResult<Res> {
        if (!connectivityChecker.hasUsableConnection()) {
            println("[GitLiveFunctionsClient] callable=$name skipped=no_usable_connection")
            return ApiResult.Err(ApiError.Network())
        }

        return try {
            val result = functions
                .httpsCallable(name)
                .invoke(requestSerializer, data)

            val payload = result.data(responseSerializer)
            println("[GitLiveFunctionsClient] callable=$name success")

            ApiResult.Ok(payload)
        } catch (e: FirebaseFunctionsException) {
            val mapped = e.toApiError()
            println(
                "[GitLiveFunctionsClient] callable=$name code=${e.code} mapped=$mapped message=${e.message} details=${e.details}"
            )
            ApiResult.Err(mapped)
        } catch (e: Throwable) {
            println("[GitLiveFunctionsClient] callable=$name Throwable=$e message=${e.message}")
            ApiResult.Err(e.toNetworkAwareApiError())
        }
    }

    private fun FirebaseFunctionsException.toApiError(): ApiError {
        val exceptionCode = code
        val haystack = searchableText(details)

        return when (exceptionCode) {
            FunctionsExceptionCode.UNAUTHENTICATED -> ApiError.Unauthenticated(message)
            FunctionsExceptionCode.PERMISSION_DENIED -> ApiError.PermissionDenied(message)
            FunctionsExceptionCode.RESOURCE_EXHAUSTED -> ApiError.ResourceExhausted(message)
            FunctionsExceptionCode.FAILED_PRECONDITION -> ApiError.FailedPrecondition(message)
            FunctionsExceptionCode.INVALID_ARGUMENT -> ApiError.InvalidArgument(message)
            FunctionsExceptionCode.INTERNAL -> ApiError.Internal(message)
            FunctionsExceptionCode.UNAVAILABLE,
            FunctionsExceptionCode.DEADLINE_EXCEEDED -> ApiError.Network(message)
            else -> when {
                "unauthenticated" in haystack -> ApiError.Unauthenticated(message)
                "permission-denied" in haystack || "permission denied" in haystack -> ApiError.PermissionDenied(message)
                "resource-exhausted" in haystack || "resource exhausted" in haystack -> ApiError.ResourceExhausted(message)
                "failed-precondition" in haystack || "failed precondition" in haystack -> ApiError.FailedPrecondition(message)
                "invalid-argument" in haystack || "invalid argument" in haystack -> ApiError.InvalidArgument(message)
                "internal" in haystack -> ApiError.Internal(message)
                haystack.hasConnectivityFailureHint() -> ApiError.Network(message)
                else -> ApiError.Unknown(message)
            }
        }
    }

    private fun Throwable.toNetworkAwareApiError(): ApiError {
        val haystack = searchableText()

        return if (haystack.hasConnectivityFailureHint()) {
            ApiError.Network(message)
        } else {
            ApiError.Unknown(message)
        }
    }

    private fun Throwable.searchableText(extra: Any? = null): String = buildString {
        append(message.orEmpty())
        append(' ')
        append(toString())
        append(' ')
        append(extra?.toString().orEmpty())
        var currentCause = cause
        while (currentCause != null) {
            append(' ')
            append(currentCause.message.orEmpty())
            append(' ')
            append(currentCause.toString())
            currentCause = currentCause.cause
        }
    }.lowercase()

    private fun String.hasConnectivityFailureHint(): Boolean =
        contains("unavailable") ||
            contains("deadline_exceeded") ||
            contains("deadline exceeded") ||
            contains("timed out") ||
            contains("timeout") ||
            contains("offline") ||
            contains("no internet") ||
            contains("not connected to the internet") ||
            contains("internet connection appears to be offline") ||
            contains("network is unreachable") ||
            contains("network unreachable") ||
            contains("network unavailable") ||
            contains("network error") ||
            contains("unable to resolve host") ||
            contains("unknownhostexception") ||
            contains("no address associated with hostname") ||
            contains("nodename nor servname provided") ||
            contains("dns") ||
            contains("failed to connect") ||
            contains("connection refused") ||
            contains("connection reset") ||
            contains("connection timed out") ||
            contains("sockettimeoutexception") ||
            contains("connectexception") ||
            contains("nsurlerrordomain") ||
            contains("-1009") ||
            contains("-1001") ||
            contains("-1003") ||
            contains("-1004")

    private companion object {
        const val DEFAULT_REGION = "europe-west1"
    }
}
