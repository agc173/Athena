package com.agc.bwitch.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.agc.bwitch.localization.appStrings

@Composable
fun BirthTimeSelector(
    selectedHour: Int,
    selectedMinute: Int,
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
    label: String,
    hourLabel: String,
    minuteLabel: String,
    pickerTitle: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        OutlinedButton(
            onClick = { showPicker = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = formatBirthTime(selectedHour, selectedMinute),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }

    if (showPicker) {
        BirthTimePickerDialog(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            hourLabel = hourLabel,
            minuteLabel = minuteLabel,
            title = pickerTitle,
            onDismiss = { showPicker = false },
            onConfirm = { hour, minute ->
                showPicker = false
                onTimeSelected(hour, minute)
            },
        )
    }
}

@Composable
private fun BirthTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    hourLabel: String,
    minuteLabel: String,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    var selectedHour by remember(initialHour) { mutableStateOf(initialHour.coerceIn(0, 23)) }
    var selectedMinute by remember(initialMinute) { mutableStateOf(initialMinute.coerceIn(0, 59)) }
    val strings = appStrings.common

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = formatBirthTime(selectedHour, selectedMinute),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    WheelSelector(
                        label = hourLabel,
                        options = (0..23).toList(),
                        selected = selectedHour,
                        displayValue = { it.toString().padStart(2, '0') },
                        onSelected = { selectedHour = it },
                        modifier = Modifier.weight(1f),
                    )
                    WheelSelector(
                        label = minuteLabel,
                        options = (0..59).toList(),
                        selected = selectedMinute,
                        displayValue = { it.toString().padStart(2, '0') },
                        onSelected = { selectedMinute = it },
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text(strings.cancelLabel) }
                    Button(onClick = { onConfirm(selectedHour, selectedMinute) }) { Text(strings.acceptLabel) }
                }
            }
        }
    }
}

private fun formatBirthTime(hour: Int, minute: Int): String =
    "${hour.coerceIn(0, 23).toString().padStart(2, '0')}:${minute.coerceIn(0, 59).toString().padStart(2, '0')}"
