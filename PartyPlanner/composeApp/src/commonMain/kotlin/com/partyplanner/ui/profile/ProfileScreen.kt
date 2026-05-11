package com.partyplanner.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.partyplanner.generated.resources.*
import com.partyplanner.presentation.profile.ProfileComponent
import com.partyplanner.presentation.profile.ProfileState
import com.partyplanner.presentation.profile.ThemeMode
import com.partyplanner.ui.theme.AppShapes
import com.partyplanner.ui.theme.appColors
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProfileScreen(component: ProfileComponent) {
    val state by component.state.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val s = state) {
            is ProfileState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            is ProfileState.Success -> ProfileContent(
                displayName  = s.displayName,
                currentTheme = s.currentTheme,
                onThemeChange = component::onThemeChange,
                onLogout      = component::onLogout,
                onBack        = component::onBack,
            )
        }
    }
}

@Composable
private fun ProfileContent(
    displayName: String,
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        ProfileHero(displayName = displayName, onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column {
                Text(
                    text = stringResource(Res.string.profile_section_name),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                HorizontalDivider(modifier = Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.outline)
            }

            Column {
                Text(
                    text = stringResource(Res.string.profile_section_theme),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                ThemeSwitcher(current = currentTheme, onChange = onThemeChange)
            }

            Spacer(Modifier.weight(1f))

            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.TextField,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(Res.string.profile_btn_logout), style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(Res.string.profile_dialog_logout_title)) },
            text  = { Text(stringResource(Res.string.profile_dialog_logout_text)) },
            confirmButton = {
                TextButton(onClick = { showLogoutDialog = false; onLogout() }) {
                    Text(stringResource(Res.string.profile_dialog_logout_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(Res.string.common_cancel))
                }
            }
        )
    }
}

@Composable
private fun ProfileHero(displayName: String, onBack: () -> Unit) {
    val gradA = MaterialTheme.appColors.gradA
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(brush = gradA)
            .statusBarsPadding()
            .padding(bottom = 32.dp)
    ) {
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

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(top = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val initial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(AppShapes.Avatar)
                    .background(Color.White.copy(alpha = 0.25f))
                    .border(2.dp, Color.White.copy(alpha = 0.5f), AppShapes.Avatar),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    fontSize = 32.sp,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ThemeSwitcher(current: ThemeMode, onChange: (ThemeMode) -> Unit) {
    val options = listOf(
        ThemeMode.SYSTEM to stringResource(Res.string.profile_theme_system),
        ThemeMode.LIGHT  to stringResource(Res.string.profile_theme_light),
        ThemeMode.DARK   to stringResource(Res.string.profile_theme_dark),
    )
    val gradA = MaterialTheme.appColors.gradA

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (mode, label) ->
            val selected = mode == current
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(AppShapes.Pill)
                    .then(
                        if (selected) Modifier.background(brush = gradA)
                        else Modifier.border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, AppShapes.Pill)
                    )
                    .clickable { onChange(mode) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
