package com.partyplanner.ui.event

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.partyplanner.presentation.event.CreateEventComponent
import com.partyplanner.presentation.event.CreateEventState
import com.partyplanner.ui.theme.AppShapes
import com.partyplanner.ui.theme.appColors
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(component: CreateEventComponent) {
    val state by component.state.collectAsState()

    var title       by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location    by remember { mutableStateOf("") }

    // Dates sélectionnées
    var startDateTime by remember { mutableStateOf<LocalDateTime?>(null) }
    var endDateTime   by remember { mutableStateOf<LocalDateTime?>(null) }

    // Dialogs
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker   by remember { mutableStateOf(false) }
    var showEndTimePicker   by remember { mutableStateOf(false) }

    // Picker states
    val startDateState = rememberDatePickerState()
    val startTimeState = rememberTimePickerState(initialHour = 19, initialMinute = 0)
    val endDateState   = rememberDatePickerState()
    val endTimeState   = rememberTimePickerState(initialHour = 22, initialMinute = 0)

    val isLoading = state is CreateEventState.Loading
    val error = (state as? CreateEventState.Error)?.message

    // ── Date picker dialogs ────────────────────────────────────────────────────

    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showStartDatePicker = false
                    showStartTimePicker = true
                }) { Text("Suivant") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Annuler") }
            }
        ) { DatePicker(state = startDateState) }
    }

    if (showStartTimePicker) {
        TimePickerDialog(
            onDismiss = { showStartTimePicker = false },
            onConfirm = {
                showStartTimePicker = false
                startDateTime = millisToDateTime(startDateState.selectedDateMillis, startTimeState)
            },
            state = startTimeState
        )
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showEndDatePicker = false
                    showEndTimePicker = true
                }) { Text("Suivant") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Annuler") }
            }
        ) { DatePicker(state = endDateState) }
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            onDismiss = { showEndTimePicker = false },
            onConfirm = {
                showEndTimePicker = false
                endDateTime = millisToDateTime(endDateState.selectedDateMillis, endTimeState)
            },
            state = endTimeState
        )
    }

    // ── Écran ─────────────────────────────────────────────────────────────────

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            CreateEventHeader(onBack = component::onBack)

            Spacer(Modifier.height(24.dp))

            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = AppShapes.Card,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {

                    CreateTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = "Titre *",
                        capitalization = KeyboardCapitalization.Sentences,
                    )

                    Spacer(Modifier.height(14.dp))

                    // Date de début
                    DateFieldButton(
                        label = "Date de début *",
                        value = startDateTime?.formatDisplay(),
                        onClick = { showStartDatePicker = true }
                    )

                    Spacer(Modifier.height(14.dp))

                    // Date de fin (optionnelle)
                    DateFieldButton(
                        label = "Date de fin",
                        value = endDateTime?.formatDisplay(),
                        onClick = { showEndDatePicker = true }
                    )

                    Spacer(Modifier.height(14.dp))

                    CreateTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = "Lieu",
                        capitalization = KeyboardCapitalization.Words,
                    )

                    Spacer(Modifier.height(14.dp))

                    CreateTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = "Description",
                        singleLine = false,
                        imeAction = ImeAction.Default,
                        capitalization = KeyboardCapitalization.Sentences,
                    )

                    if (error != null) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    val gradA = MaterialTheme.appColors.gradA
                    val canSubmit = title.isNotBlank() && startDateTime != null && !isLoading
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(AppShapes.Pill)
                            .alpha(if (canSubmit) 1f else 0.5f)
                            .background(brush = gradA)
                            .clickable(enabled = canSubmit) {
                                component.createEvent(
                                    title = title.trim(),
                                    description = description.ifBlank { null },
                                    location = location.ifBlank { null },
                                    startDate = startDateTime!!,
                                    endDate = endDateTime,
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Text("Créer l'événement", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ── Composants ────────────────────────────────────────────────────────────────

@Composable
private fun CreateEventHeader(onBack: () -> Unit) {
    val gradA = MaterialTheme.appColors.gradA
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(brush = gradA)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(AppShapes.ActionIcon)
                .background(Color.White.copy(alpha = 0.25f))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Text("←", color = Color.White, style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = "Nouvel événement",
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun DateFieldButton(label: String, value: String?, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.TextField,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (value != null) MaterialTheme.colorScheme.onSurface
                           else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value ?: label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text("📅", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun CreateTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = singleLine,
        shape = AppShapes.TextField,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            capitalization = capitalization,
            imeAction = imeAction
        ),
        minLines = if (!singleLine) 3 else 1,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    state: TimePickerState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onConfirm) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
        text = { TimePicker(state = state) }
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
private fun millisToDateTime(millis: Long?, timeState: TimePickerState): LocalDateTime? {
    millis ?: return null
    val localDate = Instant.fromEpochMilliseconds(millis)
        .toLocalDateTime(TimeZone.UTC)
    return LocalDateTime(
        localDate.year, localDate.monthNumber, localDate.dayOfMonth,
        timeState.hour, timeState.minute
    )
}

@Suppress("DEPRECATION")
private fun LocalDateTime.formatDisplay(): String {
    val d   = dayOfMonth.toString().padStart(2, '0')
    val m   = monthNumber.toString().padStart(2, '0')
    val h   = hour.toString().padStart(2, '0')
    val min = minute.toString().padStart(2, '0')
    return "$d/$m/$year à $h:$min"
}
