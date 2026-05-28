package com.agc.bwitch.data.account

import com.agc.bwitch.domain.account.AccountDeletionStatus
import kotlinx.serialization.Serializable

@Serializable
internal data class AccountDeletionStatusDto(
    val pendingDeletion: Boolean = false,
) {
    fun toDomain(): AccountDeletionStatus = AccountDeletionStatus(
        pendingDeletion = pendingDeletion,
    )
}

@Serializable
internal data class AccountDeletionEmptyRequestDto(
    val client: String = "bwitch",
)

@Serializable
internal data class AccountDeletionResponseDto(
    val success: Boolean = true,
    val pendingDeletion: Boolean = false,
)
