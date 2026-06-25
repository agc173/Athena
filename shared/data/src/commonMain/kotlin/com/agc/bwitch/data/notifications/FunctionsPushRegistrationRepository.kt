package com.agc.bwitch.data.notifications

import com.agc.bwitch.data.functions.FunctionsClient
import com.agc.bwitch.domain.notifications.PushNotificationPreferences
import com.agc.bwitch.domain.notifications.PushPlatform
import com.agc.bwitch.domain.notifications.PushRegistrationRepository
import com.agc.bwitch.domain.notifications.PushTokenRegistration
import com.agc.bwitch.domain.shared.ApiError
import com.agc.bwitch.domain.shared.ApiResult
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

class FunctionsPushRegistrationRepository(
    private val functionsClient: FunctionsClient,
) : PushRegistrationRepository {

    override suspend fun getPreferences(): PushNotificationPreferences? {
        val response = callBackend(
            name = GET_NOTIFICATION_PREFERENCES_CALLABLE,
            data = EmptyRequestDto(),
            requestSerializer = EmptyRequestDto.serializer(),
            responseSerializer = GetNotificationPreferencesResponseDto.serializer(),
        )

        return if (response.exists) {
            PushNotificationPreferences(
                globalEnabled = response.globalEnabled,
                dailyHoroscopeEnabled = response.dailyHoroscopeEnabled,
                dailyRewardEnabled = response.dailyRewardEnabled,
                tarotOracleReminderEnabled = response.tarotOracleReminderEnabled,
                ritualsEnabled = response.ritualsEnabled,
                habitsEnabled = response.habitsEnabled,
            )
        } else {
            null
        }
    }

    override suspend fun registerToken(payload: PushTokenRegistration) {
        callBackend(
            name = REGISTER_PUSH_TOKEN_CALLABLE,
            data = RegisterPushTokenRequestDto(
                token = payload.token,
                platform = payload.platform.toBackendValue(),
                appVersion = payload.appVersion,
                locale = payload.locale,
                timezone = payload.timezone,
                permissionGranted = payload.notificationsPermissionGranted,
            ),
            requestSerializer = RegisterPushTokenRequestDto.serializer(),
            responseSerializer = EmptyResponseDto.serializer(),
        )
    }

    override suspend fun unregisterToken(token: String, platform: PushPlatform) {
        callBackend(
            name = UNREGISTER_PUSH_TOKEN_CALLABLE,
            data = UnregisterPushTokenRequestDto(
                token = token,
                platform = platform.toBackendValue(),
            ),
            requestSerializer = UnregisterPushTokenRequestDto.serializer(),
            responseSerializer = EmptyResponseDto.serializer(),
        )
    }

    override suspend fun updatePreferences(preferences: PushNotificationPreferences) {
        callBackend(
            name = UPDATE_NOTIFICATION_PREFERENCES_CALLABLE,
            data = UpdateNotificationPreferencesRequestDto(
                globalEnabled = preferences.globalEnabled,
                dailyHoroscopeEnabled = preferences.dailyHoroscopeEnabled,
                dailyRewardEnabled = preferences.dailyRewardEnabled,
                tarotOracleReminderEnabled = preferences.tarotOracleReminderEnabled,
                ritualsEnabled = preferences.ritualsEnabled,
                habitsEnabled = preferences.habitsEnabled,
            ),
            requestSerializer = UpdateNotificationPreferencesRequestDto.serializer(),
            responseSerializer = EmptyResponseDto.serializer(),
        )
    }


    private suspend fun <Req : Any, Res : Any> callBackend(
        name: String,
        data: Req,
        requestSerializer: KSerializer<Req>,
        responseSerializer: KSerializer<Res>,
    ): Res = when (
        val result = functionsClient.call(
            name = name,
            data = data,
            requestSerializer = requestSerializer,
            responseSerializer = responseSerializer,
        )
    ) {
        is ApiResult.Ok -> result.value
        is ApiResult.Err -> throw result.error.toException()
    }

    private companion object {
        const val GET_NOTIFICATION_PREFERENCES_CALLABLE = "getNotificationPreferences"
        const val REGISTER_PUSH_TOKEN_CALLABLE = "registerPushToken"
        const val UNREGISTER_PUSH_TOKEN_CALLABLE = "unregisterPushToken"
        const val UPDATE_NOTIFICATION_PREFERENCES_CALLABLE = "updateNotificationPreferences"
    }
}

@Serializable
private data class EmptyRequestDto(
    val unused: Boolean = false,
)

@Serializable
private data class GetNotificationPreferencesResponseDto(
    val exists: Boolean,
    val globalEnabled: Boolean = false,
    val dailyHoroscopeEnabled: Boolean = false,
    val dailyRewardEnabled: Boolean = false,
    val tarotOracleReminderEnabled: Boolean = false,
    val ritualsEnabled: Boolean = false,
    val habitsEnabled: Boolean = false,
)

@Serializable
private data class RegisterPushTokenRequestDto(
    val token: String,
    val platform: String,
    val appVersion: String? = null,
    val locale: String? = null,
    val timezone: String? = null,
    val permissionGranted: Boolean,
)

@Serializable
private data class UnregisterPushTokenRequestDto(
    val token: String,
    val platform: String,
)

@Serializable
private data class UpdateNotificationPreferencesRequestDto(
    val globalEnabled: Boolean,
    val dailyHoroscopeEnabled: Boolean,
    val dailyRewardEnabled: Boolean,
    val tarotOracleReminderEnabled: Boolean,
    val ritualsEnabled: Boolean,
    val habitsEnabled: Boolean,
)

@Serializable
private data class EmptyResponseDto(
    val ok: Boolean? = null,
)

private fun PushPlatform.toBackendValue(): String = when (this) {
    PushPlatform.ANDROID -> "android"
    PushPlatform.IOS -> "ios"
}

private fun ApiError.toException(): IllegalStateException =
    IllegalStateException(message ?: "Push registration backend request failed")
