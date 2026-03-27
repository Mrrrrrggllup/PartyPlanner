package com.partyplanner.ui.event

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.partyplanner.domain.model.CarpoolOffer
import com.partyplanner.domain.model.ChatMessage
import com.partyplanner.domain.model.Event
import com.partyplanner.domain.model.EventItems
import com.partyplanner.domain.model.Invitation
import com.partyplanner.domain.model.InvitationStatus
import com.partyplanner.domain.model.ItemBrought
import com.partyplanner.domain.model.ItemCategory
import com.partyplanner.domain.model.ItemRequest
import com.partyplanner.presentation.event.EventDetailComponent
import com.partyplanner.presentation.event.EventDetailState
import com.partyplanner.ui.theme.AppShapes
import com.partyplanner.ui.theme.appColors

private enum class DetailTab(val label: String, val icon: String) {
    INVITES("Invités", "👥"),
    ITEMS("Items", "🛒"),
    CHAT("Chat", "💬"),
    COVOIT("Covoit", "🚗"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(component: EventDetailComponent) {
    val state by component.state.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(DetailTab.INVITES) }
    var showAddItemRequestSheet by remember { mutableStateOf(false) }
    var showAddItemBroughtSheet by remember { mutableStateOf(false) }
    var showCreateCarpoolSheet by remember { mutableStateOf(false) }
    var joinCarpoolOfferId by remember { mutableStateOf<Int?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val s = state) {
            is EventDetailState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            is EventDetailState.Error -> Box(
                Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(s.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
            is EventDetailState.Success -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    DetailHero(
                        title    = s.event.title,
                        subtitle = buildSubtitle(s.event),
                        onBack   = component::onBack,
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        if (selectedTab == DetailTab.CHAT) {
                            ChatTabLayout(
                                messages      = s.chatMessages,
                                currentUserId = s.currentUserId,
                                onSend        = component::onSendMessage,
                                modifier      = Modifier.fillMaxSize(),
                            )
                        } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            item {
                                StatsRow(
                                    total     = s.invitations.size,
                                    confirmed = s.invitations.count { it.status == InvitationStatus.ACCEPTED },
                                    covoits   = s.carpoolOffers.size,
                                    modifier  = Modifier.padding(16.dp)
                                )
                            }
                            item { Spacer(Modifier.height(4.dp)) }

                            when (selectedTab) {
                                DetailTab.INVITES -> {
                                    if (s.isOwner) {
                                        s.event.inviteToken?.let { token ->
                                            item {
                                                InviteButton(
                                                    token = token,
                                                    modifier = Modifier.padding(horizontal = 16.dp)
                                                )
                                            }
                                            item { Spacer(Modifier.height(12.dp)) }
                                        }
                                    }
                                    if (s.invitations.isEmpty()) {
                                        item {
                                            Box(
                                                Modifier.fillMaxWidth().padding(40.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Aucun invité pour l'instant",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    } else {
                                        item {
                                            Text(
                                                text = "INVITÉS (${s.invitations.size})",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                            )
                                        }
                                        items(s.invitations) { inv ->
                                            GuestRow(
                                                invitation = inv,
                                                modifier = Modifier
                                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    if (s.isOwner) {
                                        item { Spacer(Modifier.height(16.dp)) }
                                        item {
                                            OutlinedButton(
                                                onClick = { showDeleteDialog = true },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp),
                                                shape = AppShapes.TextField,
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.error
                                                ),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.5.dp,
                                                    MaterialTheme.colorScheme.error
                                                )
                                            ) {
                                                Text(
                                                    "Supprimer l'événement",
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                            }
                                        }
                                    }
                                }
                                DetailTab.ITEMS -> {
                                    item {
                                        ItemsTabHeader(
                                            onAddRequest = { showAddItemRequestSheet = true },
                                            onAddBrought = { showAddItemBroughtSheet = true },
                                            modifier     = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                    item { Spacer(Modifier.height(8.dp)) }
                                    ItemsTabContent(
                                        eventItems      = s.items,
                                        isOwner         = s.isOwner,
                                        currentUserId   = s.currentUserId,
                                        onFulfill       = component::onFulfillItemRequest,
                                        onDeleteRequest = component::onDeleteItemRequest,
                                        onDeleteBrought = component::onDeleteItemBrought,
                                    )
                                }
                                DetailTab.CHAT -> item {
                                    PlaceholderSection(
                                        emoji    = "💬",
                                        title    = "Chat de groupe",
                                        subtitle = "Disponible en Phase 4"
                                    )
                                }
                                DetailTab.COVOIT -> {
                                    item {
                                        CarpoolTabHeader(
                                            onCreateOffer = { showCreateCarpoolSheet = true },
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                    item { Spacer(Modifier.height(8.dp)) }
                                    CarpoolTabContent(
                                        offers        = s.carpoolOffers,
                                        currentUserId = s.currentUserId,
                                        isOwner       = s.isOwner,
                                        onJoin        = { offerId -> joinCarpoolOfferId = offerId },
                                        onLeave       = component::onLeaveCarpool,
                                        onDelete      = component::onDeleteCarpoolOffer,
                                    )
                                }
                                DetailTab.CHAT -> { /* handled above */ }
                            }
                        }
                        } // end else (non-chat tabs)

                        DetailTabBar(
                            selected = selectedTab,
                            onSelect = { selectedTab = it },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                        )
                    }
                }
            }
        }
    }

    val categories = (state as? EventDetailState.Success)?.categories ?: emptyList()

    if (showAddItemRequestSheet) {
        AddItemSheet(
            title      = "On a besoin de…",
            hint       = "Ex: chips, jus d'orange, serviettes…",
            categories = categories,
            onConfirm  = { label, qty, catId ->
                component.onAddItemRequest(label, qty, catId)
                showAddItemRequestSheet = false
            },
            onDismiss  = { showAddItemRequestSheet = false }
        )
    }

    if (showAddItemBroughtSheet) {
        AddItemSheet(
            title      = "Ce que j'apporte",
            hint       = "Ex: salade de fruits, sodas, plateau…",
            categories = categories,
            onConfirm  = { label, qty, catId ->
                component.onAddItemBrought(label, qty, catId)
                showAddItemBroughtSheet = false
            },
            onDismiss  = { showAddItemBroughtSheet = false }
        )
    }

    if (showCreateCarpoolSheet) {
        CreateCarpoolSheet(
            onConfirm = { seats, departurePoint, notes ->
                component.onCreateCarpoolOffer(seats, departurePoint, notes)
                showCreateCarpoolSheet = false
            },
            onDismiss = { showCreateCarpoolSheet = false }
        )
    }

    joinCarpoolOfferId?.let { offerId ->
        JoinCarpoolSheet(
            onConfirm = { pickupPoint ->
                component.onJoinCarpool(offerId, pickupPoint)
                joinCarpoolOfferId = null
            },
            onDismiss = { joinCarpoolOfferId = null }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer l'événement ?") },
            text  = { Text("Cette action est irréversible.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; component.onDelete() }) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Annuler") }
            }
        )
    }
}

// ── Hero ───────────────────────────────────────────────────────────────────────

@Composable
private fun DetailHero(title: String, subtitle: String, onBack: () -> Unit) {
    val gradA = MaterialTheme.appColors.gradA
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(brush = gradA)
            .statusBarsPadding()
            .height(200.dp)
    ) {
        Text(
            text = "🎊",
            fontSize = 120.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 8.dp, top = 4.dp)
                .alpha(0.15f)
        )
        Text(
            text = "🎶",
            fontSize = 60.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 44.dp)
                .alpha(0.12f)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(36.dp)
                .clip(AppShapes.ActionIcon)
                .background(Color.White.copy(alpha = 0.25f))
                .border(1.dp, Color.White.copy(alpha = 0.4f), AppShapes.ActionIcon),
            contentAlignment = Alignment.Center
        ) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                Text("←", color = Color.White, style = MaterialTheme.typography.titleLarge)
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, end = 24.dp, bottom = 20.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

// ── Stats row ─────────────────────────────────────────────────────────────────

@Composable
private fun StatsRow(total: Int, confirmed: Int, covoits: Int, modifier: Modifier = Modifier) {
    val gradA = MaterialTheme.appColors.gradA
    val gradB = MaterialTheme.appColors.gradB
    val gradC = MaterialTheme.appColors.gradC
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatTile(value = "$total",     label = "Invités",   gradient = gradA, modifier = Modifier.weight(1f))
        StatTile(value = "$confirmed", label = "Confirmés", gradient = gradC, modifier = Modifier.weight(1f))
        StatTile(value = "$covoits",   label = "Covoits",   gradient = gradB, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(value: String, label: String, gradient: Brush, modifier: Modifier = Modifier) {
    OutlinedCard(
        modifier = modifier,
        shape = AppShapes.Card,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(brush = gradient),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Bottom tab bar ────────────────────────────────────────────────────────────

@Composable
private fun DetailTabBar(
    selected: DetailTab,
    onSelect: (DetailTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                DetailTab.entries.forEach { tab ->
                    val isActive = tab == selected
                    Column(
                        modifier = Modifier
                            .clickable { onSelect(tab) }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(AppShapes.ActionIcon)
                                .then(
                                    if (isActive) Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(tab.icon, fontSize = 17.sp)
                        }
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isActive) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ── Guest row ─────────────────────────────────────────────────────────────────

@Composable
private fun GuestRow(invitation: Invitation, modifier: Modifier = Modifier) {
    val emoji = when (invitation.status) {
        InvitationStatus.ACCEPTED -> "✅"
        InvitationStatus.DECLINED -> "❌"
        InvitationStatus.MAYBE    -> "🤔"
        InvitationStatus.PENDING  -> "⏳"
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.Card)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.5.dp, MaterialTheme.colorScheme.outline, AppShapes.Card)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(invitation.userDisplayName, style = MaterialTheme.typography.bodyMedium)
        Text(emoji, style = MaterialTheme.typography.bodyMedium)
    }
}

// ── Invite button ─────────────────────────────────────────────────────────────

@Composable
private fun InviteButton(token: String, modifier: Modifier = Modifier) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(2000)
            copied = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(AppShapes.TextField)
            .background(
                if (copied) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .border(1.5.dp, MaterialTheme.colorScheme.primary, AppShapes.TextField)
            .clickable {
                clipboardManager.setText(AnnotatedString("partyplanner://invite/$token"))
                copied = true
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text  = if (copied) "✅ Lien copié !" else "🔗 Copier le lien d'invitation",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// ── Items tab ─────────────────────────────────────────────────────────────────

@Composable
private fun ItemsTabHeader(
    onAddRequest: () -> Unit,
    onAddBrought: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(AppShapes.TextField)
                .background(brush = MaterialTheme.appColors.gradA)
                .clickable(onClick = onAddRequest),
            contentAlignment = Alignment.Center
        ) {
            Text("+ Besoin", color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(AppShapes.TextField)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(onClick = onAddBrought),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text  = "J'apporte…",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

fun androidx.compose.foundation.lazy.LazyListScope.ItemsTabContent(
    eventItems: EventItems,
    isOwner: Boolean,
    currentUserId: Int,
    onFulfill: (Int) -> Unit,
    onDeleteRequest: (Int) -> Unit,
    onDeleteBrought: (Int) -> Unit,
) {
    if (eventItems.requests.isNotEmpty()) {
        item {
            Text(
                text     = "CE QU'IL MANQUE (${eventItems.requests.size})",
                style    = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        // Group by category — backend already sorts by categoryId ASC NULLS LAST
        val requestGroups = eventItems.requests.groupBy { it.categoryId }
        requestGroups.forEach { (catId, groupItems) ->
            val catIcon  = groupItems.first().categoryIcon
            val catLabel = groupItems.first().categoryLabel
            if (catIcon != null || catLabel != null) {
                item(key = "req-cat-$catId") {
                    CategoryHeader(icon = catIcon, label = catLabel ?: "")
                }
            }
            items(groupItems, key = { "req-${it.id}" }) { req ->
                ItemRequestRow(
                    item      = req,
                    isOwner   = isOwner,
                    onFulfill = { onFulfill(req.id) },
                    onDelete  = { onDeleteRequest(req.id) },
                    modifier  = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }

    if (eventItems.brought.isNotEmpty()) {
        item {
            Text(
                text     = "CE QU'ON APPORTE (${eventItems.brought.size})",
                style    = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        val broughtGroups = eventItems.brought.groupBy { it.categoryId }
        broughtGroups.forEach { (catId, groupItems) ->
            val catIcon  = groupItems.first().categoryIcon
            val catLabel = groupItems.first().categoryLabel
            if (catIcon != null || catLabel != null) {
                item(key = "brt-cat-$catId") {
                    CategoryHeader(icon = catIcon, label = catLabel ?: "")
                }
            }
            items(groupItems, key = { "brt-${it.id}" }) { brought ->
                ItemBroughtRow(
                    item      = brought,
                    canDelete = isOwner || brought.userId == currentUserId,
                    onDelete  = { onDeleteBrought(brought.id) },
                    modifier  = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }

    if (eventItems.requests.isEmpty() && eventItems.brought.isEmpty()) {
        item {
            Box(
                Modifier.fillMaxWidth().padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = "Aucun item pour l'instant",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CategoryHeader(icon: String?, label: String) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        icon?.let { Text(it, fontSize = 13.sp) }
        Text(
            text  = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        HorizontalDivider(
            modifier  = Modifier.weight(1f).padding(start = 4.dp),
            thickness = 1.dp,
            color     = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun ItemRequestRow(
    item: ItemRequest,
    isOwner: Boolean,
    onFulfill: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fulfilled = item.isFulfilled
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.Card)
            .background(
                if (fulfilled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surface
            )
            .border(1.5.dp, MaterialTheme.colorScheme.outline, AppShapes.Card)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Fulfill toggle (checkbox style)
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(AppShapes.ActionIcon)
                .then(
                    if (fulfilled) Modifier.background(brush = MaterialTheme.appColors.gradA)
                    else Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                )
                .clickable(onClick = onFulfill),
            contentAlignment = Alignment.Center
        ) {
            if (fulfilled) Text("✓", color = Color.White, style = MaterialTheme.typography.labelMedium)
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                item.categoryIcon?.let { Text(it, fontSize = 14.sp) }
                Text(
                    text  = item.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (fulfilled) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface,
                )
            }
            val sub = buildList {
                if (item.quantity > 1) add("×${item.quantity}")
                item.assignedToName?.let { add("par $it") }
            }.joinToString(" · ")
            if (sub.isNotEmpty()) {
                Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (isOwner) {
            TextButton(
                onClick = onDelete,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("✕", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ItemBroughtRow(
    item: ItemBrought,
    canDelete: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.Card)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.5.dp, MaterialTheme.colorScheme.outline, AppShapes.Card)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(item.categoryIcon ?: "🎒", fontSize = 18.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(item.label, style = MaterialTheme.typography.bodyMedium)
            val sub = buildList {
                if (item.quantity > 1) add("×${item.quantity}")
                add(item.userName)
            }.joinToString(" · ")
            Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (canDelete) {
            TextButton(
                onClick = onDelete,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("✕", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// ── Add item sheet ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddItemSheet(
    title: String,
    hint: String,
    categories: List<ItemCategory>,
    onConfirm: (String, Int, Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    var label by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf(1) }
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value         = label,
                onValueChange = { label = it },
                label         = { Text(hint) },
                modifier      = Modifier.fillMaxWidth(),
                shape         = AppShapes.TextField,
                singleLine    = true,
            )

            // Category picker
            if (categories.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories, key = { it.id }) { cat ->
                        val selected = cat.id == selectedCategoryId
                        Box(
                            modifier = Modifier
                                .clip(AppShapes.Pill)
                                .then(
                                    if (selected) Modifier.background(brush = MaterialTheme.appColors.gradA)
                                    else Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                                .clickable {
                                    selectedCategoryId = if (selected) null else cat.id
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text  = "${cat.icon ?: ""} ${cat.label}".trim(),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Quantité :", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { if (quantity > 1) quantity-- }) {
                    Text("−", style = MaterialTheme.typography.titleLarge)
                }
                Text("$quantity", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { quantity++ }) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(AppShapes.TextField)
                    .then(
                        if (label.isNotBlank()) Modifier.background(brush = MaterialTheme.appColors.gradA)
                        else Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    .clickable(enabled = label.isNotBlank()) { onConfirm(label.trim(), quantity, selectedCategoryId) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = "Ajouter",
                    color = if (label.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

// ── Carpool tab ───────────────────────────────────────────────────────────────

@Composable
private fun CarpoolTabHeader(onCreateOffer: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(AppShapes.TextField)
            .background(brush = MaterialTheme.appColors.gradA)
            .clickable(onClick = onCreateOffer),
        contentAlignment = Alignment.Center
    ) {
        Text("🚗 Proposer un trajet", color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

fun androidx.compose.foundation.lazy.LazyListScope.CarpoolTabContent(
    offers: List<CarpoolOffer>,
    currentUserId: Int,
    isOwner: Boolean,
    onJoin: (Int) -> Unit,
    onLeave: (Int) -> Unit,
    onDelete: (Int) -> Unit,
) {
    if (offers.isEmpty()) {
        item {
            Box(
                Modifier.fillMaxWidth().padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = "Aucun trajet proposé pour l'instant",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }
    items(offers, key = { it.id }) { offer ->
        val isDriver      = offer.driverId == currentUserId
        val isPassenger   = offer.passengers.any { it.passengerId == currentUserId }
        val canJoin       = !isDriver && !isPassenger && offer.seatsRemaining > 0
        val canDelete     = isDriver || isOwner
        CarpoolOfferCard(
            offer      = offer,
            isDriver   = isDriver,
            isPassenger = isPassenger,
            canJoin    = canJoin,
            canDelete  = canDelete,
            onJoin     = { onJoin(offer.id) },
            onLeave    = { onLeave(offer.id) },
            onDelete   = { onDelete(offer.id) },
            modifier   = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun CarpoolOfferCard(
    offer: CarpoolOffer,
    isDriver: Boolean,
    isPassenger: Boolean,
    canJoin: Boolean,
    canDelete: Boolean,
    onJoin: () -> Unit,
    onLeave: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.Card)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.5.dp, MaterialTheme.colorScheme.outline, AppShapes.Card)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Header row: driver name + seats
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🚗", fontSize = 18.sp)
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(offer.driverName, style = MaterialTheme.typography.bodyMedium)
                        if (isDriver) {
                            Text(
                                text  = "(moi)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        text  = "${offer.seatsRemaining}/${offer.seatsAvailable} places",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (offer.seatsRemaining == 0) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (canDelete) {
                TextButton(onClick = onDelete, contentPadding = PaddingValues(0.dp)) {
                    Text("✕", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Details
        offer.departurePoint?.let { point ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("📍", fontSize = 13.sp)
                Text(point, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        offer.notes?.let { note ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("💬", fontSize = 13.sp)
                Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Passengers list (active only)
        val activePassengers = offer.passengers.filter { it.pickupPoint != null || true }
            // All passengers returned are active (MATCHED), backend filters CANCELLED
        if (activePassengers.isNotEmpty()) {
            activePassengers.forEach { p ->
                Row(
                    modifier = Modifier.padding(start = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("👤", fontSize = 12.sp)
                    Text(p.passengerName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    p.pickupPoint?.let { pt ->
                        Text("· $pt", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Action button
        when {
            isDriver -> Unit // no action for driver
            isPassenger -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(AppShapes.TextField)
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .clickable(onClick = onLeave),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Je descends 🚪", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                }
            }
            canJoin -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(AppShapes.TextField)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(onClick = onJoin),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Je monte ! 🙋", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(AppShapes.TextField)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Complet", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateCarpoolSheet(
    onConfirm: (Int, String?, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var seats by remember { mutableStateOf(2) }
    var departurePoint by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Proposer un trajet", style = MaterialTheme.typography.titleMedium)

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Places disponibles :", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { if (seats > 1) seats-- }) {
                    Text("−", style = MaterialTheme.typography.titleLarge)
                }
                Text("$seats", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { seats++ }) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }

            OutlinedTextField(
                value         = departurePoint,
                onValueChange = { departurePoint = it },
                label         = { Text("Point de départ (optionnel)") },
                modifier      = Modifier.fillMaxWidth(),
                shape         = AppShapes.TextField,
                singleLine    = true,
            )

            OutlinedTextField(
                value         = notes,
                onValueChange = { notes = it },
                label         = { Text("Notes (optionnel)") },
                modifier      = Modifier.fillMaxWidth(),
                shape         = AppShapes.TextField,
                singleLine    = true,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(AppShapes.TextField)
                    .background(brush = MaterialTheme.appColors.gradA)
                    .clickable {
                        onConfirm(
                            seats,
                            departurePoint.trim().takeIf { it.isNotBlank() },
                            notes.trim().takeIf { it.isNotBlank() }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("Créer le trajet", color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JoinCarpoolSheet(
    onConfirm: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var pickupPoint by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Rejoindre le trajet", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value         = pickupPoint,
                onValueChange = { pickupPoint = it },
                label         = { Text("Point de prise en charge (optionnel)") },
                modifier      = Modifier.fillMaxWidth(),
                shape         = AppShapes.TextField,
                singleLine    = true,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(AppShapes.TextField)
                    .background(brush = MaterialTheme.appColors.gradA)
                    .clickable {
                        onConfirm(pickupPoint.trim().takeIf { it.isNotBlank() })
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("Je monte ! 🙋", color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// ── Chat tab ──────────────────────────────────────────────────────────────────

@Composable
private fun ChatTabLayout(
    messages: List<ChatMessage>,
    currentUserId: Int,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text  = "Aucun message pour l'instant.\nSoyez le premier ! 👋",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                items(messages, key = { it.id }) { msg ->
                    ChatBubble(message = msg, isMine = msg.senderId == currentUserId)
                }
            }
        }

        // Input bar (sits above the tab bar which overlaps from Box parent)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value         = inputText,
                onValueChange = { inputText = it },
                placeholder   = { Text("Message…", style = MaterialTheme.typography.bodyMedium) },
                modifier      = Modifier.weight(1f),
                shape         = AppShapes.TextField,
                singleLine    = true,
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(AppShapes.ActionIcon)
                    .then(
                        if (inputText.isNotBlank()) Modifier.background(brush = MaterialTheme.appColors.gradA)
                        else Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    .clickable(enabled = inputText.isNotBlank()) {
                        onSend(inputText.trim())
                        inputText = ""
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = "➤",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (inputText.isNotBlank()) Color.White
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // Space for the overlaid tab bar
        Spacer(Modifier.navigationBarsPadding().height(72.dp))
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, isMine: Boolean) {
    val gradA = MaterialTheme.appColors.gradA
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        if (!isMine) {
            Text(
                text     = message.senderName,
                style    = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    if (isMine) androidx.compose.foundation.shape.RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
                    else        androidx.compose.foundation.shape.RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
                )
                .then(
                    if (isMine) Modifier.background(brush = gradA)
                    else        Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text  = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text     = formatChatTime(message.createdAt),
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 2.dp)
        )
    }
}

@Suppress("DEPRECATION")
private fun formatChatTime(dt: kotlinx.datetime.LocalDateTime): String {
    val h = dt.hour.toString().padStart(2, '0')
    val m = dt.minute.toString().padStart(2, '0')
    return "$h:$m"
}

// ── Placeholder ───────────────────────────────────────────────────────────────

@Composable
private fun PlaceholderSection(emoji: String, title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(emoji, fontSize = 40.sp)
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Suppress("DEPRECATION")
private fun buildSubtitle(event: Event): String {
    val s = event.startDate
    val sb = StringBuilder()
    sb.append(s.dayOfMonth.toString().padStart(2, '0'))
    sb.append("/")
    sb.append(s.monthNumber.toString().padStart(2, '0'))
    sb.append(" à ")
    sb.append(s.hour.toString().padStart(2, '0'))
    sb.append("h")
    sb.append(s.minute.toString().padStart(2, '0'))
    event.endDate?.let { e ->
        sb.append(" → ")
        sb.append(e.hour.toString().padStart(2, '0'))
        sb.append("h")
        sb.append(e.minute.toString().padStart(2, '0'))
    }
    event.location?.let { sb.append(" · ").append(it) }
    return sb.toString()
}
