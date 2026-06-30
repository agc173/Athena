package com.agc.bwitch.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.agc.bwitch.localization.appStrings
import kotlin.time.Clock
import kotlinx.datetime.Instant
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
                text = selectedDate?.toFriendlyBirthDate() ?: appStrings.onboarding.birthDatePlaceholder,
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

    val strings = appStrings.common
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
                        text = strings.birthDatePickerTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = confirmedDate.toFriendlyBirthDate(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    WheelSelector(
                        label = strings.dayLabel,
                        options = (1..maxDayForSelection(selectedYear, selectedMonth, today)).toList(),
                        selected = selectedDay,
                        displayValue = { it.toString().padStart(2, '0') },
                        onSelected = { updateSelection(day = it) },
                        modifier = Modifier.weight(1f),
                    )
                    WheelSelector(
                        label = strings.monthLabel,
                        options = (1..maxMonthForYear(selectedYear, today)).toList(),
                        selected = selectedMonth,
                        displayValue = { it.toString().padStart(2, '0') },
                        onSelected = { updateSelection(month = it) },
                        modifier = Modifier.weight(1f),
                    )
                    WheelSelector(
                        label = strings.yearLabel,
                        options = (MinimumBirthYear..today.year).toList(),
                        selected = selectedYear,
                        displayValue = { it.toString() },
                        onSelected = { updateSelection(year = it) },
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text(strings.cancelLabel) }
                    Button(onClick = { onConfirm(confirmedDate) }) { Text(strings.acceptLabel) }
                }
            }
        }
    }
}

@Composable
internal fun WheelSelector(
    label: String,
    options: List<Int>,
    selected: Int,
    displayValue: (Int) -> String,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = options.indexOf(selected).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
        )
        LazyColumn(
            state = listState,
            modifier = Modifier
                .height(132.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(options) { option ->
                val isSelected = option == selected
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            shape = MaterialTheme.shapes.medium,
                        )
                        .clickable { onSelected(option) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = displayValue(option),
                        style = if (isSelected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

fun LocalDate.toFriendlyBirthDate(): String = toString()

@Composable
private fun rememberToday(): LocalDate = remember { currentSystemLocalDate() }

private fun currentSystemLocalDate(): LocalDate {
    val now = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
    return now.toLocalDateTime(TimeZone.currentSystemDefault()).date
}

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
