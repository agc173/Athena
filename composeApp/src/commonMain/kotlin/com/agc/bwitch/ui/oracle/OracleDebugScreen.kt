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
import com.agc.bwitch.presentation.oracle.OracleStatusViewModel
import org.koin.compose.koinInject

@Composable
fun OracleDebugScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: OracleStatusViewModel = koinInject(),
) {
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
            state.isLoading -> Text("Loading...")
            state.mode != null -> Text("mode: ${state.mode}", style = MaterialTheme.typography.titleMedium)
            state.error != null -> Text("error: ${state.error}", color = MaterialTheme.colorScheme.error)
            else -> Text("No data")
        }

        Button(
            onClick = viewModel::refresh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Refresh")
        }
    }
}
