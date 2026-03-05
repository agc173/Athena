package com.agc.bwitch.data.functions

import com.agc.bwitch.domain.shared.ApiError
import com.agc.bwitch.domain.shared.ApiResult

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.functions.FirebaseFunctionsException
import dev.gitlive.firebase.functions.functions

class GitLiveFunctionsClient(
    region: String = DEFAULT_REGION
) : FunctionsClient {

    private val functions = Firebase.functions(region)

    override suspend fun call(name: String, data: Map<String, Any?>?): ApiResult<Map<String, Any?>> {
        return try {
            val result = functions
                .httpsCallable(name)
                .invoke(data)

            val payload = result.data<Map<String, Any?>>()

            if (payload is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                ApiResult.Ok(payload as Map<String, Any?>)
            } else {
                ApiResult.Err(ApiError.Internal("Callable '$name' did not return an object payload."))
            }
        } catch (e: FirebaseFunctionsException) {
            ApiResult.Err(e.toApiError())
        } catch (e: Throwable) {
            ApiResult.Err(ApiError.Unknown(e.message))
        }
    }

    private fun FirebaseFunctionsException.toApiError(): ApiError {
        val normalizedCode = runCatching { code.toString() }
            .getOrDefault("")
            .lowercase()
            .substringAfterLast('.')
            .replace('_', '-')

        return when (normalizedCode) {
            "unauthenticated" -> ApiError.Unauthenticated(message)
            "permission-denied" -> ApiError.PermissionDenied(message)
            "resource-exhausted" -> ApiError.ResourceExhausted(message)
            "failed-precondition" -> ApiError.FailedPrecondition(message)
            "invalid-argument" -> ApiError.InvalidArgument(message)
            "internal" -> ApiError.Internal(message)
            else -> ApiError.Unknown(message)
        }
    }

    private companion object {
        const val DEFAULT_REGION = "europe-west1"
    }
}
