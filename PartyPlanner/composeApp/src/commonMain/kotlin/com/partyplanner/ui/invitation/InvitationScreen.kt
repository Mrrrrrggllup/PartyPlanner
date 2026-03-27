package com.partyplanner.ui.invitation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.partyplanner.domain.model.InvitationStatus
import com.partyplanner.domain.model.InviteInfo
import com.partyplanner.presentation.invitation.InvitationComponent
import com.partyplanner.presentation.invitation.InvitationState
import com.partyplanner.ui.theme.AppShapes
import com.partyplanner.ui.theme.appColors

@Composable
fun InvitationScreen(component: InvitationComponent) {
    val state by component.state.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val s = state) {
            is InvitationState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            is InvitationState.Error -> Box(
                Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(s.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = component::onBack) { Text("Retour") }
                }
            }
            is InvitationState.Success -> InvitationContent(
                info        = s.info,
                isSubmitting = s.isSubmitting,
                onRsvp      = component::onRsvp,
                onBack      = component::onBack,
            )
        }
    }
}

@Composable
private fun InvitationContent(
    info: InviteInfo,
    isSubmitting: Boolean,
    onRsvp: (InvitationStatus) -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        InvitationHero(title = info.title, onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Event info
            EventInfoRow(label = "Organisé par", value = info.organizerName)
            EventInfoRow(label = "Date", value = info.startDate.formatDisplay())
            info.endDate?.let { EventInfoRow(label = "Jusqu'au", value = it.formatDisplay()) }
            info.location?.let { EventInfoRow(label = "Lieu", value = it) }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            if (info.isOwner) {
                // Organizer viewing their own invite link
                Text(
                    text = "Vous êtes l'organisateur de cet événement.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Retour")
                }
            } else {
                // Current status banner
                info.currentStatus?.let { status ->
                    StatusBanner(status)
                    Spacer(Modifier.height(4.dp))
                }

                Text(
                    text = if (info.currentStatus == null) "Serez-vous présent ?" else "Modifier votre réponse",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                // RSVP buttons
                RsvpButtons(isSubmitting = isSubmitting, onRsvp = onRsvp)
            }
        }
    }
}

@Composable
private fun InvitationHero(title: String, onBack: () -> Unit) {
    val gradA = MaterialTheme.appColors.gradA
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(brush = gradA)
            .statusBarsPadding()
            .height(200.dp)
    ) {
        Text(
            text = "🎉",
            fontSize = 120.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp)
                .alpha(0.15f)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(36.dp)
                .clip(AppShapes.ActionIcon)
                .background(Color.White.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                Text("←", color = Color.White, style = MaterialTheme.typography.titleLarge)
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.displayMedium,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
        )
    }
}

@Composable
private fun EventInfoRow(label: String, value: String) {
    Column {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatusBanner(status: InvitationStatus) {
    val (emoji, label, color) = when (status) {
        InvitationStatus.ACCEPTED -> Triple("✅", "Vous avez accepté", MaterialTheme.colorScheme.primary)
        InvitationStatus.DECLINED -> Triple("❌", "Vous avez décliné", MaterialTheme.colorScheme.error)
        InvitationStatus.MAYBE    -> Triple("🤔", "Vous avez répondu peut-être", MaterialTheme.colorScheme.secondary)
        InvitationStatus.PENDING  -> Triple("⏳", "En attente de réponse", MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.1f), AppShapes.Card)
            .padding(12.dp)
    ) {
        Text(emoji, style = MaterialTheme.typography.bodyLarge)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

@Composable
private fun RsvpButtons(isSubmitting: Boolean, onRsvp: (InvitationStatus) -> Unit) {
    if (isSubmitting) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        RsvpButton(
            emoji = "✅",
            label = "J'y serai !",
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            onClick = { onRsvp(InvitationStatus.ACCEPTED) }
        )
        RsvpButton(
            emoji = "🤔",
            label = "Peut-être",
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = { onRsvp(InvitationStatus.MAYBE) }
        )
        RsvpButton(
            emoji = "❌",
            label = "Je ne pourrai pas",
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            onClick = { onRsvp(InvitationStatus.DECLINED) }
        )
    }
}

@Composable
private fun RsvpButton(
    emoji: String,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.Button,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor   = contentColor,
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Text(emoji, style = MaterialTheme.typography.bodyLarge)
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Suppress("DEPRECATION")
private fun kotlinx.datetime.LocalDateTime.formatDisplay(): String {
    val d   = dayOfMonth.toString().padStart(2, '0')
    val m   = monthNumber.toString().padStart(2, '0')
    val h   = hour.toString().padStart(2, '0')
    val min = minute.toString().padStart(2, '0')
    return "$d/$m/$year à $h:$min"
}
