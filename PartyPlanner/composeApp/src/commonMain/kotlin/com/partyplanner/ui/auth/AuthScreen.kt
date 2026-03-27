package com.partyplanner.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.partyplanner.core.AppConfig
import com.partyplanner.presentation.auth.AuthComponent
import com.partyplanner.presentation.auth.AuthState
import com.partyplanner.ui.theme.AppShapes
import com.partyplanner.ui.theme.appColors

@Composable
fun AuthScreen(component: AuthComponent) {
    val state by component.state.collectAsState()
    var mode by remember { mutableStateOf(AuthMode.Login) }

    LaunchedEffect(mode) { component.resetState() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            AuthHero()

            Spacer(Modifier.height(24.dp))

            AuthTabSwitcher(
                mode = mode,
                onModeChange = { mode = it },
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(20.dp))

            when (mode) {
                AuthMode.Login -> LoginForm(
                    isLoading = state is AuthState.Loading,
                    error = (state as? AuthState.Error)?.message,
                    onSubmit = { email, password -> component.login(email, password) }
                )
                AuthMode.Register -> RegisterForm(
                    isLoading = state is AuthState.Loading,
                    error = (state as? AuthState.Error)?.message,
                    onSubmit = { email, password, displayName, phone ->
                        component.register(email, password, displayName, phone)
                    }
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ── Hero ──────────────────────────────────────────────────────────────────────

@Composable
private fun AuthHero() {
    val gradA = MaterialTheme.appColors.gradA
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(brush = gradA)
            .statusBarsPadding()
            .heightIn(min = 160.dp)
            .padding(start = 28.dp, end = 28.dp, top = 32.dp, bottom = 40.dp)
    ) {
        Text(
            text = "🎉",
            fontSize = 80.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .alpha(0.2f)
        )
        Text(
            text = "✨",
            fontSize = 40.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .alpha(0.25f)
        )
        Column {
            Text(
                text = AppConfig.APP_NAME,
                style = MaterialTheme.typography.displaySmall,
                color = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Organisez vos événements",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.75f)
            )
        }
    }
}

// ── Tab switcher ──────────────────────────────────────────────────────────────

@Composable
private fun AuthTabSwitcher(
    mode: AuthMode,
    onModeChange: (AuthMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AuthTab(
            text = "Connexion",
            selected = mode == AuthMode.Login,
            onClick = { onModeChange(AuthMode.Login) },
            modifier = Modifier.weight(1f)
        )
        AuthTab(
            text = "Inscription",
            selected = mode == AuthMode.Register,
            onClick = { onModeChange(AuthMode.Register) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AuthTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradA = MaterialTheme.appColors.gradA
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val textColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .height(44.dp)
            .clip(AppShapes.Pill)
            .then(
                if (selected) Modifier.background(brush = gradA)
                else Modifier.border(1.5.dp, borderColor, AppShapes.Pill)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

// ── Forms ─────────────────────────────────────────────────────────────────────

@Composable
private fun LoginForm(
    isLoading: Boolean,
    error: String?,
    onSubmit: (email: String, password: String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    FormCard {
        AuthTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email",
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
        )

        Spacer(Modifier.height(14.dp))

        AuthTextField(
            value = password,
            onValueChange = { password = it },
            label = "Mot de passe",
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            onImeAction = { focusManager.clearFocus() },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(
                        text = if (passwordVisible) "Cacher" else "Voir",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )

        ErrorText(error)

        Spacer(Modifier.height(20.dp))

        GradientButton(
            text = "Se connecter",
            isLoading = isLoading,
            enabled = email.isNotBlank() && password.isNotBlank(),
            onClick = { onSubmit(email, password) }
        )
    }
}

@Composable
private fun RegisterForm(
    isLoading: Boolean,
    error: String?,
    onSubmit: (email: String, password: String, displayName: String, phone: String?) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    FormCard {
        AuthTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = "Nom affiché",
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
        )

        Spacer(Modifier.height(14.dp))

        AuthTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email",
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
        )

        Spacer(Modifier.height(14.dp))

        AuthTextField(
            value = password,
            onValueChange = { password = it },
            label = "Mot de passe",
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(
                        text = if (passwordVisible) "Cacher" else "Voir",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )

        Spacer(Modifier.height(14.dp))

        AuthTextField(
            value = phone,
            onValueChange = { phone = it },
            label = "Téléphone (optionnel)",
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Done,
            onImeAction = { focusManager.clearFocus() }
        )

        ErrorText(error)

        Spacer(Modifier.height(20.dp))

        GradientButton(
            text = "Créer un compte",
            isLoading = isLoading,
            enabled = email.isNotBlank() && password.isNotBlank() && displayName.isNotBlank(),
            onClick = { onSubmit(email, password, displayName, phone.ifBlank { null }) }
        )
    }
}

// ── Composables utilitaires ───────────────────────────────────────────────────

@Composable
private fun FormCard(content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = AppShapes.Card,
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(24.dp), content = content)
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = AppShapes.TextField,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon
    )
}

@Composable
private fun GradientButton(
    text: String,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val gradA = MaterialTheme.appColors.gradA
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(AppShapes.Pill)
            .alpha(if (enabled && !isLoading) 1f else 0.5f)
            .background(brush = gradA)
            .clickable(enabled = enabled && !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
        }
    }
}

@Composable
private fun ErrorText(error: String?) {
    if (error != null) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private enum class AuthMode { Login, Register }