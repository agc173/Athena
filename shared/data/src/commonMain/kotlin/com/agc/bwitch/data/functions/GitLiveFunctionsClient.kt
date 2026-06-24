package com.agc.bwitch.data.functions

import com.agc.bwitch.domain.shared.ApiError
import com.agc.bwitch.domain.shared.ApiResult
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.functions.FirebaseFunctionsException
import dev.gitlive.firebase.functions.functions
import kotlinx.serialization.KSerializer

class GitLiveFunctionsClient(
    region: String = DEFAULT_REGION
) : FunctionsClient {

    private val functions = Firebase.functions(region)

    override suspend fun <Req : Any, Res : Any> call(
        name: String,
        data: Req,
        requestSerializer: KSerializer<Req>,
        responseSerializer: KSerializer<Res>,
    ): ApiResult<Res> {
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
                "[GitLiveFunctionsClient] callable=$name mapped=$mapped message=${e.message}"
            )
            ApiResult.Err(mapped)
        } catch (e: Throwable) {
            println("[GitLiveFunctionsClient] callable=$name Throwable=$e message=${e.message}")
            ApiResult.Err(e.toNetworkAwareApiError())
        }
    }

    private fun FirebaseFunctionsException.toApiError(): ApiError {
        val haystack = buildString {
            append(message ?: "")
            append(' ')
            append(this@toApiError.toString())
        }.lowercase()

        return when {
            "unauthenticated" in haystack -> ApiError.Unauthenticated(message)
            "permission-denied" in haystack || "permission denied" in haystack -> ApiError.PermissionDenied(message)
            "resource-exhausted" in haystack || "resource exhausted" in haystack -> ApiError.ResourceExhausted(message)
            "failed-precondition" in haystack || "failed precondition" in haystack -> ApiError.FailedPrecondition(message)
            "invalid-argument" in haystack || "invalid argument" in haystack -> ApiError.InvalidArgument(message)
            "internal" in haystack -> ApiError.Internal(message)
            haystack.hasNetworkFailureHint() -> ApiError.Network(message)
            else -> ApiError.Unknown(message)
        }
    }

    private fun Throwable.toNetworkAwareApiError(): ApiError {
        val haystack = buildString {
            append(message ?: "")
            append(' ')
            append(toString())
        }.lowercase()

        return if (haystack.hasNetworkFailureHint()) {
            ApiError.Network(message)
        } else {
            ApiError.Unknown(message)
        }
    }

    private fun String.hasNetworkFailureHint(): Boolean =
        contains("network") ||
            contains("timeout") ||
            contains("timed out") ||
            contains("unavailable") ||
            contains("offline") ||
            contains("unable to resolve host") ||
            contains("failed to connect") ||
            contains("connection") ||
            contains("socket") ||
            contains("dns") ||
            contains("internet")

    private companion object {
        const val DEFAULT_REGION = "europe-west1"
    }
}
