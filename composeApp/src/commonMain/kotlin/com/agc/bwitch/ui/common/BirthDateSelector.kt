package com.agc.bwitch.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun BirthDateSelector(
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    isError: Boolean = false,
) {
    val today = rememberToday()
    var selectedYear by remember(selectedDate) { mutableStateOf(selectedDate?.year ?: (today.year - 18)) }
    var selectedMonth by remember(selectedDate) { mutableStateOf(selectedDate?.monthNumber ?: 1) }
    var selectedDay by remember(selectedDate) { mutableStateOf(selectedDate?.dayOfMonth ?: 1) }

    fun syncSelection(year: Int = selectedYear, month: Int = selectedMonth, day: Int = selectedDay) {
        val clampedDay = day.coerceAtMost(daysInMonth(year, month))
        val candidate = LocalDate(year, month, clampedDay)
        val safeDate = if (candidate > today) today else candidate
        selectedYear = safeDate.year
        selectedMonth = safeDate.monthNumber
        selectedDay = safeDate.dayOfMonth
        onDateSelected(safeDate)
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        Text(
            text = selectedDate?.toFriendlyBirthDate().orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            BirthDateDropdown(
                text = selectedDay.toString().padStart(2, '0'),
                options = (1..daysInMonth(selectedYear, selectedMonth)).toList(),
                enabled = enabled,
                modifier = Modifier.weight(1f),
                format = { it.toString().padStart(2, '0') },
            ) { syncSelection(day = it) }
            BirthDateDropdown(
                text = selectedMonth.toString().padStart(2, '0'),
                options = availableMonths(selectedYear, today),
                enabled = enabled,
                modifier = Modifier.weight(1f),
                format = { it.toString().padStart(2, '0') },
            ) { syncSelection(month = it) }
            BirthDateDropdown(
                text = selectedYear.toString(),
                options = (today.year downTo 1900).toList(),
                enabled = enabled,
                modifier = Modifier.weight(1.4f),
            ) { syncSelection(year = it) }
        }
        supportingText?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BirthDateDropdown(
    text: String,
    options: List<Int>,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    format: (Int) -> String = { it.toString() },
    onSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { expanded = true }, enabled = enabled, modifier = modifier) { Text(text) }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        options.forEach { option ->
            DropdownMenuItem(text = { Text(format(option)) }, onClick = {
                expanded = false
                onSelected(option)
            })
        }
    }
}

fun LocalDate.toFriendlyBirthDate(): String =
    "${dayOfMonth.toString().padStart(2, '0')}/${monthNumber.toString().padStart(2, '0')}/$year"

@Composable
private fun rememberToday(): LocalDate = remember { currentSystemLocalDate() }

private fun currentSystemLocalDate(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

private fun availableMonths(year: Int, today: LocalDate): List<Int> =
    if (year == today.year) (1..today.monthNumber).toList() else (1..12).toList()

private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if (isLeapYear(year)) 29 else 28
    else -> 31
}

private fun isLeapYear(year: Int): Boolean =
    (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
