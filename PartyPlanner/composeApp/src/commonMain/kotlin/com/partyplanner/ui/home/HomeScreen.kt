package com.partyplanner.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.partyplanner.core.AppConfig
import com.partyplanner.domain.model.Event
import com.partyplanner.presentation.home.HomeComponent
import com.partyplanner.presentation.home.HomeState
import com.partyplanner.ui.theme.AppShapes
import com.partyplanner.ui.theme.appColors
import kotlinx.coroutines.launch
import kotlinx.datetime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(component: HomeComponent) {
    val state by component.state.collectAsState()
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showMonthSheet by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                item {
                    HomeHeader(
                        displayName    = (state as? HomeState.Success)?.displayName ?: "",
                        onProfileClick = component::onProfileClick,
                    )
                }
                item { Spacer(Modifier.height(20.dp)) }
                item {
                    val events = (state as? HomeState.Success)?.events ?: emptyList()
                    CalendarStrip(
                        events          = events,
                        selectedDate    = selectedDate,
                        onDateSelected  = { date ->
                            selectedDate = if (selectedDate == date) null else date
                        },
                        onExpandCalendar = { showMonthSheet = true }
                    )
                }
                item { Spacer(Modifier.height(20.dp)) }
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedDate != null)
                                "Événements du ${selectedDate!!.formatShort()}"
                            else
                                "Mes événements",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f)
                        )
                        if (selectedDate != null) {
                            TextButton(onClick = { selectedDate = null }) {
                                Text("Tout afficher", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }

                when (val s = state) {
                    is HomeState.Loading -> item {
                        Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    is HomeState.Error -> item {
                        Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = s.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    is HomeState.Success -> {
                        val displayedEvents = if (selectedDate != null)
                            s.events.filter { it.startDate.date == selectedDate }
                        else
                            s.events

                        if (displayedEvents.isEmpty()) {
                            item { EmptyState(filtered = selectedDate != null) }
                        } else {
                            items(displayedEvents) { event ->
                                EventCard(
                                    event = event,
                                    isOwner = event.ownerId == s.currentUserId,
                                    onClick = { component.onEventClick(event.id) },
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }

            // FAB gradient
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 20.dp, bottom = 24.dp)
                    .size(56.dp)
                    .clip(AppShapes.Pill)
                    .background(brush = MaterialTheme.appColors.gradA)
                    .clickable(onClick = component::onCreateEvent),
                contentAlignment = Alignment.Center
            ) {
                Text("+", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Light)
            }
        }
    }

    if (showMonthSheet) {
        MonthCalendarSheet(
            events       = (state as? HomeState.Success)?.events ?: emptyList(),
            selectedDate = selectedDate,
            onDateSelected = { date ->
                selectedDate = if (selectedDate == date) null else date
                showMonthSheet = false
            },
            onDismiss = { showMonthSheet = false }
        )
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun HomeHeader(displayName: String, onProfileClick: () -> Unit) {
    val gradA = MaterialTheme.appColors.gradA
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = AppConfig.APP_NAME,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary
        )
        val initial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(AppShapes.Avatar)
                .background(brush = gradA)
                .clickable(onClick = onProfileClick),
            contentAlignment = Alignment.Center
        ) {
            Text(initial, color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
    }
}

// ── Calendar strip ────────────────────────────────────────────────────────────

@Composable
private fun CalendarStrip(
    events: List<Event>,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onExpandCalendar: () -> Unit
) {
    val today       = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val startOffset = 14
    val totalDays   = 90
    val days        = (0 until totalDays).map { today.plus(it - startOffset, DateTimeUnit.DAY) }
    val eventDates  = events.map { it.startDate.date }.toSet()

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startOffset)
    val scope     = rememberCoroutineScope()

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Text(
                text = "Calendrier",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = {
                    scope.launch { listState.animateScrollToItem(startOffset) }
                    onDateSelected(today)
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text("Aujourd'hui", style = MaterialTheme.typography.labelMedium)
            }
            TextButton(
                onClick = onExpandCalendar,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text("⊞ Mois", style = MaterialTheme.typography.labelMedium)
            }
        }
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(days) { day ->
                DayPill(
                    date       = day,
                    isToday    = day == today,
                    isSelected = day == selectedDate,
                    hasEvent   = day in eventDates,
                    onClick    = { onDateSelected(day) }
                )
            }
        }
    }
}

@Composable
private fun DayPill(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    hasEvent: Boolean,
    onClick: () -> Unit
) {
    val gradA = MaterialTheme.appColors.gradA
    val bgModifier = when {
        isSelected -> Modifier.background(brush = gradA)
        isToday    -> Modifier.background(MaterialTheme.colorScheme.primaryContainer)
        else       -> Modifier.background(MaterialTheme.colorScheme.surface)
    }
    val textColor = when {
        isSelected -> Color.White
        isToday    -> MaterialTheme.colorScheme.primary
        else       -> MaterialTheme.colorScheme.onSurface
    }
    val labelColor = when {
        isSelected -> Color.White.copy(alpha = 0.7f)
        isToday    -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        else       -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val dotColor = when {
        isSelected || isToday -> Color.White.copy(alpha = 0.8f)
        else                  -> MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier
            .width(46.dp)
            .height(68.dp)
            .clip(AppShapes.Pill)
            .then(bgModifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text  = date.dayOfWeek.name.take(3),
                style = MaterialTheme.typography.labelSmall,
                color = labelColor
            )
            Text(
                text  = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = textColor
            )
            // Always reserve space for the dot to keep pill height stable
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(AppShapes.Pill)
                    .then(if (hasEvent) Modifier.background(dotColor) else Modifier)
            )
        }
    }
}

// ── Month calendar bottom sheet ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthCalendarSheet(
    events: List<Event>,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val today      = Clock.System.todayIn(TimeZone.currentSystemDefault())
    var monthStart by remember {
        mutableStateOf(LocalDate(today.year, today.monthNumber, 1))
    }
    val eventDates = events.map { it.startDate.date }.toSet()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Month navigation
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = { monthStart = monthStart.minus(1, DateTimeUnit.MONTH) }) {
                    Text("‹", fontSize = 22.sp)
                }
                Text(
                    text      = monthStart.formatMonth(),
                    style     = MaterialTheme.typography.titleMedium,
                    modifier  = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                TextButton(onClick = { monthStart = monthStart.plus(1, DateTimeUnit.MONTH) }) {
                    Text("›", fontSize = 22.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Day-of-week header (Monday first)
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("L", "M", "M", "J", "V", "S", "D").forEach { label ->
                    Text(
                        text      = label,
                        modifier  = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style     = MaterialTheme.typography.labelSmall,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Grid
            val daysInMonth   = monthStart.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).dayOfMonth
            val firstDayOffset = monthStart.dayOfWeek.isoDayNumber - 1 // 0=Mon, 6=Sun
            val totalCells    = firstDayOffset + daysInMonth
            val rows          = (totalCells + 6) / 7

            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - firstDayOffset + 1
                        if (dayNumber < 1 || dayNumber > daysInMonth) {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            val date = LocalDate(monthStart.year, monthStart.monthNumber, dayNumber)
                            CalendarGridDay(
                                date       = date,
                                isToday    = date == today,
                                isSelected = date == selectedDate,
                                hasEvent   = date in eventDates,
                                onClick    = { onDateSelected(date) },
                                modifier   = Modifier.weight(1f)
                            )
                        }
                    }
                }
                if (row < rows - 1) Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun CalendarGridDay(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    hasEvent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradA = MaterialTheme.appColors.gradA
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(3.dp)
            .clip(AppShapes.Pill)
            .then(when {
                isSelected -> Modifier.background(brush = gradA)
                isToday    -> Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                else       -> Modifier
            })
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text       = date.dayOfMonth.toString(),
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color      = when {
                    isSelected -> Color.White
                    isToday    -> MaterialTheme.colorScheme.primary
                    else       -> MaterialTheme.colorScheme.onSurface
                }
            )
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(AppShapes.Pill)
                    .then(if (hasEvent)
                        Modifier.background(if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary)
                    else Modifier)
            )
        }
    }
}

// ── Event card ────────────────────────────────────────────────────────────────

@Composable
private fun EventCard(event: Event, isOwner: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape    = AppShapes.Card,
        border   = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
    ) {
        // Accent bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(brush = MaterialTheme.appColors.gradA)
        )
        Column(modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 16.dp)) {
            Row(
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier             = Modifier.fillMaxWidth()
            ) {
                Text(
                    text     = event.title,
                    style    = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = AppShapes.Pill,
                    color = if (isOwner) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text     = if (isOwner) "Organisateur" else "Invité",
                        style    = MaterialTheme.typography.labelMedium,
                        color    = if (isOwner) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            MetaRow(label = event.startDate.formatDisplay())
            event.location?.let { Spacer(Modifier.height(6.dp)); MetaRow(label = it) }
        }
    }
}

@Composable
private fun MetaRow(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(AppShapes.Pill)
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text     = label,
            style    = MaterialTheme.typography.bodyMedium,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(filtered: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(if (filtered) "📅" else "🎉", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text  = if (filtered) "Aucun événement ce jour" else "Aucun événement pour l'instant",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text  = if (filtered) "Sélectionne une autre date ou crée un événement" else "Crée ton premier événement avec le bouton +",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Formatting ────────────────────────────────────────────────────────────────

@Suppress("DEPRECATION")
private fun kotlinx.datetime.LocalDateTime.formatDisplay(): String {
    val d   = dayOfMonth.toString().padStart(2, '0')
    val m   = monthNumber.toString().padStart(2, '0')
    val h   = hour.toString().padStart(2, '0')
    val min = minute.toString().padStart(2, '0')
    return "$d/$m/$year à $h:$min"
}

private fun LocalDate.formatShort(): String {
    val months = listOf("jan", "fév", "mar", "avr", "mai", "juin",
                        "juil", "août", "sep", "oct", "nov", "déc")
    return "$dayOfMonth ${months[monthNumber - 1]}"
}

private fun LocalDate.formatMonth(): String {
    val months = listOf("Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
                        "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre")
    return "${months[monthNumber - 1]} $year"
}