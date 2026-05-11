package com.agc.bwitch.data.remote.premium

import com.agc.bwitch.data.functions.FunctionsClient
import com.agc.bwitch.domain.shared.ApiResult

class PremiumRemoteDataSource(
    private val functionsClient: FunctionsClient,
) {
    suspend fun validateGooglePlaySubscription(
        request: ValidateGooglePlaySubscriptionRequest,
    ): PremiumEntitlementDto = when (
        val result = functionsClient.call(
            name = VALIDATE_GOOGLE_PLAY_SUBSCRIPTION_CALLABLE,
            data = request,
            requestSerializer = ValidateGooglePlaySubscriptionRequest.serializer(),
            responseSerializer = PremiumEntitlementDto.serializer(),
        )
    ) {
        is ApiResult.Ok -> result.value
        is ApiResult.Err -> throw result.error.toException()
    }

    suspend fun restoreGooglePlayPurchases(
        request: RestoreGooglePlayPurchasesRequest,
    ): RestoreGooglePlayPurchasesResponse = when (
        val result = functionsClient.call(
            name = RESTORE_GOOGLE_PLAY_PURCHASES_CALLABLE,
            data = request,
            requestSerializer = RestoreGooglePlayPurchasesRequest.serializer(),
            responseSerializer = RestoreGooglePlayPurchasesResponse.serializer(),
        )
    ) {
        is ApiResult.Ok -> result.value
        is ApiResult.Err -> throw result.error.toException()
    }

    suspend fun refreshEntitlement(force: Boolean): PremiumEntitlementDto = when (
        val result = functionsClient.call(
            name = REFRESH_ENTITLEMENT_CALLABLE,
            data = RefreshEntitlementRequest(force = force),
            requestSerializer = RefreshEntitlementRequest.serializer(),
            responseSerializer = PremiumEntitlementDto.serializer(),
        )
    ) {
        is ApiResult.Ok -> result.value
        is ApiResult.Err -> throw result.error.toException()
    }

    private companion object {
        const val VALIDATE_GOOGLE_PLAY_SUBSCRIPTION_CALLABLE = "validateGooglePlaySubscription"
        const val RESTORE_GOOGLE_PLAY_PURCHASES_CALLABLE = "restoreGooglePlayPurchases"
        const val REFRESH_ENTITLEMENT_CALLABLE = "refreshEntitlement"
    }
}

private fun com.agc.bwitch.domain.shared.ApiError.toException(): IllegalStateException {
    return IllegalStateException(message ?: "Premium entitlement backend request failed")
}
