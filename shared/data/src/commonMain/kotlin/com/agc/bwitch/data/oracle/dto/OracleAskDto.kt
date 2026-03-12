package com.agc.bwitch.data.oracle.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OracleAskRequestDto(
    val requestType: String,
    val requestId: String,
    val question: String,
    val topic: String? = null,
    val lang: String? = null,
    val adUnlock: AdUnlockDto? = null,
)

@Serializable
data class AdUnlockDto(
    val rewardedProof: String,
)

@Serializable
data class OracleAskResponseDto(
    val requestId: String,
    val status: String,
    val answer: OracleAnswerDto? = null,
    val quotaSnapshot: OracleQuotaSnapshotDto? = null,
    val systemMode: String? = null,
    val error: OracleErrorDto? = null,
)

@Serializable
data class OracleAnswerDto(
    val title: String? = null,
    val guidance: OracleGuidanceDto? = null,
)

@Serializable
data class OracleGuidanceDto(
    val core: String? = null,
    @SerialName("do")
    val doList: List<String>? = null,
    @SerialName("avoid")
    val avoidList: List<String>? = null,
    val reflection: String? = null,
)

@Serializable
data class OracleQuotaSnapshotDto(
    val maxRequestsRemaining: Int? = null,
    val adUnlockRemaining: Int? = null,
)

@Serializable
data class OracleErrorDto(
    val message: String? = null,
)
