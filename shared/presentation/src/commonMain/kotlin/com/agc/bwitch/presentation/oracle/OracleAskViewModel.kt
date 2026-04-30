package com.agc.bwitch.presentation.oracle

import com.agc.bwitch.domain.oracle.OracleAnswer
import com.agc.bwitch.domain.oracle.OracleAskRequest
import com.agc.bwitch.domain.oracle.OracleAskResult
import com.agc.bwitch.domain.oracle.OracleQuotaSnapshot
import com.agc.bwitch.domain.oracle.OracleRepository
import com.agc.bwitch.domain.oracle.OracleTopic
import com.agc.bwitch.domain.economy.EconomyRepository
import com.agc.bwitch.domain.analytics.AnalyticsEvent
import com.agc.bwitch.domain.analytics.AnalyticsTracker
import com.agc.bwitch.domain.analytics.NoOpAnalyticsTracker
import com.agc.bwitch.domain.localization.AppLanguage
import com.agc.bwitch.domain.localization.ObserveCurrentLanguageUseCase
import com.agc.bwitch.domain.localization.ResolveCurrentLanguageUseCase
import com.agc.bwitch.domain.shared.ApiError
import com.agc.bwitch.domain.shared.ApiResult
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MAX_QUESTION_LENGTH = 400

data class OracleAskUiState(
    val question: String = "",
    val isLoading: Boolean = false,
    val requestId: String? = null,
    val result: OracleAskResult? = null,
    val answer: OracleAnswer? = null,
    val quotaSnapshot: OracleQuotaSnapshot? = null,
    val economyBalance: Int? = null,
    val inProgress: Boolean = false,
    val error: OracleAskMessage? = null,
)

data class OracleAskMessage(
    val id: OracleAskMessageId,
    val rawMessage: String? = null,
)

enum class OracleAskMessageId {
    EmptyQuestion,
    Unauthenticated,
    PermissionDenied,
    ResourceExhaustedWithAdUnlock,
    ResourceExhaustedGeneric,
    FailedPreconditionWithAdUnlock,
    FailedPreconditionTemporaryUnavailable,
    FailedPreconditionGeneric,
    InsufficientMoons,
    InvalidArgumentFallback,
    InternalTemporaryUnavailable,
    InternalGeneric,
    UnknownFallback,
    RawBackendMessage,
}

class OracleAskViewModel(
    private val oracleRepository: OracleRepository,
    private val resolveCurrentLanguageUseCase: ResolveCurrentLanguageUseCase,
    private val observeCurrentLanguageUseCase: ObserveCurrentLanguageUseCase,
    private val economyRepository: EconomyRepository,
    private val analyticsTracker: AnalyticsTracker = NoOpAnalyticsTracker,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow(OracleAskUiState())
    val uiState: StateFlow<OracleAskUiState> = _uiState.asStateFlow()
    private val currentLanguageCode = MutableStateFlow(AppLanguage.fallback.code)
    private var lastSubmittedQuestion: String? = null

    init {
        scope.launch {
            runCatching { resolveCurrentLanguageUseCase() }
                .onSuccess { language ->
                    currentLanguageCode.value = language.code
                }
        }

        scope.launch {
            observeCurrentLanguageUseCase()
                .map { it.code }
                .distinctUntilChanged()
                .collectLatest { languageCode ->
                    currentLanguageCode.value = languageCode
                }
        }
    }

    fun onQuestionChange(value: String) {
        _uiState.update {
            it.copy(
                question = value,
                error = null,
                answer = null,
                quotaSnapshot = null,
                result = null,
                inProgress = false,
            )
        }
    }

    fun ask(topic: OracleTopic? = null, lang: String? = null) {
        val trimmedQuestion = _uiState.value.question.trim().take(MAX_QUESTION_LENGTH)
        if (trimmedQuestion.isBlank()) {
            _uiState.update {
                it.copy(
                    error = OracleAskMessage(
                        id = OracleAskMessageId.EmptyQuestion,
                    )
                )
            }
            return
        }

        val requestId = generateRequestId()
        analyticsTracker.track(AnalyticsEvent.ModuleUsed(module = "oracle", action = "ask"))
        _uiState.update {
            it.copy(
                question = trimmedQuestion,
                requestId = requestId,
                isLoading = true,
                error = null,
                inProgress = false,
                result = null,
                answer = null,
                quotaSnapshot = null,
            )
        }
        lastSubmittedQuestion = trimmedQuestion

        scope.launch {
            val effectiveLang = lang ?: currentLanguageCode.value
            when (
                val result = oracleRepository.ask(
                    OracleAskRequest(
                        requestId = requestId,
                        question = trimmedQuestion,
                        topic = topic,
                        lang = effectiveLang,
                    )
                )
            ) {
                is ApiResult.Ok -> {
                    val oracleResult = result.value
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            result = oracleResult,
                            answer = (oracleResult as? OracleAskResult.CompletedSuccess)?.answer,
                            quotaSnapshot = (oracleResult as? OracleAskResult.CompletedSuccess)?.quotaSnapshot,
                            inProgress = oracleResult is OracleAskResult.InProgress,
                            error = null,
                        )
                    }
                    refreshEconomySnapshot()
                }

                is ApiResult.Err -> {
                    val mappedError = result.error.toUserMessage()
                    if (result.error is ApiError.ResourceExhausted || result.error is ApiError.FailedPrecondition) {
                        analyticsTracker.track(
                            AnalyticsEvent.ModuleLimitReached(
                                module = "oracle",
                                isPremium = false,
                            ),
                        )
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            result = null,
                            answer = null,
                            quotaSnapshot = null,
                            inProgress = false,
                            error = mappedError.message,
                        )
                    }
                    if (mappedError.refreshEconomyAfterError) {
                        refreshEconomySnapshot()
                    }
                }
            }
        }
    }

    fun retry(topic: OracleTopic? = null, lang: String? = null) {
        if (lastSubmittedQuestion.isNullOrBlank()) return
        _uiState.update { it.copy(question = lastSubmittedQuestion.orEmpty()) }
        ask(topic = topic, lang = lang)
    }

    fun startNewConsultation() {
        lastSubmittedQuestion = null
        _uiState.value = OracleAskUiState()
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun generateRequestId(): String = Uuid.random().toString()

    private fun ApiError.toUserMessage(): OracleAskMappedError {
        val backendMessage = message.orEmpty()
        return when (this) {
            is ApiError.Unauthenticated -> OracleAskMappedError(
                message = OracleAskMessage(id = OracleAskMessageId.Unauthenticated),
            )
            is ApiError.PermissionDenied -> OracleAskMappedError(
                message = OracleAskMessage(id = OracleAskMessageId.PermissionDenied),
            )
            is ApiError.ResourceExhausted -> when {
                backendMessage.requiresLegacyAdUnlockFallback() -> OracleAskMappedError(
                    message = OracleAskMessage(id = OracleAskMessageId.ResourceExhaustedWithAdUnlock),
                )
                backendMessage.isEconomyRestrictionHint() -> OracleAskMappedError(
                    message = OracleAskMessage(id = OracleAskMessageId.InsufficientMoons),
                    refreshEconomyAfterError = true,
                )
                else -> OracleAskMappedError(
                    message = OracleAskMessage(id = OracleAskMessageId.ResourceExhaustedGeneric),
                )
            }
            is ApiError.FailedPrecondition -> when {
                backendMessage.requiresLegacyAdUnlockFallback() -> OracleAskMappedError(
                    message = OracleAskMessage(id = OracleAskMessageId.FailedPreconditionWithAdUnlock),
                )
                backendMessage.isTemporaryUnavailabilityHint() -> OracleAskMappedError(
                    message = OracleAskMessage(id = OracleAskMessageId.FailedPreconditionTemporaryUnavailable),
                )
                backendMessage.isEconomyRestrictionHint() -> OracleAskMappedError(
                    message = OracleAskMessage(id = OracleAskMessageId.InsufficientMoons),
                    refreshEconomyAfterError = true,
                )
                else -> OracleAskMappedError(
                    message = OracleAskMessage(id = OracleAskMessageId.FailedPreconditionGeneric),
                )
            }
            is ApiError.InvalidArgument -> {
                if (message.isNullOrBlank()) {
                    OracleAskMappedError(
                        message = OracleAskMessage(id = OracleAskMessageId.InvalidArgumentFallback),
                    )
                } else {
                    OracleAskMappedError(
                        message = OracleAskMessage(id = OracleAskMessageId.RawBackendMessage, rawMessage = message),
                    )
                }
            }
            is ApiError.Internal -> when {
                backendMessage.isTemporaryUnavailabilityHint() -> OracleAskMappedError(
                    message = OracleAskMessage(id = OracleAskMessageId.InternalTemporaryUnavailable),
                )
                backendMessage.isEconomyRestrictionHint() -> OracleAskMappedError(
                    message = OracleAskMessage(id = OracleAskMessageId.InsufficientMoons),
                    refreshEconomyAfterError = true,
                )
                else -> OracleAskMappedError(
                    message = OracleAskMessage(id = OracleAskMessageId.InternalGeneric),
                )
            }
            is ApiError.Unknown -> when {
                backendMessage.isEconomyRestrictionHint() -> OracleAskMappedError(
                    message = OracleAskMessage(id = OracleAskMessageId.InsufficientMoons),
                    refreshEconomyAfterError = true,
                )
                message.isNullOrBlank() -> OracleAskMappedError(
                    message = OracleAskMessage(id = OracleAskMessageId.UnknownFallback),
                )
                else -> OracleAskMappedError(
                    message = OracleAskMessage(id = OracleAskMessageId.RawBackendMessage, rawMessage = message),
                )
            }
        }
    }

    private suspend fun refreshEconomySnapshot() {
        val backendBalance = runCatching { economyRepository.getStatus().balance }
            .recoverCatching { economyRepository.getBalance().balance }
            .getOrNull()
            ?: return

        _uiState.update { current ->
            current.copy(economyBalance = backendBalance)
        }
    }

    private fun String.requiresLegacyAdUnlockFallback(): Boolean {
        val normalized = lowercase()
        return normalized.contains("rewardedproof") ||
            normalized.contains("rewarded proof") ||
            normalized.contains("ad unlock") ||
            normalized.contains("ad_unlock") ||
            normalized.contains("unlock by ad")
    }

    private fun String.isEconomyRestrictionHint(): Boolean {
        val normalized = lowercase()
        return normalized.contains("insufficient_moons") ||
            normalized.contains("not_enough_moons") ||
            normalized.contains("not enough moons") ||
            normalized.contains("insufficient moons") ||
            normalized.contains("moon_balance")
    }

    private fun String.isTemporaryUnavailabilityHint(): Boolean {
        val normalized = lowercase()
        return normalized.contains("temporar") ||
            normalized.contains("unavailable") ||
            normalized.contains("not available") ||
            normalized.contains("maintenance") ||
            normalized.contains("try again later")
    }
}

private data class OracleAskMappedError(
    val message: OracleAskMessage,
    val refreshEconomyAfterError: Boolean = false,
)
