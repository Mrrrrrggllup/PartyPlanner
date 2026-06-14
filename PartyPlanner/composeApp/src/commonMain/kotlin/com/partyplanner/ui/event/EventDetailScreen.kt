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
import com.partyplanner.presentation.event.InviteEmailResult
import com.partyplanner.domain.model.ItemCategory
import com.partyplanner.domain.model.ItemRequest
import partyplanner.composeapp.generated.resources.*
import com.partyplanner.presentation.event.EventDetailComponent
import com.partyplanner.presentation.event.EventDetailState
import com.partyplanner.ui.theme.AppShapes
import com.partyplanner.ui.theme.appColors
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private enum class DetailTab(val icon: String) {
    INVITES("👥"),
    ITEMS("🛒"),
    CHAT("💬"),
    COVOIT("🚗"),
}

private val DetailTab.localizedLabel: String
    @Composable get() = when (this) {
        DetailTab.INVITES -> stringResource(Res.string.detail_tab_guests)
        DetailTab.ITEMS   -> stringResource(Res.string.detail_tab_items)
        DetailTab.CHAT    -> stringResource(Res.string.detail_tab_chat)
        DetailTab.COVOIT  -> stringResource(Res.string.detail_tab_carpool)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(component: EventDetailComponent) {
    val state by component.state.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(DetailTab.INVITES) }
    var inviteEmail by remember { mutableStateOf("") }
    var showAddItemRequestSheet by remember { mutableStateOf(false) }
    var showAddItemBroughtSheet by remember { mutableStateOf(false) }
    var showCreateCarpoolSheet by remember { mutableStateOf(false) }
    var joinCarpoolOfferId by remember { mutableStateOf<Int?>(null) }
    var editCarpoolOffer by remember { mutableStateOf<CarpoolOffer?>(null) }

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
                        title        = s.event.title,
                        subtitle     = buildSubtitle(s.event),
                        onBack       = component::onBack,
                        onEdit       = if (s.isOwner) component::onEdit else null,
                        guestSummary = run {
                            val confirmed = s.invitations.count { it.status == InvitationStatus.ACCEPTED }
                            val total     = s.invitations.size
                            if (total > 0) "👥 $confirmed/$total présents" else null
                        },
                    )
                    if (!s.isOwner) {
                        QuickRsvpBar(
                            status  = s.currentUserInvitationStatus,
                            onRsvp  = component::onRsvp,
                        )
                    }
                    PullToRefreshBox(
                        isRefreshing = s.isRefreshing,
                        onRefresh    = component::onRefresh,
                        modifier     = Modifier.weight(1f),
                    ) {
                        if (selectedTab == DetailTab.CHAT) {
                            LaunchedEffect(Unit) { component.onChatRead() }
                            ChatTabLayout(
                                messages      = s.chatMessages,
                                currentUserId = s.currentUserId,
                                onSend        = component::onSendMessage,
                                modifier      = Modifier.fillMaxSize(),
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 104.dp)
                            ) {
                                item {
                                    StatsRow(
                                        confirmed  = s.invitations.count { it.status == InvitationStatus.ACCEPTED },
                                        totalItems = s.items.requests.size + s.items.brought.size,
                                        unreadChat = s.unreadChatCount,
                                        covoits    = s.carpoolOffers.size,
                                        onTabClick = { selectedTab = it },
                                        modifier   = Modifier.padding(16.dp)
                                    )
                                }
                                item { Spacer(Modifier.height(4.dp)) }

                                when (selectedTab) {
                                    DetailTab.INVITES -> {
                                        // RSVP banner — non-owner seulement
                                        if (!s.isOwner) {
                                            item {
                                                RsvpBanner(
                                                    status   = s.currentUserInvitationStatus,
                                                    onRsvp   = component::onRsvp,
                                                    modifier = Modifier.padding(horizontal = 16.dp)
                                                )
                                            }
                                            item { Spacer(Modifier.height(12.dp)) }
                                        }
                                        // Invite by email + deep link — owner seulement
                                        if (s.isOwner) {
                                            s.event.inviteToken?.let { token ->
                                                item {
                                                    InviteButton(
                                                        token = token,
                                                        modifier = Modifier.padding(horizontal = 16.dp)
                                                    )
                                                }
                                                item { Spacer(Modifier.height(8.dp)) }
                                            }
                                            if (s.inviteSuggestions.isNotEmpty()) {
                                                item {
                                                    InviteSuggestions(
                                                        suggestions = s.inviteSuggestions,
                                                        onInvite    = component::onInviteByUserId,
                                                        modifier    = Modifier.padding(horizontal = 16.dp)
                                                    )
                                                }
                                                item { Spacer(Modifier.height(4.dp)) }
                                            }
                                            item {
                                                InviteByEmailRow(
                                                    email    = inviteEmail,
                                                    onEmail  = { inviteEmail = it },
                                                    onInvite = {
                                                        component.onInviteByEmail(inviteEmail.trim())
                                                        inviteEmail = ""
                                                    },
                                                    modifier = Modifier.padding(horizontal = 16.dp)
                                                )
                                            }
                                            s.inviteEmailResult?.let { result ->
                                                item {
                                                    InviteResultChip(
                                                        result   = result,
                                                        onDismiss = component::onDismissInviteResult,
                                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                            item { Spacer(Modifier.height(8.dp)) }
                                        }
                                        if (s.invitations.isEmpty()) {
                                            item {
                                                Box(
                                                    Modifier.fillMaxWidth().padding(40.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = stringResource(Res.string.detail_guests_empty),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        } else {
                                            item {
                                                Text(
                                                    text = stringResource(Res.string.detail_guests_count, s.invitations.size),
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
                                                        stringResource(Res.string.detail_btn_delete_event),
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
                                            onEdit        = { offer -> editCarpoolOffer = offer },
                                        )
                                    }
                                    else -> {}
                                }
                            }
                        }

                        DetailTabBar(
                            selected = selectedTab,
                            onSelect = { tab ->
                                if (tab == DetailTab.CHAT) component.onChatRead()
                                else if (selectedTab == DetailTab.CHAT) component.onChatLeft()
                                selectedTab = tab
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding(),
                            badgeCounts = mapOf(
                                DetailTab.CHAT   to s.unreadChatCount,
                                DetailTab.ITEMS  to s.items.requests.count { !it.isFulfilled },
                                DetailTab.INVITES to if (s.isOwner) s.invitations.count { it.status == InvitationStatus.PENDING } else 0,
                                DetailTab.COVOIT to s.carpoolOffers.count { it.seatsRemaining > 0 },
                            ),
                        )
                    }
                }
            }
        }
    }

    val categories = (state as? EventDetailState.Success)?.categories ?: emptyList()

    if (showAddItemRequestSheet) {
        AddItemSheet(
            title      = stringResource(Res.string.detail_items_sheet_need_title),
            hint       = stringResource(Res.string.detail_items_sheet_need_hint),
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
            title      = stringResource(Res.string.detail_items_sheet_brought_title),
            hint       = stringResource(Res.string.detail_items_sheet_brought_hint),
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

    editCarpoolOffer?.let { offer ->
        EditCarpoolSheet(
            offer     = offer,
            onConfirm = { seats, departurePoint, notes ->
                component.onUpdateCarpoolOffer(offer.id, seats, departurePoint, notes)
                editCarpoolOffer = null
            },
            onDismiss = { editCarpoolOffer = null }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(Res.string.detail_dialog_delete_title)) },
            text  = { Text(stringResource(Res.string.detail_dialog_delete_text)) },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; component.onDelete() }) {
                    Text(stringResource(Res.string.common_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(Res.string.common_cancel))
                }
            }
        )
    }

    val deleteError = (state as? EventDetailState.Success)?.deleteError
    if (deleteError != null) {
        AlertDialog(
            onDismissRequest = { component.onDismissDeleteError() },
            title = { Text("Erreur") },
            text  = { Text(deleteError) },
            confirmButton = {
                TextButton(onClick = { component.onDismissDeleteError() }) {
                    Text(stringResource(Res.string.common_ok))
                }
            }
        )
    }
}

// ── Hero ───────────────────────────────────────────────────────────────────────

@Composable
private fun DetailHero(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    guestSummary: String? = null,
    onEdit: (() -> Unit)? = null,
) {
    val gradA = MaterialTheme.appColors.gradA
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(brush = gradA)
            .statusBarsPadding()
            .height(140.dp)
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
                .padding(start = 20.dp, bottom = 20.dp)
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
        if (onEdit != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(36.dp)
                    .clip(AppShapes.ActionIcon)
                    .background(Color.White.copy(alpha = 0.25f))
                    .border(1.dp, Color.White.copy(alpha = 0.4f), AppShapes.ActionIcon)
                    .clickable { onEdit() },
                contentAlignment = Alignment.Center
            ) {
                Text("✏️", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
            if (guestSummary != null) {
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(AppShapes.Pill)
                        .background(Color.White.copy(alpha = 0.20f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = guestSummary,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ── Stats row (raccourcis cliquables vers les onglets) ────────────────────────

@Composable
private fun StatsRow(
    confirmed: Int,
    totalItems: Int,
    unreadChat: Int,
    covoits: Int,
    onTabClick: (DetailTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val gradA = MaterialTheme.appColors.gradA
    val gradB = MaterialTheme.appColors.gradB
    val gradC = MaterialTheme.appColors.gradC
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatTile(
            icon = "👥", value = "$confirmed",
            label = stringResource(Res.string.detail_stat_confirmed),
            gradient = gradA, onClick = { onTabClick(DetailTab.INVITES) },
            modifier = Modifier.weight(1f)
        )
        StatTile(
            icon = "🛒", value = "$totalItems",
            label = stringResource(Res.string.detail_tab_items),
            gradient = gradC, onClick = { onTabClick(DetailTab.ITEMS) },
            modifier = Modifier.weight(1f)
        )
        StatTile(
            icon = "💬", value = if (unreadChat > 0) "$unreadChat" else "·",
            label = stringResource(Res.string.detail_tab_chat),
            gradient = gradB, onClick = { onTabClick(DetailTab.CHAT) },
            modifier = Modifier.weight(1f)
        )
        StatTile(
            icon = "🚗", value = "$covoits",
            label = stringResource(Res.string.detail_tab_carpool),
            gradient = gradA, onClick = { onTabClick(DetailTab.COVOIT) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatTile(
    icon: String,
    value: String,
    label: String,
    gradient: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier,
        shape = AppShapes.Card,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 18.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(brush = gradient),
                textAlign = TextAlign.Center
            )
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1
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
    badgeCounts: Map<DetailTab, Int> = emptyMap(),
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
                    val isActive  = tab == selected
                    val badgeCount = badgeCounts[tab] ?: 0
                    Column(
                        modifier = Modifier
                            .clickable { onSelect(tab) }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        BadgedBox(
                            badge = {
                                if (badgeCount > 0) Badge {
                                    Text(if (badgeCount > 9) "9+" else badgeCount.toString())
                                }
                            }
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
                        }
                        Text(
                            text = tab.localizedLabel,
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

// ── Quick RSVP bar (non-owner, always visible under hero) ────────────────────

@Composable
private fun QuickRsvpBar(
    status: InvitationStatus?,
    onRsvp: (InvitationStatus) -> Unit,
) {
    val gradA = MaterialTheme.appColors.gradA
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text  = "Tu viens ?",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        listOf(
            InvitationStatus.ACCEPTED to "✅",
            InvitationStatus.MAYBE    to "🤔",
            InvitationStatus.DECLINED to "❌",
        ).forEach { (s, emoji) ->
            val isSelected = status == s
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(AppShapes.ActionIcon)
                    .then(
                        if (isSelected) Modifier.background(brush = gradA)
                        else Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    .clickable { onRsvp(s) },
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 16.sp)
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
}

// ── RSVP banner (non-owner) ───────────────────────────────────────────────────

@Composable
private fun RsvpBanner(
    status: InvitationStatus?,
    onRsvp: (InvitationStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gradA = MaterialTheme.appColors.gradA
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.Card)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.5.dp, MaterialTheme.colorScheme.outline, AppShapes.Card)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text  = stringResource(Res.string.detail_rsvp_title),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                InvitationStatus.ACCEPTED to stringResource(Res.string.invitation_rsvp_accept),
                InvitationStatus.MAYBE    to stringResource(Res.string.invitation_rsvp_maybe),
                InvitationStatus.DECLINED to stringResource(Res.string.invitation_rsvp_decline),
            ).forEach { (s, label) ->
                val isSelected = status == s
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(AppShapes.TextField)
                        .then(
                            if (isSelected) Modifier.background(brush = gradA)
                            else Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        .clickable { onRsvp(s) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

// ── Invite suggestions (owner) ────────────────────────────────────────────────

@Composable
private fun InviteSuggestions(
    suggestions: List<com.partyplanner.domain.model.UserSuggestion>,
    onInvite: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gradA = MaterialTheme.appColors.gradA
    LazyRow(
        modifier            = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(suggestions, key = { it.id }) { user ->
            val initial = user.displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            Column(
                modifier              = Modifier.clickable { onInvite(user.id) },
                horizontalAlignment   = Alignment.CenterHorizontally,
                verticalArrangement   = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier           = Modifier
                        .size(44.dp)
                        .clip(AppShapes.Avatar)
                        .background(brush = gradA),
                    contentAlignment   = Alignment.Center,
                ) {
                    Text(initial, color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    text     = user.displayName.split(" ").first(),
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

// ── Invite by email (owner) ───────────────────────────────────────────────────

@Composable
private fun InviteByEmailRow(
    email: String,
    onEmail: (String) -> Unit,
    onInvite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value         = email,
            onValueChange = onEmail,
            label         = { Text(stringResource(Res.string.detail_invite_email_hint)) },
            modifier      = Modifier.weight(1f),
            shape         = AppShapes.TextField,
            singleLine    = true,
        )
        Box(
            modifier = Modifier
                .size(width = 72.dp, height = 56.dp)
                .clip(AppShapes.TextField)
                .then(
                    if (email.contains('@')) Modifier.background(brush = MaterialTheme.appColors.gradA)
                    else Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                )
                .clickable(enabled = email.contains('@'), onClick = onInvite),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text  = stringResource(Res.string.detail_invite_email_btn),
                style = MaterialTheme.typography.labelLarge,
                color = if (email.contains('@')) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InviteResultChip(
    result: InviteEmailResult,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (bg, text) = when (result) {
        is InviteEmailResult.Success -> MaterialTheme.colorScheme.primaryContainer to
            stringResource(Res.string.detail_invite_success, result.userName)
        is InviteEmailResult.Error   -> MaterialTheme.colorScheme.errorContainer to result.message
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.TextField)
            .background(bg)
            .clickable(onClick = onDismiss)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
        Text("✕", style = MaterialTheme.typography.labelMedium)
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
    @Suppress("DEPRECATION")
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
                @Suppress("DEPRECATION")
                clipboardManager.setText(AnnotatedString("${com.partyplanner.util.BASE_URL}/i/$token"))
                copied = true
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text  = if (copied) stringResource(Res.string.detail_invite_copied)
                    else stringResource(Res.string.detail_invite_copy),
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
            Text(stringResource(Res.string.detail_items_add_request), color = Color.White, style = MaterialTheme.typography.labelLarge)
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
                text  = stringResource(Res.string.detail_items_add_brought),
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
                text     = stringResource(Res.string.detail_items_needed_header, eventItems.requests.size),
                style    = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
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
                text     = stringResource(Res.string.detail_items_brought_header, eventItems.brought.size),
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
                    text  = stringResource(Res.string.detail_items_empty),
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
    val by = stringResource(Res.string.common_by)
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
                item.assignedToName?.let { add("$by $it") }
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
                Text(stringResource(Res.string.detail_items_quantity), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
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
                    text  = stringResource(Res.string.common_add),
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
        Text(stringResource(Res.string.detail_carpool_offer_btn), color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

fun androidx.compose.foundation.lazy.LazyListScope.CarpoolTabContent(
    offers: List<CarpoolOffer>,
    currentUserId: Int,
    isOwner: Boolean,
    onJoin: (Int) -> Unit,
    onLeave: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onEdit: (CarpoolOffer) -> Unit,
) {
    if (offers.isEmpty()) {
        item {
            Box(
                Modifier.fillMaxWidth().padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = stringResource(Res.string.detail_carpool_empty),
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
            offer       = offer,
            isDriver    = isDriver,
            isPassenger = isPassenger,
            canJoin     = canJoin,
            canDelete   = canDelete,
            onJoin      = { onJoin(offer.id) },
            onLeave     = { onLeave(offer.id) },
            onDelete    = { onDelete(offer.id) },
            onEdit      = if (isDriver) { { onEdit(offer) } } else null,
            modifier    = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
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
    onEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val meLabel = stringResource(Res.string.detail_carpool_me)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.Card)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.5.dp, MaterialTheme.colorScheme.outline, AppShapes.Card)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
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
                                text  = meLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        text  = stringResource(Res.string.detail_carpool_seats_count, offer.seatsRemaining, offer.seatsAvailable),
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

        val activePassengers = offer.passengers.filter { it.pickupPoint != null || true }
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

        when {
            isDriver && onEdit != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(AppShapes.TextField)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable(onClick = onEdit),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✏️ Modifier mon offre", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium)
                }
            }
            isDriver -> Unit
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
                    Text(stringResource(Res.string.detail_carpool_leave_btn), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
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
                    Text(stringResource(Res.string.detail_carpool_join_btn), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
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
                    Text(stringResource(Res.string.detail_carpool_full), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
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
            Text(stringResource(Res.string.detail_carpool_sheet_title), style = MaterialTheme.typography.titleMedium)

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.detail_carpool_seats), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
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
                label         = { Text(stringResource(Res.string.detail_carpool_departure)) },
                modifier      = Modifier.fillMaxWidth(),
                shape         = AppShapes.TextField,
                singleLine    = true,
            )

            OutlinedTextField(
                value         = notes,
                onValueChange = { notes = it },
                label         = { Text(stringResource(Res.string.detail_carpool_notes)) },
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
                Text(stringResource(Res.string.detail_carpool_create), color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditCarpoolSheet(
    offer: CarpoolOffer,
    onConfirm: (Int, String?, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var seats by remember { mutableStateOf(offer.seatsAvailable) }
    var departurePoint by remember { mutableStateOf(offer.departurePoint ?: "") }
    var notes by remember { mutableStateOf(offer.notes ?: "") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Modifier mon offre", style = MaterialTheme.typography.titleMedium)

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.detail_carpool_seats), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
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
                label         = { Text(stringResource(Res.string.detail_carpool_departure)) },
                modifier      = Modifier.fillMaxWidth(),
                shape         = AppShapes.TextField,
                singleLine    = true,
            )

            OutlinedTextField(
                value         = notes,
                onValueChange = { notes = it },
                label         = { Text(stringResource(Res.string.detail_carpool_notes)) },
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
                Text("Enregistrer", color = Color.White, style = MaterialTheme.typography.labelLarge)
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
            Text(stringResource(Res.string.detail_carpool_join_title), style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value         = pickupPoint,
                onValueChange = { pickupPoint = it },
                label         = { Text(stringResource(Res.string.detail_carpool_pickup)) },
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
                Text(stringResource(Res.string.detail_carpool_join_btn), color = Color.White, style = MaterialTheme.typography.labelLarge)
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
                            text  = stringResource(Res.string.detail_chat_empty),
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
                placeholder   = { Text(stringResource(Res.string.detail_chat_placeholder), style = MaterialTheme.typography.bodyMedium) },
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
    val local = dt.toInstant(kotlinx.datetime.TimeZone.UTC)
        .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
    val h = local.hour.toString().padStart(2, '0')
    val m = local.minute.toString().padStart(2, '0')
    return "$h:$m"
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
