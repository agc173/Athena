package com.agc.bwitch.data.functions

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
                .call(data)
                .data

            if (result is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                ApiResult.Ok(result as Map<String, Any?>)
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
        return when (code) {
            FirebaseFunctionsException.Code.UNAUTHENTICATED -> ApiError.Unauthenticated(message)
            FirebaseFunctionsException.Code.PERMISSION_DENIED -> ApiError.PermissionDenied(message)
            FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED -> ApiError.ResourceExhausted(message)
            FirebaseFunctionsException.Code.FAILED_PRECONDITION -> ApiError.FailedPrecondition(message)
            FirebaseFunctionsException.Code.INVALID_ARGUMENT -> ApiError.InvalidArgument(message)
            FirebaseFunctionsException.Code.INTERNAL -> ApiError.Internal(message)
            else -> ApiError.Unknown(message)
        }
    }

    private companion object {
        const val DEFAULT_REGION = "europe-west1"
    }
}
