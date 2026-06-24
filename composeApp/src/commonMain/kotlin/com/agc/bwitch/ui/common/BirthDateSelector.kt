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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private const val MinimumBirthYear = 1900

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
    var showPicker by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        OutlinedButton(
            onClick = { showPicker = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = selectedDate?.toFriendlyBirthDate() ?: "DD/MM/YYYY",
                color = when {
                    isError -> MaterialTheme.colorScheme.error
                    selectedDate == null -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
        }
        supportingText?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showPicker) {
        BirthDatePickerDialog(
            initialDate = selectedDate ?: defaultBirthDate(today),
            today = today,
            onDismiss = { showPicker = false },
            onConfirm = {
                showPicker = false
                onDateSelected(it)
            },
        )
    }
}

@Composable
private fun BirthDatePickerDialog(
    initialDate: LocalDate,
    today: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    var selectedYear by remember(initialDate) { mutableStateOf(initialDate.year.coerceIn(MinimumBirthYear, today.year)) }
    var selectedMonth by remember(initialDate) { mutableStateOf(initialDate.monthNumber.coerceIn(1, maxMonthForYear(selectedYear, today))) }
    var selectedDay by remember(initialDate) { mutableStateOf(initialDate.dayOfMonth.coerceIn(1, maxDayForSelection(selectedYear, selectedMonth, today))) }

    fun updateSelection(year: Int = selectedYear, month: Int = selectedMonth, day: Int = selectedDay) {
        val safeYear = year.coerceIn(MinimumBirthYear, today.year)
        val safeMonth = month.coerceIn(1, maxMonthForYear(safeYear, today))
        val safeDay = day.coerceIn(1, maxDayForSelection(safeYear, safeMonth, today))
        selectedYear = safeYear
        selectedMonth = safeMonth
        selectedDay = safeDay
    }

    val confirmedDate = LocalDate(selectedYear, selectedMonth, selectedDay)

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
                        text = "Selecciona tu fecha de nacimiento",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = confirmedDate.toFriendlyBirthDate(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                DateStepperRow(
                    label = "Día",
                    value = selectedDay.toString().padStart(2, '0'),
                    onDecrease = { updateSelection(day = selectedDay - 1) },
                    onIncrease = { updateSelection(day = selectedDay + 1) },
                    decreaseEnabled = selectedDay > 1,
                    increaseEnabled = selectedDay < maxDayForSelection(selectedYear, selectedMonth, today),
                )
                DateStepperRow(
                    label = "Mes",
                    value = selectedMonth.toString().padStart(2, '0'),
                    onDecrease = { updateSelection(month = selectedMonth - 1) },
                    onIncrease = { updateSelection(month = selectedMonth + 1) },
                    decreaseEnabled = selectedMonth > 1,
                    increaseEnabled = selectedMonth < maxMonthForYear(selectedYear, today),
                )
                DateStepperRow(
                    label = "Año",
                    value = selectedYear.toString(),
                    onDecrease = { updateSelection(year = selectedYear - 1) },
                    onIncrease = { updateSelection(year = selectedYear + 1) },
                    decreaseEnabled = selectedYear > MinimumBirthYear,
                    increaseEnabled = selectedYear < today.year,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Button(onClick = { onConfirm(confirmedDate) }) { Text("Aceptar") }
                }
            }
        }
    }
}

@Composable
private fun DateStepperRow(
    label: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    decreaseEnabled: Boolean,
    increaseEnabled: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        OutlinedButton(onClick = onDecrease, enabled = decreaseEnabled) { Text("−") }
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
        )
        OutlinedButton(onClick = onIncrease, enabled = increaseEnabled) { Text("+") }
    }
}

fun LocalDate.toFriendlyBirthDate(): String =
    "${dayOfMonth.toString().padStart(2, '0')}/${monthNumber.toString().padStart(2, '0')}/$year"

@Composable
private fun rememberToday(): LocalDate = remember { currentSystemLocalDate() }

private fun currentSystemLocalDate(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

private fun defaultBirthDate(today: LocalDate): LocalDate =
    LocalDate((today.year - 18).coerceAtLeast(MinimumBirthYear), today.monthNumber, today.dayOfMonth)

private fun maxMonthForYear(year: Int, today: LocalDate): Int =
    if (year == today.year) today.monthNumber else 12

private fun maxDayForSelection(year: Int, month: Int, today: LocalDate): Int {
    val monthMax = daysInMonth(year, month)
    return if (year == today.year && month == today.monthNumber) today.dayOfMonth.coerceAtMost(monthMax) else monthMax
}

private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if (isLeapYear(year)) 29 else 28
    else -> 31
}

private fun isLeapYear(year: Int): Boolean =
    (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
