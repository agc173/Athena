package com.agc.bwitch.ui.oracle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agc.bwitch.localization.OracleStrings
import com.agc.bwitch.localization.appStrings
import com.agc.bwitch.presentation.oracle.OracleStatusErrorMessage
import com.agc.bwitch.presentation.oracle.OracleStatusErrorMessageId
import com.agc.bwitch.presentation.oracle.OracleStatusViewModel
import org.koin.compose.koinInject

@Composable
fun OracleDebugScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: OracleStatusViewModel = koinInject(),
) {
    val strings = appStrings.oracle
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Column(
        modifier = modifier
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            state.isLoading -> Text(strings.debugLoading)
            state.mode != null -> Text(
                "${strings.debugModePrefix}: ${state.mode}",
                style = MaterialTheme.typography.titleMedium,
            )
            state.error != null -> Text(
                "${strings.debugErrorPrefix}: ${state.error?.toUiText(strings)}",
                color = MaterialTheme.colorScheme.error,
            )
            else -> Text(strings.debugNoData)
        }

        Button(
            onClick = viewModel::refresh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(strings.debugRefreshCta)
        }
    }
}

private fun OracleStatusErrorMessage.toUiText(strings: OracleStrings): String = when (id) {
    OracleStatusErrorMessageId.UnknownFallback -> strings.debugUnknownError
    OracleStatusErrorMessageId.RawBackendMessage -> rawMessage ?: strings.debugUnknownError
}
