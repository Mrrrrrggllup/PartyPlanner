package com.partyplanner.ui.event

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.partyplanner.presentation.event.EditEventComponent
import com.partyplanner.presentation.event.EditEventState
import com.partyplanner.ui.theme.AppShapes
import com.partyplanner.ui.theme.appColors
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import partyplanner.composeapp.generated.resources.Res
import partyplanner.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventScreen(component: EditEventComponent) {
    val state by component.state.collectAsState()

    var title       by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location    by remember { mutableStateOf("") }

    var startDateTime by remember { mutableStateOf<LocalDateTime?>(null) }
    var endDateTime   by remember { mutableStateOf<LocalDateTime?>(null) }
    var prefilled     by remember { mutableStateOf(false) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker   by remember { mutableStateOf(false) }
    var showEndTimePicker   by remember { mutableStateOf(false) }

    val startDateState = rememberDatePickerState()
    val startTimeState = rememberTimePickerState(initialHour = 19, initialMinute = 0)
    val endDateState   = rememberDatePickerState()
    val endTimeState   = rememberTimePickerState(initialHour = 22, initialMinute = 0)

    // Pre-fill form once event is loaded
    val loaded = state as? EditEventState.Loaded
    LaunchedEffect(loaded?.event) {
        val event = loaded?.event ?: return@LaunchedEffect
        if (!prefilled) {
            title       = event.title
            description = event.description ?: ""
            location    = event.location ?: ""
            startDateTime = event.startDate
            endDateTime   = event.endDate
            prefilled = true
        }
    }

    val isLoading = loaded?.isSaving == true
    val error = loaded?.error

    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showStartDatePicker = false
                    showStartTimePicker = true
                }) { Text(stringResource(Res.string.common_next)) }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text(stringResource(Res.string.common_cancel))
                }
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
                }) { Text(stringResource(Res.string.common_next)) }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text(stringResource(Res.string.common_cancel))
                }
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

    val focusManager = LocalFocusManager.current

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (state) {
            is EditEventState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            is EditEventState.Error -> Box(
                Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    (state as EditEventState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
            }
            is EditEventState.Loaded -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { focusManager.clearFocus() }
            ) {
                EditEventHeader(onBack = component::onBack)

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
                            label = stringResource(Res.string.create_event_field_title),
                            capitalization = KeyboardCapitalization.Sentences,
                        )

                        Spacer(Modifier.height(14.dp))

                        DateFieldButton(
                            label = stringResource(Res.string.create_event_field_start_date),
                            value = startDateTime?.formatDisplay(),
                            onClick = { showStartDatePicker = true }
                        )

                        Spacer(Modifier.height(14.dp))

                        DateFieldButton(
                            label = stringResource(Res.string.create_event_field_end_date),
                            value = endDateTime?.formatDisplay(),
                            onClick = { showEndDatePicker = true }
                        )

                        Spacer(Modifier.height(14.dp))

                        CreateTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = stringResource(Res.string.common_location),
                            capitalization = KeyboardCapitalization.Words,
                        )

                        Spacer(Modifier.height(14.dp))

                        CreateTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = stringResource(Res.string.create_event_field_description),
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
                                    component.onSave(
                                        title       = title.trim(),
                                        description = description.ifBlank { null },
                                        location    = location.ifBlank { null },
                                        startDate   = startDateTime!!,
                                        endDate     = endDateTime,
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Text(
                                    "Enregistrer",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun EditEventHeader(onBack: () -> Unit) {
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
            text = "Modifier l'événement",
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

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
