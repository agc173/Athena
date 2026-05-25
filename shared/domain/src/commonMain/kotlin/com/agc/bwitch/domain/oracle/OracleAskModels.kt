package com.agc.bwitch.domain.oracle

import com.agc.bwitch.domain.model.DeckCardUnlockReward
enum class OracleTopic {
    GENERAL,
    LOVE,
    WORK,
    HEALTH,
    SPIRITUAL,
}

data class OracleAskRequest(
    val requestId: String,
    val question: String,
    val topic: OracleTopic? = null,
    val lang: String? = null,
)

sealed class OracleAskResult {
    abstract val requestId: String
    abstract val status: String

    data class InProgress(
        override val requestId: String,
        override val status: String = "IN_PROGRESS",
    ) : OracleAskResult()

    data class CompletedSuccess(
        override val requestId: String,
        val answer: OracleAnswer,
        val deckCardUnlockRewards: List<DeckCardUnlockReward> = emptyList(),
        val systemMode: SystemMode? = null,
        val quotaSnapshot: OracleQuotaSnapshot? = null,
        override val status: String = "COMPLETED_SUCCESS",
    ) : OracleAskResult()
}

data class OracleAnswer(
    val title: String? = null,
    val coreGuidance: String,
    val doList: List<String> = emptyList(),
    val avoidList: List<String> = emptyList(),
    val reflection: String? = null,
)

data class OracleQuotaSnapshot(
    val maxRequestsRemaining: Int? = null,
    val adUnlockRemaining: Int? = null,
)
